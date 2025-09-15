#!/bin/bash

set -e

echo "=== Gemma 3n 모델 다운로드 시작 ==="

# Ollama 서버가 시작될 때까지 대기
echo "Ollama 서버 시작 대기 중..."
while ! curl -s http://localhost:11434/api/tags > /dev/null 2>&1; do
    echo "Ollama 서버 대기 중... (5초 후 재시도)"
    sleep 5
done

echo "Ollama 서버가 준비되었습니다!"

# Gemma 3n E4B 모델 다운로드 (권장)
echo "Gemma 3n E4B 모델 다운로드 중... (시간이 오래 걸릴 수 있습니다)"
ollama pull gemma3n:e4b

# 모델이 정상적으로 다운로드되었는지 확인
echo "다운로드된 모델 확인:"
ollama list

# 간단한 테스트
echo "모델 테스트 중..."
ollama run gemma3n:e4b "안녕하세요. 간단한 인사를 해주세요." --verbose

echo "=== Gemma 3n 모델 다운로드 완료 ==="