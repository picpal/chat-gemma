#!/bin/bash

# 오프라인 배포 준비 스크립트
# 이 스크립트는 개발환경에서 실행하여 폐쇄망 배포 패키지를 생성합니다.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
PACKAGE_NAME="gemma3n-offline-package"
MODELS_DIR="$ROOT_DIR/models"
BUILD_DIR="$ROOT_DIR/build"

echo "=================================================="
echo "🚀 Gemma 3n 오프라인 배포 패키지 생성 시작"
echo "=================================================="

# 빌드 디렉토리 정리
echo "📁 빌드 디렉토리 정리 중..."
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
mkdir -p "$MODELS_DIR"

# 1. 현재 실행 중인 Ollama에서 모델 확인
echo "1️⃣ 현재 모델 상태 확인 중..."
if ! curl -s http://localhost:11434/api/tags > /dev/null; then
    echo "❌ Ollama 서버가 실행 중이지 않습니다"
    echo "   먼저 'docker compose up -d ollama'를 실행하세요"
    exit 1
fi

# 사용 가능한 모델 확인
AVAILABLE_MODELS=$(curl -s http://localhost:11434/api/tags | jq -r '.models[].name' || echo "")

if ! echo "$AVAILABLE_MODELS" | grep -q "gemma3n:e4b"; then
    echo "❌ Gemma 3n E4B 모델이 없습니다"
    echo "   먼저 './scripts/setup.sh'를 실행하여 모델을 다운로드하세요"
    exit 1
fi

echo "✅ Gemma 3n E4B 모델 확인됨"

# 2. 모델 파일 추출
echo "2️⃣ 모델 파일 추출 중..."
CONTAINER_NAME="gemma3n-ollama"

# 컨테이너에서 모델 파일 복사
echo "   컨테이너에서 모델 파일 복사 중..."
docker exec "$CONTAINER_NAME" tar -czf /tmp/models.tar.gz -C /root/.ollama models/

docker cp "$CONTAINER_NAME:/tmp/models.tar.gz" "$BUILD_DIR/models.tar.gz"

# 모델 파일 압축 해제
echo "   모델 파일 압축 해제 중..."
cd "$BUILD_DIR"
tar -xzf models.tar.gz
cd "$ROOT_DIR"

# 모델 디렉토리로 이동
mv "$BUILD_DIR/models" "$MODELS_DIR"

# 모델 파일 검증
echo "   모델 파일 검증 중..."
MODEL_SIZE=$(du -sh "$MODELS_DIR" | cut -f1)
echo "   📊 추출된 모델 크기: $MODEL_SIZE"

# 3. 운영용 Docker 이미지 빌드
echo "3️⃣ 운영용 Docker 이미지 빌드 중..."
IMAGE_NAME="chatgemma/gemma3n-production"
IMAGE_TAG="latest"

echo "   이미지 빌드: $IMAGE_NAME:$IMAGE_TAG"
docker build -f Dockerfile.production -t "$IMAGE_NAME:$IMAGE_TAG" .

# 빌드 성공 확인
if [ $? -eq 0 ]; then
    echo "   ✅ Docker 이미지 빌드 완료"
    IMAGE_SIZE=$(docker images "$IMAGE_NAME:$IMAGE_TAG" --format "table {{.Size}}" | tail -n 1)
    echo "   📊 이미지 크기: $IMAGE_SIZE"
else
    echo "   ❌ Docker 이미지 빌드 실패"
    exit 1
fi

# 4. Docker 이미지 저장
echo "4️⃣ Docker 이미지 저장 중..."
docker save -o "$BUILD_DIR/$IMAGE_NAME-$IMAGE_TAG.tar" "$IMAGE_NAME:$IMAGE_TAG"

echo "   ✅ 이미지 저장 완료: $BUILD_DIR/$IMAGE_NAME-$IMAGE_TAG.tar"

# 5. 배포 파일들 준비
echo "5️⃣ 배포 파일 준비 중..."

# 운영용 docker-compose.yml 생성
cat > "$BUILD_DIR/docker-compose.prod.yml" << 'EOF'
version: '3.8'

services:
  gemma3n:
    image: chatgemma/gemma3n-production:latest
    container_name: gemma3n-production
    ports:
      - "11434:11434"
    environment:
      - OLLAMA_HOST=0.0.0.0
      - OLLAMA_ORIGINS=*  # 운영 시 제한 필요
      - OLLAMA_NUM_PARALLEL=2
      - OLLAMA_MAX_LOADED_MODELS=1
      - OLLAMA_KEEP_ALIVE=10m
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 16G
        reservations:
          memory: 8G
    healthcheck:
      test: ["CMD", "/usr/local/bin/health.sh"]
      interval: 30s
      timeout: 15s
      retries: 3
      start_period: 60s
    networks:
      - gemma-network

volumes:
  gemma-logs:
    driver: local

networks:
  gemma-network:
    driver: bridge
EOF

# 배포 스크립트 복사
cp "$SCRIPT_DIR/deploy.sh" "$BUILD_DIR/"
cp "$SCRIPT_DIR/health.sh" "$BUILD_DIR/"

# 운영 가이드 생성
cat > "$BUILD_DIR/DEPLOYMENT.md" << 'EOF'
# 폐쇄망 배포 가이드

## 1. 파일 전송
이 폴더의 모든 파일을 폐쇄망 서버로 전송하세요.

## 2. 배포 실행
```bash
# 실행 권한 설정
chmod +x deploy.sh health.sh

# 배포 실행
./deploy.sh
```

## 3. 동작 확인
```bash
# API 테스트
curl http://localhost:11434/api/tags

# 텍스트 생성 테스트
curl -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{"model":"gemma3n:e4b","prompt":"Hello","stream":false}'
```

## 4. 서비스 관리
```bash
# 시작
docker compose -f docker-compose.prod.yml up -d

# 중지
docker compose -f docker-compose.prod.yml down

# 로그 확인
docker compose -f docker-compose.prod.yml logs -f
```
EOF

# 6. 최종 패키지 생성
echo "6️⃣ 최종 패키지 생성 중..."
cd "$BUILD_DIR"

# 압축 파일 생성
tar -czf "../$PACKAGE_NAME.tar.gz" \
    chatgemma-gemma3n-production-latest.tar \
    docker-compose.prod.yml \
    deploy.sh \
    health.sh \
    DEPLOYMENT.md

cd "$ROOT_DIR"

# 빌드 디렉토리 정리 (선택사항)
rm -rf "$BUILD_DIR"
rm -rf "$MODELS_DIR"

# 완료 메시지
PACKAGE_SIZE=$(du -sh "$PACKAGE_NAME.tar.gz" | cut -f1)

echo "=================================================="
echo "✅ 오프라인 배포 패키지 생성 완료!"
echo "=================================================="
echo "📦 패키지 파일: $PACKAGE_NAME.tar.gz"
echo "📊 패키지 크기: $PACKAGE_SIZE"
echo ""
echo "🚀 배포 방법:"
echo "1. $PACKAGE_NAME.tar.gz를 폐쇄망 서버로 전송"
echo "2. 압축 해제: tar -xzf $PACKAGE_NAME.tar.gz"
echo "3. 배포 실행: ./deploy.sh"
echo ""
echo "📝 자세한 내용은 압축 해제 후 DEPLOYMENT.md를 참조하세요"
echo "=================================================="