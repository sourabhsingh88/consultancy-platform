# Consultancy Scheduling & Seminar Platform

A production-ready consultancy platform providing one-to-one booking, seminar management, payments, real-time meetings, push notifications and optional AI-powered meeting summaries. The backend is implemented in Spring Boot and the mobile client is built with React Native.

## Key Features
- Authentication: Google OAuth and email/password admin bootstrap
- Authorization: JWT access tokens, rotating refresh tokens and RBAC (`SUPER_ADMIN`, `CONSULTANT`, `MODERATOR`, `USER`)
- Consultant profiles, availability rules and dynamic slot generation
- One-to-one consultation booking with capacity locking for seminars
- Payments: Razorpay order creation, checkout verification and webhook signature verification
- Real-time chat/meetings via STOMP/WebSocket with persisted messages
- Push notifications via Firebase Cloud Messaging (FCM) and notification history
- AI endpoints for meeting summaries and support (pluggable provider)
- Admin features: user management, analytics, announcements and bans
- Database migrations with Flyway, Docker Compose for local development, and OpenAPI/Swagger docs

## Architecture Overview
- Backend: Spring Boot (Java), REST + WebSocket endpoints, MySQL (Flyway migrations)
- Mobile: React Native app (in `MobileApp/` and `src/`)
- Integrations: Google OAuth, Razorpay, Firebase (FCM), OpenAI or other AI providers
- Deployment: containerized via Docker/Docker Compose; suitable for Kubernetes or other orchestrators

## Tech Stack
- Spring Boot, Spring Security, WebSocket (STOMP)
- MySQL, Flyway
- React Native (mobile client)
- Docker, Docker Compose
- Razorpay, Firebase, OpenAI (optional)

## Quick Start (Local)
Prerequisites: Docker & Docker Compose installed, Git, and optional Android/iOS toolchains for mobile development.

1. Build and bring up services with Docker Compose:

```powershell
cd e:\Projects\consultancy-platform
docker compose up --build
```

2. Open the API docs in your browser:

```
http://localhost:8080/swagger-ui/index.html
```

3. Default bootstrap admin (development only) is configured in `docker-compose.yml`.

## Environment Variables
Replace these in production; sensible defaults may exist for local/dev in `docker-compose.yml`:
- `JWT_SECRET`
- `BOOTSTRAP_ADMIN_EMAIL`
- `BOOTSTRAP_ADMIN_PASSWORD`
- `GOOGLE_CLIENT_ID`
- `RAZORPAY_KEY_ID`
- `RAZORPAY_KEY_SECRET`
- `RAZORPAY_WEBHOOK_SECRET`
- `AI_PROVIDER` (e.g. `openai`)
- `OPENAI_API_KEY`
- `FIREBASE_SERVICE_ACCOUNT_PATH`

Store secrets securely (Vault, environment, or cloud secret manager) for production deployments.

## Mobile App (React Native)
The repo contains a React Native client under `MobileApp/` and source files in `src/`.

Local dev steps:

```powershell
cd e:\Projects\consultancy-platform\MobileApp
npm install
npm start
```

Follow React Native standard workflow to run on simulator or device (`npx react-native run-android` / `npx react-native run-ios`).

## Testing
- Backend unit/integration tests are in `backend/src/test/java` and can be run with Maven:

```powershell
cd e:\Projects\consultancy-platform\backend
mvn test
```

## Deployment Notes
- Use Docker images built from the backend `Dockerfile` and the mobile release pipelines for app stores.
- Configure environment variables and external integrations (Google, Razorpay, Firebase, OpenAI) before deploying.
- For scale, run the backend behind a load balancer and use a managed MySQL service with read replicas and backups.

## Contributing
- Fork and open a pull request with a clear description of changes.
- Run existing tests and linters before submitting.

## Useful Links
- API docs: `http://localhost:8080/swagger-ui/index.html` (local)
- Backend source: `backend/`
- Mobile source: `MobileApp/` and `src/`

## License & Contact
This repository does not include a license file — add one if you intend to publish. For questions or support, open an issue or contact the maintainers.

---
If you'd like, I can also:
- add a `CONTRIBUTING.md` and `CODE_OF_CONDUCT.md`
- create a sample `.env.example` for local development
- run backend tests and report results
Tell me which you'd prefer next.
