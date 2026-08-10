import api from "../api/axios";

// All the auth-related API calls live here, so components don't build URLs.

// Logs in and returns the raw JWT token string.
export async function login(email, password) {
  const response = await api.post("/api/auth/login", { email, password });
  return response.data;
}

// Creates a new account. Students have no department; staff must pass one.
export async function signup({ name, email, password, role, department }) {
  const response = await api.post("/api/auth/signup", {
    name,
    email,
    password,
    role,
    department: role === "STAFF" ? department : null,
  });
  return response.data;
}
