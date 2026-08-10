-- ============================================================
-- seed.sql  —  Required starter data for the Complaint System
-- ============================================================
--
-- WHY THIS IS NEEDED:
-- The backend looks up roles, departments, categories, priorities and
-- statuses BY NAME on every signup / complaint. These tables are empty
-- on a fresh database, so without this data:
--   * signup fails with "Role not found"
--   * submitting a complaint fails with "Category/Priority/Status not found"
--
-- WHEN TO RUN:
-- 1. Start the backend at least ONCE first. With ddl-auto=update,
--    Hibernate creates all the empty tables on first startup.
-- 2. Stop the backend (optional), then run this file ONCE:
--
--      mysql -u root -p complaint_system < seed.sql
--
--    (password is 0000 in application.properties)
--
-- Run it only once — re-running will create duplicate rows.
-- ============================================================

USE complaint_system;

-- Roles ------------------------------------------------------
INSERT INTO roles (name) VALUES ('STUDENT'), ('STAFF'), ('ADMIN');

-- Departments -----------------------------------------------
INSERT INTO departments (name) VALUES ('WIFI'), ('ELECTRICITY'), ('HOSTEL'), ('MAINTENANCE');

-- Priorities ------------------------------------------------
INSERT INTO priority (name) VALUES ('LOW'), ('MEDIUM'), ('HIGH');

-- Statuses --------------------------------------------------
INSERT INTO status (name) VALUES ('OPEN'), ('IN_PROGRESS'), ('RESOLVED'), ('ESCALATED');

-- Categories ------------------------------------------------
-- Each category is linked to the department that handles it, so that
-- new complaints auto-assign to staff in that department.
-- GENERAL has no department (it will simply stay unassigned).
--
-- NOTE: the foreign-key column is 'department_id' by default. If MySQL
-- says "Unknown column 'department_id'", run `DESCRIBE category;` and
-- use whatever the FK column is actually named.
INSERT INTO category (name, department_id) VALUES
  ('WIFI',        (SELECT id FROM departments WHERE name = 'WIFI')),
  ('ELECTRICITY', (SELECT id FROM departments WHERE name = 'ELECTRICITY')),
  ('HOSTEL',      (SELECT id FROM departments WHERE name = 'HOSTEL')),
  ('MAINTENANCE', (SELECT id FROM departments WHERE name = 'MAINTENANCE')),
  ('GENERAL',     NULL);

-- ============================================================
-- Done. Verify with:
--   SELECT * FROM roles;
--   SELECT * FROM departments;
--   SELECT * FROM category;
--   SELECT * FROM priority;
--   SELECT * FROM status;
-- ============================================================
