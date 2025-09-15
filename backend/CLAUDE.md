# Backend - Spring Boot 애플리케이션

## 📋 요구사항

### 핵심 기능
- 세션 기반 인증/인가 시스템
- 회원가입 및 관리자 승인 워크플로우
- 채팅 메시지 저장 및 조회
- 이미지 업로드 및 처리
- 보안 감사 로깅
- Gemma 3n AI 모델 연동

### 기술 스택
- **Framework**: Spring Boot 3.4+
- **Java Version**: Java 21 LTS
- **Security**: Spring Security 6.4+ (세션 기반)
- **Database**: H2 (개발) / Oracle 19c (운영)
- **ORM**: Spring Data JPA / Hibernate 6.6+
- **WebSocket**: Spring WebSocket (실시간 채팅)
- **Testing**: JUnit 5, Mockito, RestAssured
- **Coverage**: JaCoCo 80% (Controller 제외)
- **Build Tool**: Maven 3.9+ / Gradle 8.5+

## 🏗️ 프로젝트 구조

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/chatgemma/
│   │   │   ├── config/          # 설정 클래스
│   │   │   ├── controller/      # REST API 컨트롤러
│   │   │   ├── service/         # 비즈니스 로직
│   │   │   ├── repository/      # 데이터 액세스
│   │   │   ├── entity/          # JPA 엔티티
│   │   │   ├── dto/             # 데이터 전송 객체
│   │   │   ├── security/        # 보안 관련
│   │   │   ├── websocket/       # WebSocket 핸들러
│   │   │   ├── audit/           # 감사 로깅
│   │   │   └── exception/       # 예외 처리
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/    # Flyway 마이그레이션
│   └── test/
│       └── java/com/chatgemma/  # 테스트 코드
└── pom.xml or build.gradle

```

## 📊 데이터베이스 스키마

### 주요 엔티티

#### User (사용자)
```java
- id: Long (PK)
- username: String (unique)
- password: String (encrypted)
- email: String
- role: Enum (USER, ADMIN)
- status: Enum (PENDING, APPROVED, REJECTED)
- createdAt: LocalDateTime
- approvedAt: LocalDateTime
- approvedBy: Long (FK to User)
```

#### Chat (채팅 세션)
```java
- id: Long (PK)
- userId: Long (FK)
- title: String
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
- deleted: Boolean
```

#### Message (메시지)
```java
- id: Long (PK)
- chatId: Long (FK)
- role: Enum (USER, ASSISTANT)
- content: String (TEXT)
- imageUrl: String (nullable)
- createdAt: LocalDateTime
```

#### AuditLog (감사 로그)
```java
- id: Long (PK)
- userId: Long (FK)
- action: String
- resourceType: String
- resourceId: Long
- ipAddress: String
- userAgent: String
- timestamp: LocalDateTime
- details: String (JSON)
```

## 🔐 보안 요구사항

### 인증/인가
- HttpSession 기반 세션 관리
- CSRF 보호 활성화
- 세션 타임아웃: 30분
- Remember-me 기능 (선택적)

### 관리자 승인 플로우
1. 사용자 회원가입 → status: PENDING
2. 관리자 로그인 → 승인 대기 목록 조회
3. 관리자 승인/거부 → status 업데이트
4. 승인된 사용자만 로그인 가능

### 감사 로깅
- 모든 API 호출 기록
- 로그인/로그아웃 이벤트
- 데이터 변경 이력
- IP 주소 및 User-Agent 저장

## 🧪 TDD 전략

### 테스트 작성 순서
1. Domain Entity 테스트
2. Repository 테스트 (DataJpaTest)
3. Service 테스트 (단위 테스트)
4. Controller 테스트 (MockMvc)
5. Integration 테스트 (E2E)

### 테스트 커버리지 목표
- Service Layer: 90%+
- Repository Layer: 80%+
- Domain Logic: 95%+
- Controller: E2E 테스트로 대체
- 전체 목표: 80%+ (Controller 제외)

### 테스트 원칙
- Given-When-Then 패턴 사용
- 각 테스트는 독립적으로 실행 가능
- 테스트 데이터는 @TestDataBuilder 패턴 사용
- 외부 의존성은 Mock 처리
- 실제 비즈니스 시나리오 기반 테스트

## 🚀 실행 명령어

```bash
# 개발 환경 실행
./gradlew bootRun --args='--spring.profiles.active=dev'

# 테스트 실행
./gradlew test

# 테스트 커버리지 확인
./gradlew jacocoTestReport
# 커버리지 리포트 확인: build/jacocoHtml/index.html

# 클린 빌드
./gradlew clean build

# 운영 빌드
./gradlew bootJar -Pprofile=prod

# H2 콘솔 접속 (개발 환경)
http://localhost:8080/h2-console
```

## 🔌 AI 모델 연동

### Ollama 서버 연동
- Host: ${OLLAMA_HOST:http://localhost:11434}
- Model: gemma3n:e4b
- Timeout: 60초
- Retry: 3회

### 멀티모달 처리
- 텍스트: 직접 전송
- 이미지: Base64 인코딩 후 전송
- 응답: Streaming 또는 Blocking 선택 가능

## 📝 환경 변수

### 개발 환경 (application-dev.yml)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    hibernate:
      ddl-auto: create-drop
  h2:
    console:
      enabled: true
```

### 운영 환경 (application-prod.yml)
```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
```

## ⚠️ 제약사항
- 오버스펙 금지
- 모든 코드는 TDD로 개발
- 테스트 우회/하드코딩 금지
- 컨트롤러는 E2E 테스트로 커버
- 로컬 H2 DB의 파일은 절대로 초기화해선 안됨.