# API Summary

Base path: `/api/v1`

## Auth

- `POST /auth/oauth/login`
- `POST /auth/admin/login`
- `POST /auth/token/refresh`

## Consultants

- `POST /consultants/me/profile`
- `POST /consultants/me/availability-rules`
- `GET /consultants`
- `GET /consultants/{consultantPublicId}`
- `GET /consultants/{consultantPublicId}/slots`

## Bookings

- `POST /bookings/consultations`
- `POST /bookings/{bookingPublicId}/cancel`

## Seminars

- `POST /seminars`
- `GET /seminars`
- `POST /seminars/{seminarPublicId}/registrations`

## Payments

- `POST /payments/razorpay/verify`
- `POST /payments/razorpay/webhook`

## Chat

- `GET /meetings/{meetingPublicId}/messages`
- `POST /messages/{messagePublicId}/read`
- WebSocket: `/ws`
- STOMP send: `/app/meetings/{meetingPublicId}/chat.send`

## AI

- `POST /meetings/{meetingPublicId}/summary/generate`
- `GET /meetings/{meetingPublicId}/summary`
- `POST /meetings/{meetingPublicId}/ai-support/messages`

## Admin

- `GET /admin/users`
- `PATCH /admin/users/{userPublicId}/ban`
- `GET /admin/payments`
- `GET /admin/analytics/overview`
- `POST /admin/announcements`
