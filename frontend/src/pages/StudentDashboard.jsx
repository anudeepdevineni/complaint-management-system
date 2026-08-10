import { useState, useEffect } from "react";
import { submitComplaint, getMyComplaints } from "../services/complaintService";
import { getErrorMessage } from "../utils/errorMessage";
import Navbar from "../components/Navbar";
import StatusBadge from "../components/StatusBadge";
import PriorityBadge from "../components/PriorityBadge";

function StudentDashboard() {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);

  // Load "my complaints" when the page opens.
  useEffect(() => {
    fetchMyComplaints();
  }, []);

  const fetchMyComplaints = async () => {
    try {
      const data = await getMyComplaints();
      setComplaints(data);
    } catch (err) {
      console.error("Could not load complaints:", err);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);

    try {
      const c = await submitComplaint(title, description);
      alert(
        "Complaint submitted!\n" +
          "Category: " + (c.category || "-") +
          "\nPriority: " + (c.priority || "-")
      );
      setTitle("");
      setDescription("");
      fetchMyComplaints(); // refresh the list
    } catch (err) {
      alert(getErrorMessage(err, "Could not submit complaint. Please try again."));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <Navbar />
      <div className="page">
        <h1>Student Dashboard</h1>

        {/* --- Submit a new complaint --- */}
        <div className="card submit-form">
          <h2>Submit New Complaint</h2>
          <form onSubmit={handleSubmit}>
            <input
              type="text"
              placeholder="Title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
            />
            <textarea
              placeholder="Describe your complaint..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={4}
              required
            />
            <button type="submit" disabled={submitting}>
              {submitting ? "Submitting..." : "Submit Complaint"}
            </button>
          </form>
        </div>

        {/* --- My complaints list --- */}
        <h2>My Complaints</h2>
        {loading ? (
          <p>Loading...</p>
        ) : complaints.length === 0 ? (
          <p>No complaints yet. Submit your first one above!</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Title</th>
                <th>Category</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Assigned To</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {complaints.map((c) => (
                <tr key={c.id}>
                  <td>{c.id}</td>
                  <td>{c.title}</td>
                  <td>{c.category || "-"}</td>
                  <td>
                    <PriorityBadge priority={c.priority} />
                  </td>
                  <td>
                    <StatusBadge status={c.status} />
                  </td>
                  <td>{c.assignedStaffName || "Not assigned"}</td>
                  <td>{new Date(c.createdAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

export default StudentDashboard;
