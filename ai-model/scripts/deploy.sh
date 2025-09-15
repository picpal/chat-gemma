#!/bin/bash

# 폐쇄망 환경 배포 스크립트
# 이 스크립트는 오프라인 패키지가 전송된 폐쇄망 서버에서 실행됩니다.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE_FILE="chatgemma-gemma3n-production-latest.tar"
COMPOSE_FILE="docker-compose.prod.yml"
CONTAINER_NAME="gemma3n-production"

echo "=================================================="
echo "🚀 Gemma 3n 폐쇄망 배포 시작"
echo "=================================================="

# 전제조건 확인
echo "1️⃣ 전제조건 확인 중..."

# Docker 설치 확인
if ! command -v docker &> /dev/null; then
    echo "❌ Docker가 설치되어 있지 않습니다"
    echo "   Docker를 먼저 설치해주세요: https://docs.docker.com/get-docker/"
    exit 1
fi

# Docker Compose 확인
if ! command -v docker compose &> /dev/null && ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose가 설치되어 있지 않습니다"
    echo "   Docker Compose를 먼저 설치해주세요"
    exit 1
fi

# Docker 서비스 실행 확인
if ! docker info &> /dev/null; then
    echo "❌ Docker 서비스가 실행 중이지 않습니다"
    echo "   Docker 서비스를 시작해주세요: sudo systemctl start docker"
    exit 1
fi

echo "✅ Docker 환경 확인 완료"

# 필요한 파일 존재 확인
echo "2️⃣ 배포 파일 확인 중..."

if [ ! -f "$IMAGE_FILE" ]; then
    echo "❌ Docker 이미지 파일이 없습니다: $IMAGE_FILE"
    exit 1
fi

if [ ! -f "$COMPOSE_FILE" ]; then
    echo "❌ Docker Compose 파일이 없습니다: $COMPOSE_FILE"
    exit 1
fi

IMAGE_SIZE=$(du -sh "$IMAGE_FILE" | cut -f1)
echo "✅ 배포 파일 확인 완료"
echo "   📦 이미지 파일: $IMAGE_FILE ($IMAGE_SIZE)"

# 기존 서비스 중지 (존재하는 경우)
echo "3️⃣ 기존 서비스 정리 중..."

if docker ps -q -f name="$CONTAINER_NAME" | grep -q .; then
    echo "   기존 컨테이너 중지 중..."
    docker compose -f "$COMPOSE_FILE" down || true
fi

# 기존 이미지 제거 (선택사항)
if docker images -q "chatgemma/gemma3n-production:latest" | grep -q .; then
    echo "   기존 이미지 제거 중..."
    docker rmi "chatgemma/gemma3n-production:latest" || true
fi

echo "✅ 기존 서비스 정리 완료"

# Docker 이미지 로드
echo "4️⃣ Docker 이미지 로드 중..."
echo "   이미지 로드 중... (시간이 오래 걸릴 수 있습니다)"

docker load -i "$IMAGE_FILE"

if [ $? -eq 0 ]; then
    echo "✅ 이미지 로드 완료"
    # 로드된 이미지 확인
    docker images "chatgemma/gemma3n-production:latest"
else
    echo "❌ 이미지 로드 실패"
    exit 1
fi

# 네트워크 생성
echo "5️⃣ Docker 네트워크 설정 중..."
docker network create gemma-network 2>/dev/null || echo "   네트워크가 이미 존재합니다"

# 서비스 시작
echo "6️⃣ Gemma 3n 서비스 시작 중..."
docker compose -f "$COMPOSE_FILE" up -d

if [ $? -eq 0 ]; then
    echo "✅ 서비스 시작 완료"
else
    echo "❌ 서비스 시작 실패"
    exit 1
fi

# 서비스 상태 확인
echo "7️⃣ 서비스 상태 확인 중..."
sleep 5

# 컨테이너 실행 상태 확인
if docker ps -f name="$CONTAINER_NAME" --format "table {{.Names}}\t{{.Status}}" | grep -q "$CONTAINER_NAME"; then
    echo "✅ 컨테이너 실행 중"
    docker ps -f name="$CONTAINER_NAME" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
else
    echo "❌ 컨테이너 실행 실패"
    echo "로그 확인:"
    docker compose -f "$COMPOSE_FILE" logs
    exit 1
fi

# API 응답 확인 (최대 60초 대기)
echo "8️⃣ API 서비스 준비 대기 중..."
MAX_WAIT=60
WAIT_COUNT=0

while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
    if curl -s -f http://localhost:11434/api/tags > /dev/null 2>&1; then
        echo "✅ API 서비스 준비 완료"
        break
    fi

    echo "   API 준비 대기 중... ($((WAIT_COUNT + 1))/$MAX_WAIT)"
    sleep 1
    WAIT_COUNT=$((WAIT_COUNT + 1))
done

if [ $WAIT_COUNT -eq $MAX_WAIT ]; then
    echo "⚠️ API 서비스 응답 대기 시간 초과"
    echo "   서비스가 시작 중일 수 있습니다. 몇 분 후 다시 확인해주세요."
else
    # 모델 확인
    echo "9️⃣ 모델 상태 확인 중..."
    MODELS=$(curl -s http://localhost:11434/api/tags | jq -r '.models[].name' 2>/dev/null || echo "")

    if echo "$MODELS" | grep -q "gemma3n:e4b"; then
        echo "✅ Gemma 3n E4B 모델 로드 완료"
    else
        echo "⚠️ 모델 로드 중이거나 문제가 있을 수 있습니다"
        echo "   사용 가능한 모델: $MODELS"
    fi
fi

# 배포 완료
echo "=================================================="
echo "🎉 Gemma 3n 폐쇄망 배포 완료!"
echo "=================================================="
echo "📍 서비스 정보:"
echo "   - API 엔드포인트: http://localhost:11434"
echo "   - 컨테이너명: $CONTAINER_NAME"
echo "   - 모델: gemma3n:e4b"
echo ""
echo "🧪 API 테스트:"
echo '   curl -X POST http://localhost:11434/api/generate \'
echo '     -H "Content-Type: application/json" \'
echo '     -d '"'"'{"model":"gemma3n:e4b","prompt":"안녕하세요","stream":false}'"'"
echo ""
echo "🔧 서비스 관리:"
echo "   - 시작: docker compose -f $COMPOSE_FILE up -d"
echo "   - 중지: docker compose -f $COMPOSE_FILE down"
echo "   - 로그: docker compose -f $COMPOSE_FILE logs -f"
echo "   - 상태: docker compose -f $COMPOSE_FILE ps"
echo ""
echo "💡 헬스체크:"
echo "   ./health.sh"
echo ""
echo "=================================================="

# 최종 상태 표시
echo "현재 서비스 상태:"
docker compose -f "$COMPOSE_FILE" ps