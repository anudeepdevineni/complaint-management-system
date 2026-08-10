// Small helper functions to work with the login token.
// The token is a JWT: three parts separated by dots.
// The middle part holds the data (email + role) as base64 JSON.

// Save the token after login.
export function saveToken(token) {
  localStorage.setItem("jwtToken", token);
}

// Remove the token (logout).
export function logout() {
  localStorage.removeItem("jwtToken");
}

// Read the token back.
export function getToken() {
  return localStorage.getItem("jwtToken");
}

// Decode the middle part of the token into an object.
// Returns null if there is no token or it looks broken.
export function getPayload() {
  const token = getToken();
  if (!token) return null;
  try {
    const middle = token.split(".")[1];
    return JSON.parse(atob(middle));
  } catch {
    return null;
  }
}

// Get the user's role: "STUDENT", "STAFF", "ADMIN", or null.
export function getRole() {
  const payload = getPayload();
  return payload ? payload.role : null;
}

// Get the user's email (stored as "sub" in the token).
export function getEmail() {
  const payload = getPayload();
  return payload ? payload.sub : null;
}
