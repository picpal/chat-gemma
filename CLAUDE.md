# ChatGemma - Gemma 3n 기반 오프라인 채팅 서비스

## 🎯 프로젝트 개요

Claude UI를 벤치마킹한 오프라인 환경 채팅 서비스입니다. Google의 최신 Gemma 3n 모델을 활용하여 텍스트와 이미지를 처리할 수 있으며, 폐쇄망 환경에서도 완벽하게 동작합니다.

### 핵심 특징
- 🤖 **Gemma 3n 모델**: 디바이스 최적화된 최신 AI 모델
- 🔒 **완전 오프라인**: 인터넷 연결 없이 동작
- 🖼️ **멀티모달**: 텍스트 + 이미지 입력 지원
- 👥 **관리자 승인**: 회원가입 시 관리자 승인 필요
- 📊 **보안 감사**: 모든 활동 로깅
- 💬 **실시간 채팅**: WebSocket 기반 스트리밍

## 🏗️ 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend (React)                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ Chat UI     │  │ Admin Panel │  │ Auth Components         │  │
│  │ (Claude 스타일)│  │             │  │ (Login/Signup)          │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↕ HTTP + WebSocket
┌─────────────────────────────────────────────────────────────────┐
│                     Backend (Spring Boot)                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ REST API    │  │ WebSocket   │  │ Security & Audit        │  │
│  │ Controllers │  │ Handlers    │  │ (Session Auth + Logs)   │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↕ JPA
┌─────────────────────────────────────────────────────────────────┐
│            Database (H2 Dev / Oracle 19c Prod)                 │
│     Users | Chats | Messages | AuditLogs | Approvals          │
└─────────────────────────────────────────────────────────────────┘
                              ↕ HTTP API
┌─────────────────────────────────────────────────────────────────┐
│                      AI Model (Ollama)                         │
│          Gemma 3n E4B (8B params, 4B effective)               │
│              Text + Image → AI Response                        │
└─────────────────────────────────────────────────────────────────┘
```

## 📁 프로젝트 구조

```
chatGemma/
├── CLAUDE.md                 # 📖 이 파일 (전체 가이드)
├── docker-compose.yml        # 🐳 전체 서비스 오케스트레이션
├── .env                      # 🔧 환경 변수
├── scripts/                  # 📜 배포/관리 스크립트
│   ├── setup.sh             # 초기 설정
│   ├── build-all.sh         # 전체 빌드
│   └── deploy-offline.sh    # 오프라인 배포
├── backend/                  # ☕ Spring Boot 애플리케이션
│   ├── CLAUDE.md            # Backend 상세 가이드
│   ├── src/main/java/
│   ├── src/test/java/
│   └── pom.xml
├── frontend/                 # ⚛️ React 애플리케이션
│   ├── CLAUDE.md            # Frontend 상세 가이드
│   ├── src/
│   ├── package.json
│   └── vite.config.ts
└── ai-model/                 # 🤖 Gemma 3n 통합
    ├── CLAUDE.md            # AI Model 상세 가이드
    ├── ollama/
    ├── docker-compose.yml
    └── scripts/
```

## 🚀 빠른 시작

### 1. 환경 준비

```bash
# 필수 소프트웨어 확인
docker --version        # Docker 20.0+
docker-compose --version # Docker Compose 2.0+
java --version          # Java 17+
node --version          # Node.js 18+
```

### 2. 초기 설정

```bash
# 프로젝트 클론 (현재 디렉토리에서)
git clone . # 이미 현재 디렉토리에 있음

# 환경 변수 설정
cp .env.example .env
# .env 파일 편집 (DB 패스워드 등)

# 초기 설정 실행
chmod +x scripts/setup.sh
./scripts/setup.sh
```

### 3. 개발 환경 실행

```bash
# AI 모델 먼저 시작 (시간이 오래 걸림)
cd ai-model
docker-compose up -d

# 백엔드 실행 (별도 터미널)
cd backend
./mvnw spring-boot:run -Dspring.profiles.active=dev

# 프론트엔드 실행 (별도 터미널)
cd frontend
npm install
npm run dev
```

### 4. 접속 확인

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **Ollama API**: http://localhost:11434
- **H2 Console**: http://localhost:8080/h2-console

## 🛠️ 개발 가이드

### TDD 개발 흐름

1. **테스트 작성** (Red)
2. **최소 구현** (Green)
3. **리팩토링** (Refactor)
4. **테스트 실행 및 커버리지 확인**

```bash
# 백엔드 테스트
cd backend
./mvnw test
./mvnw jacoco:report

# 프론트엔드 테스트
cd frontend
npm run test
npm run test:coverage
```

### 컴포넌트별 개발 가이드

- **Backend**: [backend/CLAUDE.md](./backend/CLAUDE.md)
- **Frontend**: [frontend/CLAUDE.md](./frontend/CLAUDE.md)
- **AI Model**: [ai-model/CLAUDE.md](./ai-model/CLAUDE.md)

## 🔐 보안 요구사항

### 인증/인가 플로우

```
1. 사용자 회원가입
   └── status: PENDING

2. 관리자 로그인
   └── 승인 대기 목록 조회

3. 관리자 승인/거부
   └── status: APPROVED/REJECTED

4. 승인된 사용자만 로그인 가능
   └── 세션 생성

5. 모든 활동 AuditLog에 기록
   └── IP, UserAgent, Action, Timestamp
```

### 보안 설정 체크리스트

- [ ] 세션 기반 인증 활성화
- [ ] CSRF 보호 설정
- [ ] 파일 업로드 제한 (크기, 형식)
- [ ] SQL Injection 방지 (JPA 사용)
- [ ] XSS 방지 (입력값 검증)
- [ ] 감사 로그 모든 API에 적용

## 🚢 배포 가이드

### 개발 환경 → 운영 환경

```bash
# 1. 오프라인 패키지 준비
./scripts/build-all.sh

# 2. 배포 패키지 생성
./scripts/package-offline.sh

# 3. 폐쇄망 서버에서 배포
tar -xzf chatgemma-offline.tar.gz
cd chatgemma
./deploy-offline.sh
```

### Docker Compose (운영)

```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  # AI 모델
  ollama:
    image: chatgemma/ollama:latest
    deploy:
      resources:
        limits:
          memory: 16G
    networks: [gemma-network]

  # 백엔드
  backend:
    image: chatgemma/backend:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_URL=jdbc:oracle:thin:@db:1521:XE
    depends_on: [ollama, database]
    networks: [gemma-network]

  # 프론트엔드
  frontend:
    image: chatgemma/frontend:latest
    ports: ["80:80"]
    depends_on: [backend]
    networks: [gemma-network]

  # 데이터베이스
  database:
    image: container-registry.oracle.com/database/express:21.3.0-xe
    environment:
      - ORACLE_PWD=${DB_PASSWORD}
    volumes:
      - db-data:/opt/oracle/oradata
    networks: [gemma-network]
```

## 📊 모니터링

### 주요 메트릭

- **응답 시간**: P50 < 2s, P95 < 5s
- **가용성**: > 99.5%
- **메모리 사용량**: < 80%
- **디스크 사용량**: < 70%
- **AI 모델 처리량**: > 1 req/sec

### 로그 모니터링

```bash
# 애플리케이션 로그
docker-compose logs -f backend

# AI 모델 로그
docker-compose logs -f ollama

# 감사 로그 조회 (admin 필요)
curl -X GET "http://localhost:8080/api/admin/audit-logs" \
  --cookie "JSESSIONID=${SESSION_ID}"
```

## 🧪 테스트 전략

### 테스트 레벨

1. **단위 테스트** (80%+ 커버리지)
   - Service Layer 로직 테스트
   - Domain Model 검증
   - Utility 함수 테스트

2. **통합 테스트**
   - Repository 테스트 (DataJpaTest)
   - WebSocket 통신 테스트
   - AI 모델 연동 테스트

3. **E2E 테스트**
   - 전체 사용자 플로우 검증
   - 관리자 승인 프로세스
   - 채팅 시나리오

```bash
# 전체 테스트 실행
./scripts/test-all.sh

# 커버리지 리포트 생성
./scripts/coverage-report.sh
```

## ⚠️ 제약사항 및 주의사항

### 개발 원칙
- ✅ TDD 철저히 준수
- ✅ 오버스펙 금지
- ✅ 테스트 우회/하드코딩 금지
- ✅ 비즈니스 맥락 이해 기반 개발

### 기술적 제약
- Java 17+ 필수
- React 18+ 사용
- Oracle 19c 호환성 유지
- Gemma 라이선스 준수

### 운영 제약
- 오프라인 환경 완전 지원
- 보안 감사 요구사항 충족
- 관리자 승인 프로세스 필수

## 📚 참고 자료

### 공식 문서
- [Gemma 3n Model Card](https://ai.google.dev/gemma/docs/gemma-3n)
- [Ollama Documentation](https://ollama.com/docs)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [React Documentation](https://react.dev/)

### 개발 가이드
- [Backend 개발 가이드](./backend/CLAUDE.md)
- [Frontend 개발 가이드](./frontend/CLAUDE.md)
- [AI Model 통합 가이드](./ai-model/CLAUDE.md)

## 🤝 기여 가이드

1. 이슈 생성 및 논의
2. Feature 브랜치 생성
3. TDD로 개발 (테스트 먼저!)
4. Pull Request 생성
5. 코드 리뷰 및 머지

---

> 💡 **시작 전에**: 각 컴포넌트별 CLAUDE.md 파일을 먼저 읽어보세요!
> 🚀 **개발 시작**: `./scripts/setup.sh`로 환경을 구성한 후 시작하세요!