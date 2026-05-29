# Consultancy Scheduling and Seminar Platform

This is a runnable Spring Boot + React Native consultancy platform build from scratch.

## Backend Features

- Google OAuth login endpoint with real Google token verification.
- Admin email/password login with bootstrap admin from properties.
- JWT access tokens and rotating refresh tokens.
- RBAC roles: `SUPER_ADMIN`, `CONSULTANT`, `MODERATOR`, `USER`.
- Consultant profile management.
- Availability rules and dynamic slot generation.
- One-to-one consultation booking.
- Seminar creation and registration with capacity locking.
- Razorpay order creation, checkout verification, and webhook signature verification.
- STOMP/WebSocket meeting chat with message persistence.
- FCM token registration and notification history.
- AI meeting summary and support endpoints with pluggable provider.
- Admin users, payments, analytics, bans, and announcements.
- Flyway MySQL schema, Docker, Docker Compose, Actuator, Swagger/OpenAPI.

## Run Backend

```powershell
cd consultancy-platform
docker compose up --build
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

Admin bootstrap defaults in `docker-compose.yml`:

```text
admin@example.com / ChangeMe123!
```

For production, replace these environment variables:

```text
JWT_SECRET
BOOTSTRAP_ADMIN_EMAIL
BOOTSTRAP_ADMIN_PASSWORD
GOOGLE_CLIENT_ID
RAZORPAY_KEY_ID
RAZORPAY_KEY_SECRET
RAZORPAY_WEBHOOK_SECRET
AI_PROVIDER
OPENAI_API_KEY
FIREBASE_SERVICE_ACCOUNT_PATH
```

## Run Mobile

This folder contains React Native source and package configuration. Create/install the native project shell if needed, then use this `src` folder as the app source.

```powershell
cd consultancy-platform/mobile
npm install
npm start
```

## Production Notes

External systems still require real credentials and console setup:

- Google OAuth client ID.
- Razorpay account keys and webhook secret.
- Firebase project and service account wiring.
- OpenAI API key if `AI_PROVIDER=openai`.
- Apple/Facebook login provider verifier if those providers are enabled.

The backend is designed so those provider implementations can be added without changing the core domain flows.
