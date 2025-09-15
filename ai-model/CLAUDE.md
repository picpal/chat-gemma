# AI Model - Gemma 3n 통합

## 📋 요구사항

### 핵심 기능
- Gemma 3n 모델 로컬 실행
- 오프라인 환경 완벽 지원
- 멀티모달 처리 (텍스트, 이미지)
- 스트리밍 응답 지원
- 모델 성능 최적화
- 폐쇄망 배포 지원

### 기술 스택
- **모델**: Google Gemma 3n (E4B 권장)
- **런타임**: Ollama
- **컨테이너**: Docker
- **API**: REST API (Ollama Native)
- **모니터링**: Prometheus + Grafana (선택)

## 🏗️ 프로젝트 구조

```
ai-model/
├── ollama/
│   ├── Dockerfile           # Ollama 컨테이너 정의
│   ├── models/             # 모델 파일 저장
│   └── scripts/
│       ├── download.sh     # 모델 다운로드 스크립트
│       ├── backup.sh       # 모델 백업 스크립트
│       └── health.sh       # 헬스체크 스크립트
├── config/
│   ├── ollama.yml         # Ollama 설정
│   └── models.json        # 모델 메타데이터
├── docker-compose.yml      # 컨테이너 오케스트레이션
├── scripts/
│   ├── prepare-offline.sh # 오프라인 준비
│   └── deploy.sh          # 배포 스크립트
└── monitoring/            # 모니터링 설정 (선택)
    ├── prometheus.yml
    └── grafana/
```

## 🤖 Gemma 3n 모델 상세

### 모델 특징
- **MatFormer 아키텍처**: 중첩된 모델 구조로 동적 파라미터 활성화
- **PLE (Per-Layer Embeddings)**: 메모리 효율적인 실행
- **멀티모달**: 텍스트, 이미지, 오디오(추후), 비디오(추후) 지원
- **128K 컨텍스트**: 긴 대화 처리 가능
- **140개 언어**: 다국어 지원

### 모델 버전 선택

| 모델 | 실제 크기 | 효과적 크기 | 메모리 요구 | 용도 |
|------|----------|------------|------------|------|
| E2B  | 6B       | 2B         | 4GB        | 경량 환경 |
| E4B  | 8B       | 4B         | 8GB        | **권장** |
| 12B  | 12B      | 12B        | 16GB       | 고성능 |
| 27B  | 27B      | 27B        | 32GB+      | 최고 성능 |

### 권장 사양
- **최소**: 8GB RAM, 20GB Storage
- **권장**: 16GB RAM, 50GB Storage, GPU (선택)
- **운영**: 32GB RAM, 100GB Storage, NVIDIA GPU

## 🐳 Docker 구성

### Ollama Dockerfile

```dockerfile
# ai-model/ollama/Dockerfile
FROM ollama/ollama:latest

# 환경 변수 설정
ENV OLLAMA_MODELS=/root/.ollama/models
ENV OLLAMA_NUM_PARALLEL=2
ENV OLLAMA_MAX_LOADED_MODELS=1
ENV OLLAMA_KEEP_ALIVE=5m

# 모델 사전 다운로드 스크립트
COPY scripts/download.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/download.sh

# 헬스체크 스크립트
COPY scripts/health.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/health.sh

# 모델 다운로드 (빌드 시)
RUN /usr/local/bin/download.sh

# 헬스체크 설정
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD /usr/local/bin/health.sh || exit 1

EXPOSE 11434

CMD ["serve"]
```

### Docker Compose

```yaml
# ai-model/docker-compose.yml
version: '3.8'

services:
  ollama:
    build:
      context: ./ollama
      dockerfile: Dockerfile
    container_name: gemma3n-ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama-models:/root/.ollama/models
      - ./config/ollama.yml:/etc/ollama/config.yml:ro
    environment:
      - OLLAMA_HOST=0.0.0.0
      - OLLAMA_ORIGINS=*
      - OLLAMA_MODELS=/root/.ollama/models
    deploy:
      resources:
        limits:
          memory: 16G
        reservations:
          memory: 8G
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
    restart: unless-stopped
    networks:
      - gemma-network

volumes:
  ollama-models:
    driver: local

networks:
  gemma-network:
    external: true
```

## 📜 스크립트

### 모델 다운로드 스크립트

```bash
#!/bin/bash
# ai-model/ollama/scripts/download.sh

echo "Downloading Gemma 3n models..."

# Gemma 3n E4B (권장)
ollama pull gemma3n:e4b

# Gemma 3n E2B (경량)
ollama pull gemma3n:e2b

# 모델 확인
ollama list

echo "Model download complete!"
```

### 오프라인 준비 스크립트

```bash
#!/bin/bash
# ai-model/scripts/prepare-offline.sh

set -e

echo "=== Gemma 3n 오프라인 배포 준비 ==="

# 1. 모델 다운로드
echo "1. 모델 다운로드 중..."
docker-compose up -d ollama
sleep 10
docker exec gemma3n-ollama ollama pull gemma3n:e4b
docker exec gemma3n-ollama ollama pull gemma3n:e2b

# 2. 모델 파일 백업
echo "2. 모델 파일 백업 중..."
docker exec gemma3n-ollama tar -czf /tmp/models.tar.gz /root/.ollama/models
docker cp gemma3n-ollama:/tmp/models.tar.gz ./models-backup.tar.gz

# 3. Docker 이미지 저장
echo "3. Docker 이미지 저장 중..."
docker save -o gemma3n-ollama.tar ollama/ollama:latest

# 4. 배포 패키지 생성
echo "4. 배포 패키지 생성 중..."
tar -czf gemma3n-offline-package.tar.gz \
    gemma3n-ollama.tar \
    models-backup.tar.gz \
    docker-compose.yml \
    scripts/deploy.sh

echo "=== 준비 완료! ==="
echo "배포 파일: gemma3n-offline-package.tar.gz"
```

### 배포 스크립트

```bash
#!/bin/bash
# ai-model/scripts/deploy.sh

set -e

echo "=== Gemma 3n 오프라인 배포 ==="

# 1. Docker 이미지 로드
echo "1. Docker 이미지 로드 중..."
docker load -i gemma3n-ollama.tar

# 2. 모델 파일 복원
echo "2. 모델 파일 복원 중..."
docker-compose up -d ollama
sleep 5
docker exec gemma3n-ollama mkdir -p /root/.ollama
docker cp models-backup.tar.gz gemma3n-ollama:/tmp/
docker exec gemma3n-ollama tar -xzf /tmp/models-backup.tar.gz -C /

# 3. 서비스 재시작
echo "3. 서비스 시작 중..."
docker-compose restart ollama

# 4. 헬스체크
echo "4. 헬스체크 중..."
sleep 10
curl -f http://localhost:11434/api/tags || exit 1

echo "=== 배포 완료! ==="
```

## 🔌 API 사용법

### 텍스트 생성

```bash
curl http://localhost:11434/api/generate -d '{
  "model": "gemma3n:e4b",
  "prompt": "안녕하세요. 자기소개를 해주세요.",
  "stream": false
}'
```

### 이미지와 함께 질문

```bash
# 이미지를 Base64로 인코딩
IMAGE_BASE64=$(base64 -i image.jpg)

curl http://localhost:11434/api/generate -d '{
  "model": "gemma3n:e4b",
  "prompt": "이 이미지에 무엇이 있나요?",
  "images": ["'$IMAGE_BASE64'"],
  "stream": false
}'
```

### 스트리밍 응답

```bash
curl -N http://localhost:11434/api/generate -d '{
  "model": "gemma3n:e4b",
  "prompt": "긴 이야기를 들려주세요.",
  "stream": true
}'
```

## 📊 모니터링

### 메트릭 수집 항목
- 응답 시간 (P50, P95, P99)
- 처리량 (requests/sec)
- 메모리 사용량
- GPU 사용률 (if available)
- 모델 로딩 시간
- 에러율

### Prometheus 설정

```yaml
# ai-model/monitoring/prometheus.yml
scrape_configs:
  - job_name: 'ollama'
    static_configs:
      - targets: ['ollama:11434']
    metrics_path: '/metrics'
```

## 🔧 최적화 팁

### 메모리 최적화
1. **모델 언로드**: `OLLAMA_KEEP_ALIVE=5m` 설정으로 미사용 시 자동 언로드
2. **배치 처리**: 여러 요청을 배치로 처리
3. **컨텍스트 관리**: 불필요한 히스토리 제거

### 성능 최적화
1. **GPU 활용**: NVIDIA GPU 사용 시 3-5배 성능 향상
2. **모델 선택**: 요구사항에 맞는 최소 모델 사용
3. **캐싱**: 자주 사용되는 프롬프트 캐싱

### 안정성 확보
1. **헬스체크**: 30초마다 모델 상태 확인
2. **자동 재시작**: 오류 시 자동 재시작
3. **리소스 제한**: 메모리/CPU 제한 설정

## ⚠️ 주의사항

### 보안
- 프로덕션 환경에서 `OLLAMA_ORIGINS` 제한 필수
- API 키 또는 인증 레이어 추가 권장
- 네트워크 격리 환경 구성

### 라이선스
- Gemma 모델은 Gemma Terms of Use 준수 필요
- 상업적 사용 시 라이선스 확인 필수

### 제한사항
- 현재 오디오/비디오 입력은 미지원 (추후 업데이트 예정)
- 동시 처리 요청 수 제한 (OLLAMA_NUM_PARALLEL)
- 단일 모델 로딩 제한 (OLLAMA_MAX_LOADED_MODELS)