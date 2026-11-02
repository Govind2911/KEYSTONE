# Project KEYSTONE — Field Service Management Platform

Full-stack platform for **Meridian Facilities Management**: dispatchers raise and assign work orders, technicians work them
from a phone, managers watch SLAs and dashboards, customers raise and track requests.

**Stack:** Java 21 · Spring Boot 3.5 · Spring Security + JWT · Spring Data JPA · MySQL 8 · Flyway · springdoc OpenAPI ·
React 18 + TypeScript (Vite).

## Quick start (Docker — one command)
```bash
cp .env.example .env        # fill DB_PASSWORD, DB_ROOT_PASSWORD, JWT_SECRET (openssl rand -base64 48)
docker compose up --build
```
Front end http://localhost:5173 · API http://localhost:8080 · Swagger http://localhost:8080/swagger-ui.html

## Local setup (without Docker)
```bash
# 1. database
mysql -uroot -p -e "create database keystone character set utf8mb4"
# 2. backend (Java 21 + Maven)
cd backend
export DB_USERNAME=root DB_PASSWORD=yourpassword JWT_SECRET="$(openssl rand -base64 48)"
mvn spring-boot:run          # Flyway creates the schema + demo data automatically on first start
# 3. frontend
cd ../frontend && npm install && npm run dev      # http://localhost:5173 (proxies /api to :8080)
```
Using your existing database? Set `DB_NAME=<your db>`; it must be empty (Flyway owns the schema).

## Environment variables
| Variable | Required | Purpose |
|---|---|---|
| `DB_USERNAME`, `DB_PASSWORD` | yes | MySQL credentials |
| `JWT_SECRET` | yes | ≥ 32 chars, signs tokens. Never commit. |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | no | default `localhost` / `3306` / `keystone` |
| `DB_SSL` | no | `true` for managed DBs that require TLS |
| `CORS_ORIGINS` | no | comma list of allowed front-end origins |
| `UPLOAD_DIR` | no | attachment folder (default `./uploads`) |
| `JWT_EXPIRY_MINUTES` | no | default 480 |
| `VITE_API_URL` | prod front end | deployed API base URL (build-time) |

## Migrations & seed
`backend/src/main/resources/db/migration`: `V1__schema.sql` (tables, constraints, append-only trigger), `V2__seed_demo_data.sql` (demo users, customers,
sites, parts, 18 work orders in every state) and `V3__cloud_storage_skills_category.sql` (Cloudinary columns, technician skills,
work-order category). All run automatically. Remove V2 (and the demo `update` lines in V3) for a production database with no demo data.

## Seed logins — password for all: `Password@123`
| Role | Email |
|---|---|
| Manager | manager@meridian.test |
| Dispatcher | dispatcher@meridian.test |
| Technician | tech1@meridian.test · tech2@meridian.test · tech3@meridian.test |
| Customer | customer1@northgate.test · customer2@harbormed.test · customer3@westfield.test |

## Architecture
```
React SPA ─▶ Controllers (thin: validate, authorise, delegate)
             ─▶ Services (business rules, state machine, SLA, @Transactional)
                ─▶ Spring Data JPA repositories ─▶ MySQL (Flyway-managed)
Cross-cutting: Spring Security + JWT · @RestControllerAdvice errors · in-app notifications · file storage
```
Entities never leave the service layer; everything crossing the API is a DTO.

### Work-order lifecycle (`WorkOrderStateMachine`)
| From → To | Who |
|---|---|
| NEW → ASSIGNED (assign) | Dispatcher, Manager |
| NEW / ASSIGNED → CANCELLED | Dispatcher, Manager |
| ASSIGNED → IN_PROGRESS (start) | the assigned Technician only |
| IN_PROGRESS ⇄ ON_HOLD, IN_PROGRESS → COMPLETED | the assigned Technician only |
| COMPLETED → CLOSED / → IN_PROGRESS (reopen) | Manager |

Anything else → **409**; right move, wrong person → **403**; CLOSED/CANCELLED are terminal. Every change writes a
`work_order_status_history` row (a DB trigger rejects UPDATE/DELETE on that table).

### Security highlights
- BCrypt passwords; JWT signed with env secret, expiring; user re-loaded from DB each request (deactivation is instant).
- `@PreAuthorize` on every controller + ownership checks in services. **No public registration.**
- Customers: all queries scoped to their organisation; another customer's order/site → **404**; internal fields (assignee, SLA, costs, actors, notes) are omitted from their JSON.
- Technicians only see/act on their assigned jobs. Parts logging uses `SELECT … FOR UPDATE` + a DB `CHECK (stock_qty >= 0)`.
- Errors share one JSON shape and never include stack traces. Uploads: type whitelist, UUID storage keys, path-traversal guard.

### SLA
Due date = created + priority window (Urgent 4h · High 8h · Medium 24h · Low 72h). A scheduler (every 60 s, and
`POST /api/sla/scan`) flags work at 75 % of its window (at risk) and past due (breach) and notifies managers/dispatchers.

## API docs
Swagger UI at `/swagger-ui.html`, OpenAPI JSON at `/v3/api-docs`. Click **Authorize** and paste the token from `/api/auth/login`.

## Tests
```bash
cd backend && mvn test
```
`WorkOrderStateMachineTest` (pure unit) and `ApiSecurityAndLifecycleIT` (real MySQL via Testcontainers, needs Docker; skipped otherwise)
cover: illegal transitions, technician-cannot-close, cross-customer access, stock-never-negative, full lifecycle.

## Deployment (suggested)
Database: any managed MySQL 8 (Railway, Aiven, PlanetScale-compatible…). Backend: Docker web service from `backend/`
(set the env vars above; mount a persistent volume at `UPLOAD_DIR`). Front end: static host (Netlify/Vercel/Render static)
with `VITE_API_URL=https://<your-api>` at build time; add its URL to `CORS_ORIGINS`. Health check: `/actuator/health`.

## Attachments & Cloudinary
Photos, PDFs and text files (max 10 MB) can be added to any open work order by technicians, staff and the owning customer.
- **Storage:** `STORAGE_PROVIDER=auto` (default) uses **Cloudinary** when `CLOUDINARY_URL=cloudinary://API_KEY:API_SECRET@CLOUD_NAME`
  is set, otherwise the server disk (`UPLOAD_DIR`). Each file remembers where it lives, so switching later never loses old files.
- **Upload:** `POST /api/work-orders/{id}/attachments` (multipart field `file`) → signed upload to Cloudinary through the REST API.
- **Stream / view:** `GET /api/attachments/{id}/view` (inline, for previews). **Download:** `GET /api/attachments/{id}/download`.
  Both stream through the back end, so the same access rules as the work order apply (another customer's file → 404).
- **URL:** every attachment returns `url` and `downloadUrl`; staff also get `publicUrl` (direct Cloudinary link) for Cloudinary files.
- **Delete:** `DELETE /api/attachments/{id}` (uploader or manager) also removes the file from Cloudinary.
- Cloudinary free plans block *PDF delivery* by default: enable it in Cloudinary → Settings → Security ("Allow delivery of PDF and ZIP files").

## Extra features beyond the core spec (brief section 13.1 stretch goals)
- **Skill-based dispatch:** work orders have a type (HVAC / Electrical / Plumbing / General); technicians have skills (set in Users).
  The assign dropdowns list matching technicians first (★), then least-loaded. `GET /api/users/technicians?category=HVAC`.
- **Audit log viewer:** Manager → *Audit log* shows every status change across all work orders (`GET /api/audit`), built from the append-only history.
- **E-mail notifications (optional):** set `SPRING_MAIL_HOST` etc. (see `.env.example`); e-mails are sent only after the change commits.
- **Integration tests** for lifecycle, authorisation, attachments and dispatch (`ApiSecurityAndLifecycleIT`).
