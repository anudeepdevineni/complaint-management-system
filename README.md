# Complaint Management System

A full-stack college complaint portal. Students file complaints; an **AI classifies**
each one into a category and priority and **auto-assigns** it to the least-busy staff
member of the right department; staff move it through a status lifecycle; stale
complaints **auto-escalate**; and admins get analytics, charts, and a full audit trail.

> **Tech:** React 19 · Spring Boot 3 · MySQL · JWT auth · Google Gemini AI
> **Full-stack** — React SPA served by the Spring Boot backend as a single deployable JAR.

![Admin dashboard](docs/admin-dashboard.png)

---

## Screenshots & demos

| | |
|---|---|
| **Submit a complaint** (student) — AI auto-classifies & assigns it | **Admin — history timeline** (audit trail) |
| ![Submit complaint](docs/submit-complaint.gif) | ![History timeline](docs/history-timeline.gif) |

**Admin — filter complaints by category**

![Category filter](docs/admin-filter.gif)

<p align="center">
  <img src="docs/login.png" alt="Login" width="45%">
  &nbsp;
  <img src="docs/signup.png" alt="Signup" width="45%">
</p>

---

## Features

- **Role-based access** — Student, Staff, and Admin, each with a distinct dashboard, enforced by JWT.
- **AI complaint classification** — each complaint is sent to Google Gemini to pick a category
  (WiFi / Electricity / Hostel / Maintenance / General) and priority; falls back to keyword rules
  if the AI is unavailable, so it never breaks.
- **Smart auto-assignment** — routes each complaint to the staff member in the matching
  department who currently has the fewest active complaints, spreading the load.
- **Status lifecycle with validation** — `OPEN → IN_PROGRESS → RESOLVED`, with reopen and
  escalation paths; invalid transitions are rejected.
- **Automatic escalation** — a scheduled job flags complaints left too long as `ESCALATED`.
- **Audit trail** — every status change is recorded (who, when, from → to) and viewable by admins
  in a timeline UI.
- **Analytics dashboard** — status distribution (bar chart), category breakdown (pie chart),
  resolution rate, and a category filter.

---

## Architecture

```
  Browser  ──►  Spring Boot (:8080)  ──►  MySQL
  React SPA      REST API + JWT auth       (JPA / Hibernate)
  (served from       │
   the same JAR)     └──►  Google Gemini API  (AI classification,
                              with keyword fallback if unavailable)
```

The React app is built and bundled **inside** the Spring Boot JAR, so the whole system
runs as one process on port 8080 — no separate frontend server needed in production.

| Layer    | Technology |
|----------|-----------|
| Frontend | React 19, Vite, React Router, axios, Chart.js |
| Backend  | Spring Boot 3, Java 17, Spring Web, Spring Security, Spring Data JPA, Spring Scheduling |
| Auth     | JWT (jjwt), BCrypt password hashing |
| Database | MySQL |
| AI       | Google Gemini (`gemini-2.0-flash`) with a keyword fallback |

---

## Running it locally

**Prerequisites:** Java 17, Node 20+, MySQL, and a MySQL database named `complaint_system`.

### Option A — one command (production-style single JAR)

```bash
./build.sh
java -jar backend/target/complaint-system-0.0.1-SNAPSHOT.jar
```

Then open **http://localhost:8080**. Lookup tables (roles, departments, categories, etc.)
seed themselves on first startup.

### Option B — separate dev servers (hot reload while developing)

```bash
# Terminal 1 — backend on :8080
cd backend && ./mvnw spring-boot:run

# Terminal 2 — frontend on :5173 (proxies /api to :8080)
cd frontend && npm install && npm run dev
```

### Configuration

The backend reads these from environment variables (with safe local defaults):

| Variable | Purpose |
|----------|---------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection |
| `JWT_SECRET`, `JWT_EXPIRATION` | JWT signing |
| `GEMINI_API_KEY` | Google Gemini key — **optional**; without it the keyword fallback is used |

---

## Roles

| Role    | Can do |
|---------|--------|
| **Student** | Submit complaints, view their own |
| **Staff**   | View complaints assigned to them, advance their status |
| **Admin**   | View all complaints, filter by category, see analytics and per-complaint history |

---

## Project layout

```
complaint-management-system/
├── frontend/   React + Vite single-page app
├── backend/    Spring Boot REST API (serves the built frontend too)
└── build.sh    builds frontend + backend into one runnable JAR
```

A deeper walkthrough of every file, endpoint, and design decision lives in
[`frontend/PROJECT_OVERVIEW.md`](frontend/PROJECT_OVERVIEW.md).
