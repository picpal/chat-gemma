#!/bin/bash

set -e

echo "=== Gemma 3n AI 모델 환경 설정 시작 ==="

# 실행 권한 설정
chmod +x ollama/scripts/*.sh
chmod +x scripts/*.sh

# Docker 네트워크 생성 (이미 있으면 무시)
docker network create gemma-network 2>/dev/null || echo "네트워크 gemma-network가 이미 존재합니다."

# Ollama 컨테이너 시작
echo "Ollama 컨테이너 시작 중..."
docker-compose up -d ollama

# 컨테이너가 완전히 시작될 때까지 대기
echo "Ollama 서비스 준비 대기 중..."
sleep 10

# 헬스체크
echo "Ollama 헬스체크..."
./ollama/scripts/health.sh

# 모델 다운로드 확인
if ! docker exec gemma3n-ollama ollama list | grep -q "gemma3n"; then
    echo "Gemma 3n 모델이 없습니다. 다운로드를 시작합니다..."
    docker exec gemma3n-ollama /bin/bash -c "
        echo 'Gemma 3n E4B 모델 다운로드 중...'
        ollama pull gemma3n:e4b
        echo '모델 다운로드 완료!'
        ollama list
    "
else
    echo "Gemma 3n 모델이 이미 존재합니다."
fi

echo "=== AI 모델 환경 설정 완료 ==="
echo ""
echo "🎉 사용 가능한 엔드포인트:"
echo "  - Ollama API: http://localhost:11434"
echo "  - 모델 목록: curl http://localhost:11434/api/tags"
echo "  - 텍스트 생성: curl -X POST http://localhost:11434/api/generate -d '{\"model\":\"gemma3n:e4b\",\"prompt\":\"안녕하세요\"}'"
echo ""
echo "📝 상태 확인:"
docker-compose ps