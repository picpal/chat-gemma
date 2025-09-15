#!/bin/bash

# 모델 무결성 검증 스크립트

set -e

echo "=== 모델 무결성 검증 시작 ==="

MODELS_DIR="/root/.ollama/models"

# 모델 디렉토리 존재 확인
if [ ! -d "$MODELS_DIR" ]; then
    echo "❌ 모델 디렉토리가 없습니다: $MODELS_DIR"
    exit 1
fi

echo "✅ 모델 디렉토리 확인: $MODELS_DIR"

# 모델 파일 존재 확인
MODEL_FILES=$(find "$MODELS_DIR" -name "*.gguf" -o -name "*.bin" -o -name "*.safetensors" | wc -l)

if [ "$MODEL_FILES" -eq 0 ]; then
    echo "❌ 모델 파일이 없습니다"
    exit 1
fi

echo "✅ 모델 파일 $MODEL_FILES 개 발견"

# 디스크 사용량 확인
DISK_USAGE=$(du -sh "$MODELS_DIR" | cut -f1)
echo "📊 모델 디스크 사용량: $DISK_USAGE"

# 최소 용량 확인 (Gemma 3n E4B는 최소 6GB)
DISK_USAGE_BYTES=$(du -sb "$MODELS_DIR" | cut -f1)
MIN_SIZE=$((6 * 1024 * 1024 * 1024))  # 6GB in bytes

if [ "$DISK_USAGE_BYTES" -lt "$MIN_SIZE" ]; then
    echo "⚠️ 모델 파일 크기가 예상보다 작습니다 (< 6GB)"
    echo "   실제 크기: $(($DISK_USAGE_BYTES / 1024 / 1024 / 1024))GB"
fi

# 파일 권한 확인
echo "🔒 파일 권한 설정 중..."
chown -R root:root "$MODELS_DIR"
find "$MODELS_DIR" -type f -exec chmod 644 {} \;
find "$MODELS_DIR" -type d -exec chmod 755 {} \;

echo "=== 모델 무결성 검증 완료 ==="
echo "준비된 모델:"
find "$MODELS_DIR" -name "*.gguf" -exec basename {} \;