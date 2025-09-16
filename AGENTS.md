# Repository Guidelines

## Project Structure & Module Organization
- backend/: Spring Boot (Java 17). Source in `backend/src/main/java`, tests in `backend/src/test/java`, configs in `backend/src/main/resources`. Build artifacts in `backend/build/`. Entry point: `com.chatgemma.ChatGemmaApplication`.
- frontend/: Vite + React + TypeScript. App code in `frontend/src` (shared libs in `src/shared`, widgets in `src/widgets`, pages in `src/pages`), assets in `frontend/public`.
- ai-model/: Ollama + Gemma model. Docker files and scripts in `ai-model/`.

## Build, Test, and Development Commands
- Backend — run API (dev): `cd backend && ./gradlew bootRun`
- Backend — unit tests + coverage: `cd backend && ./gradlew test` (HTML report: `backend/build/jacocoHtml/index.html`)
- Backend — build JAR: `cd backend && ./gradlew build`
- Frontend — dev server: `cd frontend && npm i && npm run dev`
- Frontend — build: `cd frontend && npm run build`
- Frontend — lint: `cd frontend && npm run lint`
- AI model — start locally: `cd ai-model && docker compose up -d ollama && ./scripts/setup.sh`

## Coding Style & Naming Conventions
- Java: 4-space indent; packages lowercase; classes `PascalCase`; methods/fields `camelCase`; constants `UPPER_SNAKE_CASE`. Suffixes: `*Controller`, `*Service`, `*Repository`, DTOs under `dto/`.
- TypeScript/React: 2-space indent; components `PascalCase` (`ChatPage.tsx`), variables/hooks `camelCase`; colocate UI in `src/shared/ui` and `src/widgets/*/ui`. Use ESLint via `npm run lint`.

## Testing Guidelines
- Frameworks: JUnit 5, Mockito, Spring Test (backend). Place tests mirroring package paths under `backend/src/test/java` with `*Test.java` names.
- Coverage: JaCoCo minimum 80% (excludes controllers/config/DTOs). Run `./gradlew test`; report at `backend/build/jacocoHtml/index.html`.
- Frontend: No test runner configured yet; keep components pure and lint-clean. If adding tests, prefer Vitest + React Testing Library.

## Commit & Pull Request Guidelines
- Commits: Use Conventional Commits (e.g., `feat: add chat sidebar`, `fix: null check in AuthService`). Keep scope small and messages imperative.
- PRs: Include description, linked issues, and screenshots/GIFs for UI changes. Ensure tests pass, coverage holds, and `npm run lint` is clean. Note any config or migration steps.

## Security & Configuration Tips
- Profiles: `application.yml` with `application-dev.yml`/`application-prod.yml`. Provide secrets via environment variables; never commit credentials.
- AI model: For air‑gapped deploys, see `ai-model/README.md` and use `docker-compose.prod.yml` with preloaded models.

