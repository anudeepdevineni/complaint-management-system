import { useState, useEffect } from "react";
import { getAssignedComplaints, updateStatus } from "../services/complaintService";
import { getErrorMessage } from "../utils/errorMessage";
import Navbar from "../components/Navbar";
import StatusBadge from "../components/StatusBadge";
import PriorityBadge from "../components/PriorityBadge";

function StaffDashboard() {
  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchAssigned();
  }, []);

  const fetchAssigned = async () => {
    try {
      const data = await getAssignedComplaints();
      setComplaints(data);
    } catch (err) {
      console.error("Could not load assigned complaints:", err);
    } finally {
      setLoading(false);
    }
  };

  const changeStatus = async (id, newStatus) => {
    try {
      await updateStatus(id, newStatus);
      fetchAssigned(); // refresh so the new status shows
    } catch (err) {
      alert(getErrorMessage(err, "Could not update status. Please try again."));
    }
  };

  return (
    <div>
      <Navbar />
      <div className="page">
        <h1>Staff Dashboard</h1>
        <h2>My Assigned Complaints</h2>

        {loading ? (
          <p>Loading...</p>
        ) : complaints.length === 0 ? (
          <p>No complaints assigned to you yet.</p>
        ) : (
          complaints.map((c) => (
            <div key={c.id} className="card complaint-card">
              <div className="header">
                <h3>{c.title}</h3>
                <PriorityBadge priority={c.priority} />
              </div>

              <p className="description">{c.description}</p>

              <div className="meta">
                <p>
                  <strong>Student:</strong> {c.studentName}
                </p>
                <p>
                  <strong>Category:</strong> {c.category || "-"}
                </p>
                <p>
                  <strong>Status:</strong> <StatusBadge status={c.status} />
                </p>
                <p>
                  <strong>Created:</strong>{" "}
                  {new Date(c.createdAt).toLocaleString()}
                </p>
              </div>

              {/* Buttons change depending on the current status. */}
              <div className="actions">
                {c.status === "OPEN" && (
                  <button
                    className="btn-primary"
                    onClick={() => changeStatus(c.id, "IN_PROGRESS")}
                  >
                    Start Working
                  </button>
                )}
                {c.status === "IN_PROGRESS" && (
                  <button
                    className="btn-success"
                    onClick={() => changeStatus(c.id, "RESOLVED")}
                  >
                    Mark Resolved
                  </button>
                )}
                {c.status === "ESCALATED" && (
                  <button
                    className="btn-warning"
                    onClick={() => changeStatus(c.id, "IN_PROGRESS")}
                  >
                    Resume Work
                  </button>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

export default StaffDashboard;
