# Deploy KodBank on the internet (Render + TiDB)

Free stack:

| Piece | Service | URL |
| ----- | ------- | --- |
| App (API + UI) | Render **Web Service** (Docker, Free) | `https://<name>.onrender.com` |
| Database | TiDB Cloud **Starter** (free, MySQL-compatible) | host:4000 |

Render injects `PORT` (default `10000`); the app listens on `${PORT}`.

---

## 1. Create a GitHub repo (project only)

This folder must be its **own** git repo (not the Windows home folder).

```bash
cd Resumeproject
git init
git add .
git commit -m "KodBank initial deploy"
```

On GitHub: **New repository** → name `kodbank` → **Create repository** (empty, no README).

Then push:

```bash
git remote add origin https://github.com/<you>/kodbank.git
git branch -M main
git push -u origin main
```

---

## 2. Create a free TiDB database

1. Sign up: https://tidbcloud.com (no credit card).
2. Create a **Serverless** cluster → plan **Free**.
3. Open **Connect** → set database name to **`atm`** (SQL: `CREATE DATABASE atm;`).
4. Copy:
   - Host
   - Port (`4000`)
   - User
   - Password
   - Set **SSL** on.

JDBC URL (used in Render env):

```text
jdbc:mysql://<HOST>:4000/atm?useSSL=true&sslMode=REQUIRED&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

If SSL handshake fails, try `sslMode=VERIFY_CA` or `useSSL=true` only.

---

## 3. Create the Render web service

1. https://dashboard.render.com → **New +** → **Web Service**.
2. Connect your GitHub repo `kodbank`.
3. Settings:
   - **Runtime**: Docker
   - **Instance**: Free
   - **Health Check Path**: `/actuator/health`
   - **Root Directory**: leave empty (repo root has the Dockerfile)
4. **Create Web Service** (Blueprint will read `render.yaml` if prompted — or configure env manually).

### Environment variables

| Key | Value |
| --- | ----- |
| `DB_URL` | JDBC URL from step 2 |
| `DB_USERNAME` | TiDB user |
| `DB_PASSWORD` | TiDB password |
| `JWT_SECRET` | Long random string (≥32 chars) |
| `CORS_ALLOWED_ORIGINS` | `https://*.onrender.com` |
| `ADMIN_USERNAME` | `admin` |
| `ADMIN_PIN` | Choose a strong PIN |
| `ADMIN_CARD` | `9999999999999999` (or custom 16 digits) |
| `ADMIN_SECURITY_ANSWER` | Your reset-PIN answer |
| `OTP_INCLUDE_CODE` | `true` (demo OTP shown on screen) |

Do **not** commit real TiDB passwords — Render env vars are enough.

---

## 4. First deploy

1. Wait for **Build → Live** (first Docker build can take 5–15 min).
2. Open `https://<name>.onrender.com` → login page.
3. Health: `https://<name>.onrender.com/actuator/health` → `{"status":"UP"}`.
4. Sign in with `ADMIN_USERNAME` / `ADMIN_PIN`.

Tables are created automatically by `schema.sql` on first boot (`CREATE TABLE IF NOT EXISTS`).

---

## 5. After deploy

- Push to `main` → Render **auto-deploys** (`autoDeploy: true` in `render.yaml`).
- Free instances **spin down** after ~15 min idle; first request takes ~1 min.
- `.github/workflows/keep-awake.yml` pings `/actuator/health` every 10 min (public repo = free Actions minutes) so the instance stays up. GitHub pauses scheduled runs after 60 days with no repo activity — re-enable under the workflow's **Actions** tab if that happens.
- When you add SMS/email OTP, set `OTP_INCLUDE_CODE=false`.

---

## 6. Keep it fast and stable (important on Free)

Render gives the app a short window (~1–2 min) to bind its port before the deploy
fails with `Port scan timeout reached`. Startup budget comes from three places:

| Cost | Where | Fix |
| ---- | ----- | --- |
| Hibernate reading DB metadata at boot (~40 s) | `application.yml` | `hibernate.boot.allow_jdbc_metadata_access: false` + explicit `MySQLDialect` |
| Opening 10 DB connections at boot (~9 s) | Hikari | `minimum-idle: 2`, `maximum-pool-size: 8` |
| JVM heap capped at ~128 MB → GC thrash | `Dockerfile` | `JAVA_TOOL_OPTIONS` sets `MaxRAMPercentage=50` + `TieredStopAtLevel=1` |
| Legacy migration table scans every boot | `render.yaml` | `SPRING_PROFILES_ACTIVE=prod` disables `LegacyDataMigrator` |

Watch the deploy log line `Started AtmApplication in N seconds` — it must stay well under
~60 s or the port scan fails. Once the database is provisioned you can set
`DB_INIT_MODE=never` in Render to skip re-running `schema.sql` on every boot
(put it back to `always` after creating a brand new database).

### Put the app and database in the same region

Your TiDB cluster is in **ap-southeast-1 (Singapore)** while Render runs in **Oregon** —
every SQL round trip costs ~170 ms. Fix (pick one):

- **Recommended:** create a new TiDB Serverless cluster in **AWS us-west-2 (Oregon)**, then
  update `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` in Render. The app recreates tables on boot.
- Or recreate the Render service with `region: singapore` (region is fixed at creation) and
  re-enter its env vars.

Add timeouts to the JDBC URL as well:

```text
...&serverTimezone=UTC&connectTimeout=10000&socketTimeout=60000
```

---

## Local check (optional)

```bash
# frontend only builds into Docker; local API:
cd backend && mvn spring-boot:run
cd frontend && npm run dev
```
