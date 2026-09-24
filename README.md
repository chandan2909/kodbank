# KodBank

Full-stack online banking app: React (Vite) frontend + Spring Boot REST API + MySQL.

## Stack

| Layer    | Tech                          |
| -------- | ----------------------------- |
| Frontend | React 19, Vite, React Router  |
| Backend  | Spring Boot 4, Spring Security, JPA, JWT (jjwt) |
| Database | MySQL 8                       |
| Deploy   | Docker Compose (or local)     |

## Quick start (Docker)

```bash
cp .env.example .env   # edit secrets
docker compose up --build
```

App: http://localhost:8080  
Health: http://localhost:8080/actuator/health

Admin seed (override in `.env` before first boot):

- Username / PIN / card / security answer via `ADMIN_*` env vars

## Local development

### Backend

```bash
cd backend
mvn spring-boot:run
```

Defaults work against local MySQL (`root`/`admin`, db `atm`). Override with env vars from `.env.example`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Dev server: http://localhost:5173 (proxies `/api` → `http://localhost:8080`)

### Production frontend build

```bash
cd frontend
npm run build
```

Docker copies `frontend/dist` into the Spring Boot `static/` folder so the API and SPA share one origin (no CORS needed).

## Configuration (env)

See `.env.example`. Important keys:

| Variable | Purpose |
| -------- | ------- |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | MySQL connection |
| `JWT_SECRET` | JWT signing key (min 32 bytes) |
| `CORS_ALLOWED_ORIGINS` | Allowed browser origins if SPA is on another host |
| `ADMIN_*` | First-boot admin credentials |
| `OTP_INCLUDE_CODE` | `true` returns OTP in API (local/demo). Set `false` after wiring SMS/email OTP |

## API

- Base path: `/api`
- Auth: `Authorization: Bearer <accessToken>`
- Errors: `{ "status": 400, "message": "...", "errors": [] }`
- Health: `GET /actuator/health` (public)

Main groups: `/api/auth`, `/api/account`, `/api/transactions`, `/api/transfers`, `/api/beneficiaries`, `/api/otp`, `/api/admin`.

## Scripts

```bash
cd frontend
npm run lint
npm run build
npm run preview   # serves dist with /api proxy
```

```bash
cd backend
mvn -DskipTests package
```
