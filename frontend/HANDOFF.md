# Complaint System — Handoff Notes

> **These are the ONLY two files that hold AI-written project context:**
> 1. **`HANDOFF.md`** (this file) — everything you need to know: how to run it, the folder layout for Ubuntu, and every backend issue found.
> 2. **`seed.sql`** — the required starter data for the database.
>
> Every other file in this folder is just normal frontend app code (with plain comments). Nothing else is an "AI notes" dump.

---

## 1. What this project is

A college **Complaint Management System**.

- **Backend** — Spring Boot (Java 17). JWT login, MySQL database, complaints auto-classified by Google Gemini AI, auto-assigned to staff by department, and auto-escalated if unresolved for 1 hour. *(The source lives in your IntelliJ project on Ubuntu; I only reviewed the built JAR.)*
- **Frontend** — React + Vite (this folder). Login / Signup, plus Student, Staff, and Admin dashboards with charts.

Three roles: **STUDENT** (submit + view own complaints), **STAFF** (see assigned complaints + update status), **ADMIN** (see all complaints + analytics/charts).

---

## 2. Ubuntu setup — which folder to open in Claude Code

To let Claude Code fix **both** the backend and the frontend in one session, put them side-by-side under one parent folder and open the **parent**:

```
~/complaint-system/          <-- open THIS folder with `claude`
├── backend/                 <-- your Spring Boot project (move your IntelliJ project here)
└── frontend/                <-- this complaint-frontend folder (you can rename it "frontend")
```

Then run:
```bash
cd ~/complaint-system
claude
```
and paste this `HANDOFF.md` in as your first message so Claude is caught up.

> If you'd rather keep them apart, just open whichever one you're working on. But a shared parent is the easiest way to fix both at once.

Chat history does **not** move between machines — that's why this file exists. It replaces the need for the old chat.

---

## 3. How to run

**Backend** (Ubuntu, where MySQL + IntelliJ already are):
- Make sure MySQL is running and the database `complaint_system` exists.
- Run the app from IntelliJ, or: `java -jar complaint-system-0.0.1-SNAPSHOT.jar`
- It serves on **http://localhost:8080**.

**Frontend** (same machine):
```bash
cd frontend
npm install      # first time only
npm run dev
```
Opens on **http://localhost:5173**. CORS on the backend already allows this port, so they talk to each other with no changes.

---

## 4. REQUIRED first step: seed the database

The backend will not work on an empty database. **Run `seed.sql` once** (full instructions are inside that file):

```bash
mysql -u root -p complaint_system < seed.sql
```

**Creating test accounts:**
- **Student / Staff** — use the **Sign Up** page in the frontend. (For a Staff account, pick a department like `WIFI`.)
- **Admin** — the signup form only offers Student/Staff, so create the admin with **Postman**:
  ```
  POST http://localhost:8080/api/auth/signup
  Body (JSON):
  { "name": "Admin", "email": "admin@college.edu", "password": "admin123", "role": "ADMIN", "department": null }
  ```
- **To see auto-assignment work:** sign up a Staff user in the `WIFI` department, then (as a student) submit a WiFi complaint — it should auto-assign to that staff member.

---

## 5. Backend issues found (from decompiling the JAR)

Ordered by importance. Items 1 is a must-fix to run at all; 2 is worth fixing for a good demo; the rest are your call.

| # | Severity | Issue | Suggested fix |
|---|----------|-------|---------------|
| 1 | **Blocker** | **No seed data ships** with the app. Every `findByName(...)` fails on a fresh DB → "Role not found" / "Category not found". | Run `seed.sql` (Section 4). For a permanent fix, add a `data.sql` or a `CommandLineRunner` seeder to the backend. |
| 2 | **High** | **All errors return HTTP 500 with no readable message.** Services throw raw `RuntimeException` and there is **no `@RestControllerAdvice`**. So "Wrong password", "Email already exists", "Only admin allowed" all arrive as a generic 500, and the message isn't even in the body by default. | Add a `@RestControllerAdvice` that maps these to proper codes (400/403/404) **and** includes the message. Quick partial fix: add `server.error.include-message=always` to `application.properties` so the message at least shows. *(The frontend currently shows generic messages because of this — see Section 6.)* |
| 3 | Medium | **No status-transition validation.** The frontend guide claims an "Invalid status transition" error exists, but `updateStatus` doesn't validate transitions — any staff can set any status (e.g. jump OPEN → RESOLVED). It only checks the complaint is assigned to you. | If you want the documented behavior, add a check in `ComplaintService.updateStatus` for allowed transitions (OPEN→IN_PROGRESS, IN_PROGRESS→RESOLVED, ESCALATED→IN_PROGRESS, RESOLVED→OPEN). |
| 4 | Medium | **`ComplaintHistory` is dead code.** The entity exists (and a table gets created), but there is **no repository** and nothing ever writes to it — so status changes are never recorded as history. | Either implement history-writing inside `updateStatus`, or delete the `ComplaintHistory` entity to avoid an unused table. |
| 5 | Medium (security) | **No JWT authentication filter.** Spring Security is set to `permitAll()` on everything; auth is done manually in each controller via `token.substring(7)`. A request with a **missing** `Authorization` header throws a 500 (not a clean 401), and an expired/invalid token also throws 500. Also `submit` and `my-complaints` don't check the role at all. | For a robust version, add a proper JWT `OncePerRequestFilter` + method security. For a student demo it's acceptable as-is, just know the failure modes. |
| 6 | **Security** | **Secrets are hard-coded** in `application.properties` (and baked into the JAR): DB password `0000`, `jwt.secret`, and a **real-looking Gemini API key**. Anyone with the JAR has all three. | **Rotate the Gemini API key** if it's a live key. Move all three into **environment variables** and read them with `${ENV_VAR}` instead of committing them. |
| 7 | Low (docs) | The AI model is actually **`gemini-2.5-flash`** (the frontend guide says 2.0). The AI prompt only offers WIFI/ELECTRICITY/HOSTEL/MAINTENANCE; `GENERAL` only appears as the keyword-fallback default — so the `GENERAL` category **must** exist in the DB (the seed adds it) or a fallback-GENERAL result would fail on submit. | No code change needed as long as `seed.sql` was run. |

**Not a bug (verified OK):** `createdAt` is set in the Complaint constructor, so it's never null; auto-escalation is correctly enabled (`@Scheduled`, every 60 minutes) and works; CORS is correctly configured; passwords are BCrypt-hashed; JWT generation/parsing is standard and fine.

---

## 6. Frontend notes (things I did on purpose)

Two places where the real backend differs from the frontend guide, so the code does **not** follow the guide literally:

1. **`category`, `priority`, `status` are objects, not strings.** The backend serializes the full entity, so the code reads **`complaint.category.name`** (not `complaint.category`). If you ever see these columns render blank, this is why — keep the `.name`.
2. **Error messages are generic** ("Login failed…", "Signup failed…") because of backend issue #2 above — the backend doesn't send a usable error message. Once you fix #2, you can make the frontend show the real message from `error.response.data`.

**Frontend structure** (all plain React, `useState`/`useEffect`, `async/await`):
```
src/
  api/axios.js        axios instance + attaches JWT to every request
  auth.js             saveToken / getRole / getEmail (decodes the JWT)
  components/          Navbar, StatusBadge, PriorityBadge, ProtectedRoute
  pages/              Login, Signup, StudentDashboard, StaffDashboard, AdminDashboard
  App.jsx             routes + role-based redirect
  index.css           all styling
```
Backend base URL is set in `src/api/axios.js` (`http://localhost:8080`) — change it there if the backend ever moves.

---

## 7. Quick test checklist

- [ ] Run `seed.sql`, confirm the 5 lookup tables have rows.
- [ ] Start backend (8080) and frontend (5173).
- [ ] Sign up a Student → log in → land on Student dashboard.
- [ ] Sign up a Staff in `WIFI` department.
- [ ] As the student, submit a "WiFi not working" complaint → check it got a category/priority and auto-assigned to the WiFi staff.
- [ ] Log in as that Staff → see the complaint → click Start Working → Mark Resolved.
- [ ] Create an Admin via Postman → log in → see stat cards, bar chart, pie chart, and the category filter.
