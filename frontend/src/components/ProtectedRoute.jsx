import { Navigate } from "react-router-dom";
import { getRole } from "../auth";

// Wraps a page so only the right role can see it.
// - If nobody is logged in -> go to /login.
// - If logged in but wrong role -> go to /login too.
// "role" is the role this page needs (e.g. "STUDENT").

function ProtectedRoute({ role, children }) {
  const myRole = getRole();

  if (!myRole) {
    return <Navigate to="/login" />;
  }

  if (myRole !== role) {
    return <Navigate to="/login" />;
  }

  return children;
}

export default ProtectedRoute;
