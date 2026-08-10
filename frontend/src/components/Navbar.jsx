import { useAuth } from "../hooks/useAuth";

// Top bar shown on every dashboard. Shows who is logged in
// and a logout button.

function Navbar() {
  const { email, role, logout } = useAuth();

  return (
    <div className="navbar">
      <div className="title">Complaint Management System</div>
      <div className="right">
        <span>
          {email} ({role})
        </span>
        <button className="btn-gray" onClick={logout}>
          Logout
        </button>
      </div>
    </div>
  );
}

export default Navbar;
