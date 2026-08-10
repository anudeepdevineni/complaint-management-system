// App-wide constant lists, kept in one place so pages don't hard-code them.

// Departments a staff member can belong to (used on the signup form).
export const DEPARTMENTS = ["WIFI", "ELECTRICITY", "HOSTEL", "MAINTENANCE"];

// Complaint categories the AI can assign (admin category filter uses this).
export const CATEGORIES = [
  "WIFI",
  "ELECTRICITY",
  "HOSTEL",
  "MAINTENANCE",
  "GENERAL",
];

// The roles a user can sign up as (admins are created separately).
export const SIGNUP_ROLES = ["STUDENT", "STAFF"];
