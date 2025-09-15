#!/bin/bash

# Ollama 헬스체크 스크립트

# API 응답 확인
if curl -f -s http://localhost:11434/api/tags > /dev/null; then
    echo "✅ Ollama API 정상"
else
    echo "❌ Ollama API 응답 없음"
    exit 1
fi

# 모델 로드 상태 확인
MODELS=$(curl -s http://localhost:11434/api/tags | jq -r '.models[].name' 2>/dev/null)

if echo "$MODELS" | grep -q "gemma3n"; then
    echo "✅ Gemma 3n 모델 사용 가능"
else
    echo "⚠️ Gemma 3n 모델 없음 - 다운로드가 필요할 수 있습니다"
fi

echo "현재 사용 가능한 모델:"
echo "$MODELS"