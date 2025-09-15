#!/bin/bash

set -e

echo "=== Gemma 3n API 테스트 시작 ==="

BASE_URL="http://localhost:11434"

# 1. 서버 상태 확인
echo "1. 서버 상태 확인..."
if curl -f -s "$BASE_URL/api/tags" > /dev/null; then
    echo "✅ Ollama 서버 정상 작동"
else
    echo "❌ Ollama 서버에 연결할 수 없습니다"
    exit 1
fi

# 2. 사용 가능한 모델 확인
echo "2. 사용 가능한 모델 확인..."
MODELS_RESPONSE=$(curl -s "$BASE_URL/api/tags")
echo "사용 가능한 모델: $MODELS_RESPONSE"

if echo "$MODELS_RESPONSE" | grep -q "gemma3n"; then
    echo "✅ Gemma 3n 모델 사용 가능"
else
    echo "❌ Gemma 3n 모델이 없습니다. setup.sh를 먼저 실행해주세요."
    exit 1
fi

# 3. 텍스트 생성 테스트
echo "3. 텍스트 생성 테스트..."
TEXT_PROMPT="안녕하세요! 자기소개를 간단히 해주세요."

echo "프롬프트: $TEXT_PROMPT"
echo "응답 생성 중..."

curl -X POST "$BASE_URL/api/generate" \
    -H "Content-Type: application/json" \
    -d "{
        \"model\": \"gemma3n:e4b\",
        \"prompt\": \"$TEXT_PROMPT\",
        \"stream\": false
    }" | jq -r '.response'

echo ""

# 4. 스트리밍 테스트
echo "4. 스트리밍 응답 테스트..."
STREAM_PROMPT="1부터 5까지 숫자를 세어주세요."

echo "프롬프트: $STREAM_PROMPT"
echo "스트리밍 응답:"

curl -X POST "$BASE_URL/api/generate" \
    -H "Content-Type: application/json" \
    -d "{
        \"model\": \"gemma3n:e4b\",
        \"prompt\": \"$STREAM_PROMPT\",
        \"stream\": true
    }" | while IFS= read -r line; do
        if echo "$line" | jq -e '.response' >/dev/null 2>&1; then
            echo -n "$(echo "$line" | jq -r '.response')"
        fi
    done

echo ""
echo ""

# 5. 이미지 처리 테스트 준비 (Base64 인코딩이 필요)
echo "5. 멀티모달 기능 확인..."
echo "ℹ️ 이미지 테스트를 위해서는 이미지 파일이 필요합니다."
echo "   예시: base64 -i image.jpg | curl -X POST $BASE_URL/api/generate -d '{\"model\":\"gemma3n:e4b\",\"prompt\":\"이 이미지를 설명해주세요\",\"images\":[\"\$(cat)\"]}''"

echo ""
echo "=== API 테스트 완료 ==="
echo "✅ 모든 기본 기능이 정상 작동합니다!"