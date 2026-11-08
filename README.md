# Campus Connect — Backend API

REST API for **Campus Connect**, a privacy-first connection platform for verified university students.
Frontend repo: `campus-connect-frontend` (React).

## Highlights
- **Auth:** email/password (BCrypt) and **Google Sign-In** (server-side ID-token verification), stateless **JWT**
- **Privacy by design:** a user's WhatsApp number is stored privately and exposed *only* when a connection request between two users is `ACCEPTED` — enforced in one place (`ProfileMapper`)
- Profiles with photo upload, interests, intent, visibility toggle, "verified student" badge by email domain
- Discover with dynamic filters + pagination + explainable compatibility score
- Connection lifecycle: send → accept/decline/cancel → remove; mutual-interest auto-match; daily limit and decline cooldown
- Safety: block, report, deactivate account · In-app notifications · Email heads-up

## Tech stack
Java 17 · Spring Boot 3.3 (Web, Security, Data JPA, Validation, Mail) · Hibernate 6 · MySQL 8 · jjwt · Google API client · Lombok · Maven · Docker

## Architecture (layered)

HTTP ─▶ Security filters (CORS, JWT) ─▶ Controller ─▶ Service ─▶ Repository ─▶ MySQL
(DTOs in/out)  (rules,      (Spring Data
@Transactional) JPA/Hibernate)

com.campusconnect
config/      AppProperties, SecurityConfig, WebConfig (static /uploads)
security/    JwtService, JwtAuthFilter
controller/  thin HTTP layer: parse request, call service, return DTO
service/     business rules (Auth, Profile, Discover, Connection, Safety, Notification, Email, WhatsApp)
repository/  Spring Data interfaces + JPQL queries
entity/      JPA entities (database tables)
dto/         request/response records (API contract; entities are never exposed)
exception/   ApiException + GlobalExceptionHandler (uniform JSON errors)
util/        PhoneUtil (E.164 normalisation)


## Run locally
**With Docker (MySQL + API):**
```bash
cp .env.example .env      # fill JWT_SECRET, GOOGLE_CLIENT_ID
docker compose up --build # API on http://localhost:8080


Without Docker: install JDK 17+, MySQL 8; then mvn spring-boot:run 
(set env vars in your IDE run configuration; DB campus_connect is auto-created, tables via ddl-auto: update).

### Configuration (Environment Variables)

* **DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD**: MySQL connection credentials.
* **JWT_SECRET**: HMAC key for tokens (must be at least 32 characters, keep secret).
* **GOOGLE_CLIENT_ID**: Must match the frontend's `VITE_GOOGLE_CLIENT_ID`.
* **CORS_ORIGINS**: Comma-separated list of allowed frontend origins.
* **FRONTEND_URL / PUBLIC_BASE_URL**: Used for links in emails and absolute photo URLs.
* **VERIFIED_DOMAINS**: e.g., `unideb.hu` grants the "Verified student" badge.
* **MAIL_***: Configuration for the email channel.
* **WHATSAPP_***: Configuration for the WhatsApp channel.

---

## Notification channels

* **In-app (bell + Requests page)**: Always active, stored in the `notifications` table.
* **Email**: Active when `MAIL_ENABLED=true` combined with SMTP settings. If false, emails are only printed to the console log.
* **WhatsApp Cloud API**: Implemented but currently dormant (`WHATSAPP_MODE=log`). Meta bills per template message, so it is reserved for a future rollout.

**Email setup (Gmail):** 
1. Go to your Google Account security settings and enable **2-Step Verification**.
2. Search for **App passwords** and create a new one.
3. Set `MAIL_ENABLED=true`, `MAIL_USERNAME=<your-gmail>`, `MAIL_PASSWORD=<16-char app password>`, and `MAIL_FROM=<your-gmail>`.

**Enable WhatsApp later (no code changes required):** 
Create a Meta developer app, get your Phone Number ID and token, and create an approved template. Set `WHATSAPP_MODE=cloud`, `WHATSAPP_PHONE_NUMBER_ID`, and `WHATSAPP_ACCESS_TOKEN`. Even without the API, WhatsApp chat links (`wa.me`) unlock automatically for both users once a connection request is accepted.

---

## Photo storage

* **local (default)**: Stores files on the server disk under `./uploads`, served locally at `/uploads/**` (use for local development only).
* **cloudinary**: Uses Cloudinary via signed REST upload; the database stores the secure cloud URL (`https://res.cloudinary.com/...`) for **production**.

**Setup:** Create a free Cloudinary account, copy your Cloud name, API Key, and API Secret from the dashboard, and set `STORAGE_PROVIDER=cloudinary` along with your credentials. The application will fail to start if the provider is set to `cloudinary` but any credential is missing.

---

## API Overview

* **Auth**: `POST /api/auth/register`, `/login`, `/google`, and `GET /api/auth/me`
* **Profile**: `GET/PUT /api/profile/me`, `POST /api/profile/me/complete`, `POST /api/profile/me/photos`, `DELETE /api/profile/me/photos/{i}`, `PUT .../{i}/primary`, `GET /api/profiles/{userId}`
* **Discover**: `GET /api/discover?q&gender&university&department&year&interest&page`, `GET /api/discover/top`
* **Connections**: `POST /api/connections/request/{userId}`, `GET /api/connections[/received|/sent|/summary]`, `POST .../{id}/accept|decline`, `DELETE .../{id}/cancel`, `DELETE .../{id}`
* **Notifications**: `GET /api/notifications`, `POST .../read-all`, `POST .../{id}/read`
* **Safety / Account**: `POST/DELETE /api/safety/block/{id}`, `GET /api/safety/blocked`, `POST /api/safety/report`, `PUT /api/account/visibility`, `DELETE /api/account`

Errors return a uniform JSON format, such as: `{ "status": 409, "message": "You already sent a request to this person" }`.

---

## Deployment Notes

* Any container host works (Render, Railway, Fly.io, etc.). Provide a MySQL-compatible database and the environment variables listed above; the app reads the `PORT` automatically.
* **Photos in production go to Cloudinary** (`STORAGE_PROVIDER=cloudinary`). Cloud containers use ephemeral file systems, meaning anything saved to a local `uploads/` folder will vanish on every redeploy.
* Change `ddl-auto` to `validate` and introduce proper database migrations (like Flyway) before onboarding real production users.

---

## Known Limitations (Deliberate Scope)

* No automated tests implemented yet.
* No refresh tokens or aggressive login rate-limiting.
* Emails are currently sent before database transactions commit (ideal practice is using `@TransactionalEventListener(AFTER_COMMIT)`).
* "Top matches" scoring calculates in memory over the latest 100 profiles.
* No automatic image resizing or advanced CDN transforms yet.  



