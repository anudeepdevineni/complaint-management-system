# Complaint Management System — Full Project Overview

A single reference for the **whole system** (backend + frontend): every folder, file,
function, endpoint, and data flow, plus the current state and ideas for what to build next.

> Reflects the code **after Phase 1 (make it run / error-free) and Phase 2 (reorganize + cleanup)**.
> Items added or changed in those phases are tagged **[P1]** / **[P2]**.

---

## Table of Contents
1. [What the system is](#1-what-the-system-is)
2. [Architecture & tech stack](#2-architecture--tech-stack)
3. [How to run it](#3-how-to-run-it)
4. [Roles & permissions](#4-roles--permissions)
5. [The core data flow](#5-the-core-data-flow)
6. [Database tables](#6-database-tables)
7. [Backend — every file explained](#7-backend--every-file-explained)
8. [API endpoints (complete)](#8-api-endpoints-complete)
9. [Frontend — every file explained](#9-frontend--every-file-explained)
10. [Key behaviours in detail](#10-key-behaviours-in-detail)
11. [What Phase 1 & 2 changed](#11-what-phase-1--2-changed)
12. [Current limitations / tech debt](#12-current-limitations--tech-debt)
13. [Ideas for Phase 3 (features)](#13-ideas-for-phase-3-features)

---

## 1. What the system is

A **college complaint management system**. Students file complaints; an AI classifies each
one into a category + priority; it is auto-assigned to the least-busy staff member of the
matching department; staff work it through a status lifecycle; complaints left too long
auto-escalate; admins see everything with charts and analytics.

Two separate projects:
- **Backend** — Spring Boot REST API (`complaint-system/`), talks to MySQL.
- **Frontend** — React + Vite single-page app (`complaint-frontend/`), talks to the backend over HTTP.

---

## 2. Architecture & tech stack

```
  Browser (React SPA, :5173)
        |  HTTP + JWT (Authorization: Bearer <token>)
        v
  Spring Boot REST API (:8080)
        |  JPA / Hibernate
        v
  MySQL  (database: complaint_system)
        ^
        |  (on submit) HTTP call out to Google Gemini API for classification
        +-- falls back to keyword rules if Gemini is unavailable
```

| Layer | Technology |
|-------|-----------|
| Frontend | React 19, Vite 8, React Router 7, axios, Chart.js (react-chartjs-2) |
| Backend | Spring Boot 3.2, Java 17, Spring Web, Spring Security, Spring Data JPA, Spring Scheduling |
| Auth | JWT (jjwt 0.11.5), BCrypt password hashing |
| DB | MySQL (Hibernate `ddl-auto=update` auto-creates tables) |
| AI | Google Gemini (`gemini-2.0-flash` by default) with a keyword fallback |
| Build | Maven (backend), npm/Vite (frontend) |
| Runtime | **Node 20+** required for the frontend, Java 17 for the backend |

---

## 3. How to run it

**Backend** (from `complaint-system/`):
```bash
export GEMINI_API_KEY="your-key"   # optional; without it, keyword fallback is used
mvn spring-boot:run                # serves on http://localhost:8080
```

**Frontend** (from `complaint-frontend/`, needs Node 20+ via nvm):
```bash
npm install     # first time only
npm run dev     # serves on http://localhost:5173
```

- The database `complaint_system` must exist in MySQL. Tables are auto-created by Hibernate,
  and the lookup tables auto-seed on first boot (see `DataSeeder`). **[P1]**
- Config lives in `complaint-system/src/main/resources/application.properties`. DB password,
  JWT secret, and Gemini key are read from **environment variables** with dev defaults. **[P1]**

---

## 4. Roles & permissions

| Role | Can do | Cannot do |
|------|--------|-----------|
| **STUDENT** | Submit complaints; view own complaints | Update status, see others' complaints, admin views |
| **STAFF** | View complaints assigned to them; change their status | See other staff's complaints, admin analytics |
| **ADMIN** | View all complaints, filter by category, analytics, category summary, complaint history | Submit complaints, update status |

- Signup form offers **Student / Staff** only. **Admin** is created by POSTing signup with
  `"role":"ADMIN"` directly (the API accepts it; the UI just doesn't expose it).
- Role is embedded in the JWT and checked in each controller endpoint.

---

## 5. The core data flow

```
Student submits { title, description }
      |
      v
AiService.analyzeComplaint(description)
      |  -> Gemini call (or keyword fallback) -> { category, priority }
      v
Look up Category, Priority, Status(OPEN)
      |
      v
If category has a department:  assign to the STAFF in that department
                               with the FEWEST ACTIVE complaints        [P1 bug fix + P2 rule]
If category is GENERAL:         leave unassigned
      |
      v
Save complaint (status OPEN, createdAt = now)
      |
      v
Staff moves it: OPEN -> IN_PROGRESS -> RESOLVED   (transitions validated) [P2]
                ESCALATED -> IN_PROGRESS,  RESOLVED -> OPEN
      |
      v
Every status change writes a ComplaintHistory row (who/when/from/to)     [P2]
      |
      v
EscalationScheduler (hourly): any OPEN/IN_PROGRESS complaint older than
1 hour becomes ESCALATED (also writes history)                           [P2]
```

---

## 6. Database tables

Auto-created by Hibernate; lookup tables auto-seeded by `DataSeeder`.

| Table | Key columns | Notes |
|-------|-------------|-------|
| `roles` | id, name | STUDENT, STAFF, ADMIN |
| `departments` | id, name | WIFI, ELECTRICITY, HOSTEL, MAINTENANCE |
| `priority` | id, name | LOW, MEDIUM, HIGH |
| `status` | id, name | OPEN, IN_PROGRESS, RESOLVED, ESCALATED |
| `category` | id, name, **department_id** | WIFI/ELECTRICITY/HOSTEL/MAINTENANCE map to a dept; GENERAL has none |
| `users` | id, name, email (unique), password (bcrypt, `@JsonIgnore`), **role_id**, **department_id** | department is null for students/admins |
| `complaints` | id, title, description, **status_id, category_id, priority_id, assigned_staff_id, student_id**, created_at, escalated | assigned_staff_id null = unassigned |
| `complaint_history` | id, **complaint_id, old_status_id, new_status_id, changed_by**, changed_at | changed_by null = system (auto-escalation) |

---

## 7. Backend — every file explained

Base package: `com.project.complaintsystem`

### Entry point
- **`ComplaintSystemApplication.java`** — `main()`. `@SpringBootApplication` + `@EnableScheduling`
  (turns on the escalation scheduler).

### `config/`
- **`SecurityConfig.java`** — Spring Security filter chain. CSRF disabled, CORS wired in,
  and currently **`permitAll()` on every request** (auth is done manually per-endpoint via the
  JWT, not via a security filter). OPTIONS preflight and `/api/auth/**` explicitly permitted.
- **`CorsConfig.java`** — allowed origins (`localhost:3000`, `localhost:5173`, several ngrok
  wildcards), methods (GET/POST/PUT/DELETE/OPTIONS), all headers, exposes `Authorization`,
  `allowCredentials=true`.
- **`DataSeeder.java`** **[P1]** — `CommandLineRunner`. On startup, if a lookup table is empty,
  fills it: roles, departments, priorities, statuses, and categories (mapping WIFI/…/MAINTENANCE
  to their department, GENERAL to none). Idempotent — skips tables that already have rows.

### `controller/`
- **`AuthController.java`** — `/api/auth`
  - `signup(SignupRequest)` → creates a user, returns `UserResponse`. **[P2 returns DTO]**
  - `login(LoginRequest)` → returns the JWT token string.
- **`ComplaintController.java`** — `/api/complaints`. All the complaint endpoints (see section 8).
  Helper methods **[P2]**: `tokenOf(header)` strips `"Bearer "`; `requireRole(header, role)`
  reads role from JWT and throws if it doesn't match; `toResponses(list)` maps entities → DTOs.

### `service/`
- **`AuthService.java`**
  - `signup(name,email,password,role,dept)` — rejects duplicate email, looks up role, requires a
    department only for STAFF, BCrypt-hashes the password, saves the user.
  - `login(email,password)` — verifies password with BCrypt, returns a signed JWT.
- **`ComplaintService.java`** — the heart of the app.
  - `ALLOWED_TRANSITIONS` **[P2]** — static map of which status can move to which.
  - `submitComplaint(title,description,email)` — calls AI, resolves category/priority/OPEN,
    **auto-assigns to the least-active staff in the category's department** (skips assignment for
    GENERAL / no-department). **[P1 fix + P2 rule]**
  - `getAllComplaints()`, `getMyComplaints(email)`, `getAssignedComplaints(staffEmail)`.
  - `updateStatus(id,newStatus,staffEmail)` — checks the complaint is yours, **validates the
    transition**, saves, then **records history**. **[P2]**
  - `recordHistory(...)` **[P2]** — writes one `ComplaintHistory` row.
  - `getHistory(complaintId)` **[P2]** — returns the change log for a complaint.
  - `getAnalytics()` — counts by status (total/open/in_progress/resolved/escalated).
  - `getCategorySummary()` — counts per category (for the pie chart).
  - `getComplaintsByCategory(category)`.
- **`AiService.java`**
  - `analyzeComplaint(text)` — tries `callAI`, falls back to `fallbackAnalysis` on any error.
  - `callAI(text)` — POSTs a classification prompt to Gemini; model is configurable via
    `gemini.model` (default `gemini-2.0-flash`). **[P1 model fix + configurable]**
  - `extractFromOutput(output)` — parses the AI text for a category + priority keyword.
  - `fallbackAnalysis(text)` — pure keyword rules (used when Gemini is unavailable / no key).
  - `containsAny(text, ...words)` — helper.

### `security/`
- **`JwtUtil.java`** — `generateToken(email,role)`, `extractEmail(token)`, `extractRole(token)`,
  `getKey()`. Reads `jwt.secret` / `jwt.expiration` from config.

### `scheduler/`
- **`EscalationScheduler.java`** — `@Scheduled(fixedRate = 1h)` `checkAndEscalate()`. Escalates
  any OPEN/IN_PROGRESS complaint older than 1 hour (null-safe; skips RESOLVED/already-escalated),
  and **writes a history row** for each escalation with `changedBy = null` (system). **[P2]**

### `exception/` **[P1]**
- **`GlobalExceptionHandler.java`** — `@RestControllerAdvice`. Turns exceptions into clean JSON
  `{ "status": <code>, "error": "<message>" }`:
  - `JwtException` → 401, `MissingRequestHeaderException` → 401,
  - `RuntimeException` → status inferred from message (404 "not found", 403 "allowed"/"not your",
    400 "already exists"/"wrong password"/"invalid status", else 400),
  - any other `Exception` → 500 generic.

### `dto/`
- **`LoginRequest.java`**, **`SignupRequest.java`** — request bodies.
- **`ComplaintResponse.java`** **[P2]** — flat complaint shape + `from(Complaint)` factory.
- **`UserResponse.java`** **[P2]** — flat user shape (no password).
- **`ComplaintHistoryResponse.java`** **[P2]** — flat history entry.

### `model/` (JPA entities)
- **`User`**, **`Complaint`**, **`ComplaintHistory`**, **`Role`**, **`Department`**,
  **`Category`**, **`Priority`**, **`Status`**. `Complaint` sets `createdAt`/`escalated` in its
  constructor. `User.password` is `@JsonIgnore`.

### `repository/` (Spring Data JPA)
- **`UserRepository`** — `findByEmail`, `existsByEmail`, `findByDepartment`.
- **`ComplaintRepository`** — `findByStudent`, `findByAssignedStaff`, `countByStatus_Name`,
  `countByCategory_Name`, `findByCategory_Name`, `countByAssignedStaffAndStatus_Name`,
  `countByAssignedStaffAndStatus_NameNot` (active-complaint count) **[P2]**.
- **`ComplaintHistoryRepository`** **[P2]** — `findByComplaintOrderByChangedAtAsc`.
- **`RoleRepository`**, **`DepartmentRepository`**, **`CategoryRepository`**,
  **`PriorityRepository`**, **`StatusRepository`** — each has `findByName`.

---

## 8. API endpoints (complete)

Base URL `http://localhost:8080`. All complaint endpoints require `Authorization: Bearer <jwt>`.

| Method | Path | Role | Body / Params | Returns |
|--------|------|------|---------------|---------|
| POST | `/api/auth/signup` | — | `{name,email,password,role,department}` | `UserResponse` |
| POST | `/api/auth/login` | — | `{email,password}` | JWT string |
| POST | `/api/complaints` | STUDENT | `{title,description}` | `ComplaintResponse` |
| GET | `/api/complaints/my` | STUDENT | — | `ComplaintResponse[]` |
| GET | `/api/complaints/assigned` | STAFF | — | `ComplaintResponse[]` |
| PUT | `/api/complaints/status` | STAFF | `?id=&status=` | `ComplaintResponse` |
| GET | `/api/complaints` | ADMIN | — | `ComplaintResponse[]` |
| GET | `/api/complaints/category/{category}` | ADMIN | — | `ComplaintResponse[]` |
| GET | `/api/complaints/categories/summary` | ADMIN | — | `{ WIFI: n, ... }` |
| GET | `/api/complaints/analytics` | ADMIN | — | `{ total, open, in_progress, resolved, escalated }` |
| GET | `/api/complaints/{id}/history` **[P2]** | ADMIN | — | `ComplaintHistoryResponse[]` |

**`ComplaintResponse` shape** (flat) **[P2]**:
```json
{ "id":1, "title":"", "description":"", "category":"WIFI", "priority":"LOW",
  "status":"OPEN", "studentName":"", "assignedStaffName":"", "assignedStaffDepartment":"",
  "createdAt":"2026-...", "escalated":false }
```
`assignedStaffName` is `null` when unassigned (frontend shows "Not assigned").

**Error shape** (any failure) **[P1]**: `{ "status": 400, "error": "Wrong password" }`

---

## 9. Frontend — every file explained

Root: `complaint-frontend/`. Source under `src/`.

### Entry / routing
- **`main.jsx`** — mounts `<App/>` inside `BrowserRouter` + `StrictMode`.
- **`App.jsx`** — routes: `/login`, `/signup`, and role-protected `/student`, `/staff`, `/admin`.
  `/` redirects to `/login`.
- **`index.css`** — all styling (cards, tables, badges, navbar, charts grid, buttons).
- **`index.html`**, **`vite.config.js`**, **`.oxlintrc.json`**, **`package.json`** — app shell / tooling.

### `api/`
- **`axios.js`** — one axios instance (`baseURL: http://localhost:8080`). Request interceptor
  attaches the JWT; response interceptor logs out + redirects on a 401.

### `auth.js`
- Token helpers: `saveToken`, `logout`, `getToken`, `getPayload` (decodes JWT middle part),
  `getRole`, `getEmail`.

### `services/` **[P2]** — all API calls live here (components never build URLs)
- **`authService.js`** — `login(email,password)`, `signup({...})`.
- **`complaintService.js`** — `submitComplaint`, `getMyComplaints`, `getAssignedComplaints`,
  `updateStatus`, `getAnalytics`, `getCategorySummary`, `getComplaintsByCategory`.

### `constants/index.js` **[P2]**
- `DEPARTMENTS`, `CATEGORIES`, `SIGNUP_ROLES`.

### `utils/errorMessage.js` **[P2]**
- `getErrorMessage(err, fallback)` — pulls `err.response.data.error` (the backend message),
  else the fallback.

### `hooks/useAuth.js` **[P2]**
- `useAuth()` → `{ email, role, logout }`.

### `components/`
- **`Navbar.jsx`** — top bar; shows email + role, logout button (uses `useAuth`). **[P2]**
- **`ProtectedRoute.jsx`** — wraps a page; redirects to `/login` if not logged in or wrong role.
- **`StatusBadge.jsx`** — colored pill for OPEN/IN_PROGRESS/RESOLVED/ESCALATED.
- **`PriorityBadge.jsx`** — colored pill for HIGH/MEDIUM/LOW.

### `pages/`
- **`Login.jsx`** — login form; on success saves token and routes by role. Shows real backend
  error message. **[P1/P2]**
- **`Signup.jsx`** — signup form; department dropdown appears only for STAFF. Uses constants +
  real error messages. **[P1/P2]**
- **`StudentDashboard.jsx`** — submit-complaint form + "my complaints" table. Reads flat DTO
  fields. **[P2]**
- **`StaffDashboard.jsx`** — assigned complaints as cards; status buttons that follow the
  lifecycle (Start Working / Mark Resolved / Resume Work). **[P2]**
- **`AdminDashboard.jsx`** — stat cards, status bar chart, category pie chart, and a
  category filter table. **[P2]**

---

## 10. Key behaviours in detail

### Authentication
JWT holds `sub` (email) and `role`. Frontend decodes it client-side to route and label the
navbar; backend verifies the signature on every request and reads the role for authorization.
Tokens expire after `jwt.expiration` ms (default 24h).

### AI classification
On submit, the description is sent to Gemini asking for one of WIFI/ELECTRICITY/HOSTEL/MAINTENANCE
and a priority LOW/MEDIUM/HIGH. If Gemini errors (no key, quota, network), a **keyword fallback**
picks the category/priority from words in the text. Either way the app keeps working.

### Auto-assignment  **[P1 fix + P2 rule]**
Only categories with a department get assigned. Among staff in that department, the one with the
**fewest active (not-RESOLVED) complaints** is chosen, spreading the load. GENERAL complaints stay
unassigned. (The old code assigned by fewest *resolved* and, for GENERAL, wrongly matched every
department-less user — including students.)

### Status lifecycle + validation  **[P2]**
Allowed moves: `OPEN→IN_PROGRESS`, `IN_PROGRESS→RESOLVED`, `ESCALATED→IN_PROGRESS`,
`RESOLVED→OPEN`. Anything else → `400 Invalid status transition from X to Y`. Only the assigned
staff member can change a complaint.

### History / audit trail  **[P2]**
Every status change (manual or auto-escalation) writes a `complaint_history` row with old/new
status, who changed it (or "System"), and when. Exposed to admins at `/api/complaints/{id}/history`.
**No UI for this yet** — it's a ready-made backend for a Phase-3 feature.

### Escalation
Hourly scheduler flips stale OPEN/IN_PROGRESS complaints (older than 1 hour) to ESCALATED and
flags `escalated=true`.

---

## 11. What Phase 1 & 2 changed

**Phase 1 — make it run / error-free**
- Frontend needs **Node 20+** (Vite 8) — was failing on Node 18.
- `GlobalExceptionHandler` → readable errors with proper status codes (was blank 500s).
- `DataSeeder` → DB self-seeds (no manual `seed.sql`).
- Secrets moved to **environment variables**; hard-coded Gemini key removed.
- Frontend shows the real backend error messages.
- **Bug fix:** GENERAL complaints no longer auto-assign to a random student.
- Gemini model updated (`gemini-2.5-flash` was returning 404) and made configurable.

**Phase 2 — reorganize + intermediate-level cleanup**
- Backend: response **DTOs** (no raw entities), **status-transition validation**,
  **ComplaintHistory implemented** + admin endpoint, controller helpers, removed dead code.
- Frontend: new **services / constants / utils / hooks** layers; pages refactored to be "dumb";
  one shared error-message helper; new assignment rule (least *active* complaints).

---

## 12. Current limitations / tech debt

- **Security is demo-grade:** `SecurityConfig` is `permitAll()`; auth is manual per-endpoint via
  `token.substring(7)`. A proper JWT `OncePerRequestFilter` + method security would be the robust
  version. `submit` / `my-complaints` don't re-check the role.
- **Admin creation** is only via a raw API call (no UI).
- **History has no UI** yet.
- **2 npm high-severity advisories** in dev dependencies (left unforced to avoid breakage).
- **Gemini key** currently has no free-tier quota → keyword fallback is what actually runs.
- No automated tests beyond the default context-load test.
- Priority is only ever set by the AI; there's no manual override.

---

## 13. Ideas for Phase 3 (features)

Roughly easy → involved:

- **History timeline in the UI** — the backend already records + serves it; just add a view
  (e.g. an admin "History" button per complaint, or a student-visible timeline).
- **"Reopen" button** for students on RESOLVED complaints (`RESOLVED→OPEN` is already allowed).
- **Search / filter / sort** complaint tables (by status, priority, keyword, date).
- **Manual priority override** for staff/admin.
- **Comments / conversation thread** between student and staff on a complaint (new entity + endpoints + UI).
- **Notifications** — in-app banner or email on status change / escalation.
- **Admin user management** — list users, create admins from the UI, manually reassign complaints.
- **Harden security** — real JWT filter + method-level role checks.
- **Dark mode / general UI polish.**

Pick any of these (or your own) and we'll scope it before building.
