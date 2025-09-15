# ChatGemma Frontend - React SPA with FSD Architecture

## 🎯 프로젝트 개요

Claude UI와 ChatGPT를 벤치마킹한 **라이트 버전 UI**를 ShadCN으로 구현한 React 기반 채팅 인터페이스입니다. **FSD(Feature-Sliced Design)** 아키텍처를 준수하여 확장 가능하고 유지보수하기 쉬운 구조로 설계되었습니다.

### 🎨 디자인 방향성

#### 1. **미니멀 라이트 테마**
- **색상 팔레트**: 화이트/라이트 그레이 기반의 클린한 디자인
- **타이포그래피**: 가독성이 좋은 Sans-serif 폰트
- **간격**: 넉넉한 패딩과 마진으로 여백미 강조
- **그림자**: 미묘한 box-shadow로 깊이감 표현

#### 2. **Claude UI 벤치마킹 요소**
- 좌측 사이드바의 채팅 리스트
- 중앙의 메시지 스레드 영역
- 하단의 입력 영역 (이미지 업로드 지원)
- 메시지 버블의 사용자/AI 구분 디자인

#### 3. **ChatGPT 인터페이스 참조**
- 심플한 헤더 영역
- 직관적인 메시지 플로우
- 타이핑 인디케이터
- 모바일 반응형 레이아웃

## 🏗️ FSD(Feature-Sliced Design) 아키텍처

```
src/
├── app/                    # 🚀 Application Layer
│   ├── providers/          # Context providers
│   ├── router/             # App routing
│   ├── store/              # Global store config
│   ├── styles/             # Global styles
│   ├── App.tsx
│   ├── main.tsx
│   └── globals.css
│
├── pages/                  # 📄 Pages Layer
│   ├── chat/               # Chat page
│   ├── auth/               # Auth pages (login/signup)
│   ├── admin/              # Admin dashboard
│   └── index.ts            # Public API
│
├── widgets/                # 🧩 Widgets Layer
│   ├── chat-sidebar/       # Chat list sidebar
│   ├── message-thread/     # Message display area
│   ├── chat-input/         # Message input widget
│   ├── auth-form/          # Authentication forms
│   └── admin-panel/        # Admin management panel
│
├── features/               # ⚡ Features Layer
│   ├── send-message/       # Message sending logic
│   ├── upload-image/       # Image upload feature
│   ├── manage-chat/        # Chat CRUD operations
│   ├── authenticate/       # Login/logout logic
│   └── admin-approve/      # User approval feature
│
├── entities/               # 🏛️ Entities Layer
│   ├── user/               # User entity
│   ├── chat/               # Chat entity
│   ├── message/            # Message entity
│   └── session/            # Session entity
│
└── shared/                 # 🔧 Shared Layer
    ├── api/                # API clients
    ├── config/             # Configuration
    ├── lib/                # Utility functions
    ├── ui/                 # ShadCN UI components
    └── types/              # TypeScript types
```

## 🛠️ 개발 환경 설정

```bash
# 의존성 설치
npm install

# 개발 서버 실행
npm run dev              # http://localhost:3000

# 타입 체크
npm run type-check

# 린트
npm run lint

# 테스트
npm run test

# 빌드
npm run build
```

## 🎨 ShadCN UI 컴포넌트

### 설치된 컴포넌트
- **Button**: 다양한 variant (default, outline, ghost, link)
- **Card**: 컨테이너 컴포넌트
- **Input**: 입력 필드
- **Avatar**: 사용자/AI 아바타
- **ScrollArea**: 스크롤 가능한 영역
- **Separator**: 구분선

### 주요 위젯

#### 1. **ChatSidebar** (`widgets/chat-sidebar/`)
- 채팅 세션 목록 표시
- 새 채팅 생성 버튼
- 채팅 삭제 기능 (hover 시 표시)
- 현재 선택된 채팅 하이라이트

#### 2. **MessageThread** (`widgets/message-thread/`)
- 메시지 버블 (사용자/AI 구분)
- 이미지 표시 및 확대 기능
- 타이핑 인디케이터
- 자동 스크롤

#### 3. **ChatInput** (`widgets/chat-input/`)
- 텍스트 입력 (자동 높이 조절)
- 이미지 업로드 버튼
- 전송 버튼 (Enter 키 지원)
- 업로드된 이미지 미리보기

## 📦 주요 의존성

```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "tailwindcss": "^3.3.0",
    "tailwindcss-animate": "^1.0.0",
    "class-variance-authority": "^0.7.0",
    "clsx": "^2.0.0",
    "tailwind-merge": "^2.0.0",
    "lucide-react": "^0.400.0"
  }
}
```

## ⚠️ 개발 제약사항

### FSD 아키텍처 준수 규칙
1. **상위 레이어는 하위 레이어만 import 가능**
2. **같은 레이어 내 슬라이스 간 직접 import 금지**
3. **Public API를 통한 접근만 허용**

### ShadCN UI 사용 가이드
```typescript
// ✅ Good - Variant 활용
<Button variant="outline" size="sm">
  Cancel
</Button>

// ✅ Good - cn 유틸리티 사용
<div className={cn(
  "flex items-center gap-2",
  isActive && "bg-accent"
)}>

// ❌ Bad - Tailwind 직접 스타일링 (ShadCN 우선 사용)
<button className="px-4 py-2 bg-blue-500 hover:bg-blue-600">
```

---

> 💡 **개발 시작**: `npm run dev`로 개발 서버를 실행한 후 브라우저에서 확인하세요!
> 🎨 **디자인 가이드**: ShadCN 컴포넌트를 기반으로 라이트 테마를 일관성 있게 적용하세요!