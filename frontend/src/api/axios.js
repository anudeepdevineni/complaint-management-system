import axios from "axios";

// One axios instance for the whole app.
// baseURL is relative ("/") so requests go to the SAME origin that served the
// page. That works both when the frontend is bundled inside the Spring Boot JAR
// (served from :8080) and wherever the app is hosted — no hard-coded host.
// For separate dev servers, Vite's proxy (see vite.config.js) forwards /api to :8080.
const api = axios.create({
  baseURL: "/",
  headers: {
    "Content-Type": "application/json",
  },
});

// Before every request: attach the JWT token if we have one.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("jwtToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// If the backend ever says 401, the token is bad -> log out.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem("jwtToken");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

export default api;
