# AI Model - Gemma 3n with Ollama

ChatGemma 프로젝트의 AI 모델 서비스입니다. Google의 최신 Gemma 3n 모델을 Ollama를 통해 로컬에서 실행합니다.

## 🚀 빠른 시작

### 개발 환경 실행

```bash
# 1. Docker 네트워크 생성 및 컨테이너 실행
docker compose up -d ollama

# 2. Gemma 3n 모델 다운로드 (자동)
./scripts/setup.sh

# 3. API 테스트
./scripts/test-api.sh
```

### 접속 정보

- **Ollama API**: http://localhost:11434
- **모델**: gemma3n:e4b (6.9B parameters)
- **헬스체크**: `curl http://localhost:11434/api/tags`

## 📊 모델 정보

### Gemma 3n E4B 상세

| 항목 | 값 |
|------|-----|
| **모델명** | gemma3n:e4b |
| **실제 크기** | 8B parameters |
| **효과적 크기** | 4B parameters (MatFormer) |
| **파일 크기** | 7.5GB (Q4_K_M 양자화) |
| **메모리 요구** | 8GB RAM 권장 |
| **컨텍스트 길이** | 128K tokens |
| **언어 지원** | 140개 언어 (한국어 포함) |
| **멀티모달** | 텍스트 + 이미지 입력 |

### 성능 특징

- **MatFormer 아키텍처**: 동적 파라미터 활성화로 메모리 효율성 증대
- **PLE (Per-Layer Embeddings)**: 실제 8B이지만 4B 수준 메모리 사용
- **디바이스 최적화**: 모바일/엣지 디바이스용으로 설계
- **오프라인 완전 지원**: 인터넷 연결 불필요

## 🐳 Docker 구성

### 개발용 (현재 설정)

```yaml
# docker-compose.yml
services:
  ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
    volumes:
      - ollama-models:/root/.ollama/models
    environment:
      - OLLAMA_HOST=0.0.0.0
      - OLLAMA_ORIGINS=*
```

**특징:**
- 모델을 런타임에 다운로드
- 개발 및 테스트용
- 볼륨으로 모델 데이터 유지

### 운영용 (폐쇄망 배포)

```dockerfile
# Dockerfile.production
FROM ollama/ollama:latest

# 모델 파일을 이미지에 직접 포함
COPY models/ /root/.ollama/models/

# 사전 로딩된 모델로 즉시 실행 가능
ENV OLLAMA_MODELS=/root/.ollama/models

CMD ["serve"]
```

**특징:**
- 모델이 이미지에 포함되어 즉시 실행 가능
- 네트워크 연결 불필요
- 폐쇄망 환경에서 바로 사용

## 🔒 폐쇄망 배포 전략

### 1단계: 개발 환경에서 준비

```bash
# 모든 필요한 모델 다운로드
./scripts/prepare-offline.sh
```

이 스크립트가 수행하는 작업:
1. Gemma 3n 모델 다운로드 및 검증
2. 모델 파일을 로컬 디렉토리로 추출
3. 운영용 Docker 이미지 빌드 (모델 포함)
4. 이미지를 tar 파일로 저장

### 2단계: 폐쇄망으로 전송

```bash
# 생성된 패키지를 폐쇄망으로 전송
# gemma3n-offline-package.tar.gz (약 8GB)
```

패키지 구성:
- `gemma3n-production.tar` - 모델 포함 Docker 이미지
- `docker-compose.prod.yml` - 운영 환경 설정
- `deploy.sh` - 배포 스크립트
- `health-check.sh` - 동작 확인 스크립트

### 3단계: 폐쇄망에서 배포

```bash
# 패키지 압축 해제
tar -xzf gemma3n-offline-package.tar.gz

# Docker 이미지 로드
docker load -i gemma3n-production.tar

# 서비스 시작
docker compose -f docker-compose.prod.yml up -d

# 동작 확인
./health-check.sh
```

## 🔌 API 사용법

### 기본 텍스트 생성

```bash
curl -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gemma3n:e4b",
    "prompt": "안녕하세요! 자기소개를 해주세요.",
    "stream": false
  }'
```

### 스트리밍 응답

```bash
curl -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gemma3n:e4b",
    "prompt": "긴 이야기를 들려주세요.",
    "stream": true
  }'
```

### 이미지와 텍스트 (멀티모달)

```bash
# 이미지를 Base64로 인코딩
IMAGE_BASE64=$(base64 -i image.jpg)

curl -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gemma3n:e4b",
    "prompt": "이 이미지에 대해 설명해주세요.",
    "images": ["'$IMAGE_BASE64'"]
  }'
```

### 응답 포맷

```json
{
  "model": "gemma3n:e4b",
  "created_at": "2025-09-14T14:16:22.979Z",
  "response": "안녕하세요! 저는 Gemma...",
  "done": true,
  "total_duration": 19370014884,
  "eval_count": 89
}
```

## 🛠️ 개발 및 운영

### 로컬 개발

```bash
# 환경 설정
./scripts/setup.sh

# 모델 상태 확인
curl http://localhost:11434/api/tags

# 성능 테스트
./scripts/benchmark.sh

# 로그 확인
docker compose logs -f ollama
```

### 운영 환경 모니터링

```bash
# 컨테이너 상태
docker compose ps

# 리소스 사용량
docker stats gemma3n-ollama

# API 응답 시간 테스트
time curl -X POST http://localhost:11434/api/generate -d '...'
```

### 문제 해결

#### 모델이 응답하지 않을 때
```bash
# 1. 컨테이너 재시작
docker compose restart ollama

# 2. 모델 리로딩
docker exec gemma3n-ollama ollama stop gemma3n:e4b
docker exec gemma3n-ollama ollama run gemma3n:e4b "test"

# 3. 로그 확인
docker compose logs ollama
```

#### 메모리 부족 오류
```bash
# Docker 메모리 제한 조정
# docker-compose.yml에서 memory 설정 변경
deploy:
  resources:
    limits:
      memory: 16G  # 8G에서 16G로 증가
```

#### 느린 응답 속도
```bash
# GPU 사용 확인 (NVIDIA GPU 있는 경우)
docker run --rm --gpus all nvidia/cuda nvidia-smi

# CPU 전용 최적화
export OLLAMA_NUM_PARALLEL=4  # CPU 코어 수에 맞게 조정
```

## 📁 디렉토리 구조

```
ai-model/
├── README.md              # 📖 이 문서
├── CLAUDE.md              # 🤖 상세 기술 문서
├── docker-compose.yml     # 🐳 개발환경 Docker 구성
├── docker-compose.prod.yml # 🚀 운영환경 Docker 구성
├── Dockerfile.production  # 🏗️ 운영용 이미지 빌드
├── ollama/
│   ├── Dockerfile
│   └── scripts/
│       ├── download.sh    # 모델 다운로드
│       └── health.sh      # 헬스체크
├── scripts/
│   ├── setup.sh           # 개발환경 설정
│   ├── test-api.sh        # API 테스트
│   ├── prepare-offline.sh # 오프라인 패키징
│   ├── deploy.sh          # 폐쇄망 배포
│   └── benchmark.sh       # 성능 테스트
├── config/
│   └── models.json        # 모델 설정
└── monitoring/            # 모니터링 (선택사항)
    ├── prometheus.yml
    └── grafana/
```

## 🔧 설정 및 튜닝

### 환경 변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `OLLAMA_HOST` | `127.0.0.1` | 바인딩 호스트 |
| `OLLAMA_ORIGINS` | `localhost` | CORS 허용 도메인 |
| `OLLAMA_NUM_PARALLEL` | `2` | 병렬 처리 수 |
| `OLLAMA_MAX_LOADED_MODELS` | `1` | 최대 로딩 모델 수 |
| `OLLAMA_KEEP_ALIVE` | `5m` | 모델 메모리 유지 시간 |

### 성능 튜닝

```yaml
# docker-compose.yml
services:
  ollama:
    environment:
      # CPU 최적화
      - OLLAMA_NUM_PARALLEL=4
      - OMP_NUM_THREADS=8

      # 메모리 관리
      - OLLAMA_MAX_LOADED_MODELS=1
      - OLLAMA_KEEP_ALIVE=10m

      # GPU 사용 (가능한 경우)
      - CUDA_VISIBLE_DEVICES=0
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
```

## 🚨 중요 사항

### 보안
- 운영 환경에서는 `OLLAMA_ORIGINS` 제한 필수
- API 인증 레이어 추가 권장 (nginx, API Gateway 등)
- 네트워크 격리 설정

### 라이선스
- Gemma 모델: [Gemma Terms of Use](https://ai.google.dev/gemma/terms) 준수
- Ollama: Apache 2.0 License
- 상업적 사용 시 라이선스 확인 필수

### 제한사항
- 현재 오디오/비디오 입력 미지원 (추후 업데이트 예정)
- 단일 모델 인스턴스로 동시 요청 처리 제한
- 대용량 이미지 처리 시 메모리 사용량 증가

---

**다음 단계**: [백엔드 연동 가이드](../backend/CLAUDE.md)를 참조하여 Spring Boot와 연동하세요.