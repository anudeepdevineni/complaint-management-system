import { useNavigate } from "react-router-dom";
import { getEmail, getRole, logout as clearToken } from "../auth";

// A small hook that bundles the current user's info and a logout action,
// so components don't each import the auth helpers separately.
export function useAuth() {
  const navigate = useNavigate();

  const logout = () => {
    clearToken();
    navigate("/login");
  };

  return {
    email: getEmail(),
    role: getRole(),
    logout,
  };
}
