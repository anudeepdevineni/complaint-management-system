import { useState, useEffect } from "react";
import { getComplaintHistory } from "../services/complaintService";
import { getErrorMessage } from "../utils/errorMessage";
import StatusBadge from "./StatusBadge";

// A popup that shows the status-change timeline (audit trail) for one complaint.
// The backend already records + serves this at GET /api/complaints/{id}/history.
function HistoryModal({ complaint, onClose }) {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    getComplaintHistory(complaint.id)
      .then((data) => {
        if (active) setHistory(data);
      })
      .catch((err) => {
        if (active) setError(getErrorMessage(err, "Could not load history."));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [complaint.id]);

  // Close when the dark backdrop (not the white box) is clicked.
  const onBackdropClick = (e) => {
    if (e.target === e.currentTarget) onClose();
  };

  return (
    <div className="modal-backdrop" onClick={onBackdropClick}>
      <div className="modal">
        <div className="modal-header">
          <h3>
            History — #{complaint.id} {complaint.title}
          </h3>
          <button className="btn-gray modal-close" onClick={onClose}>
            ✕
          </button>
        </div>

        {loading && <p>Loading history...</p>}
        {error && <p className="error-text">{error}</p>}

        {!loading && !error && history.length === 0 && (
          <p>No status changes recorded yet.</p>
        )}

        {!loading && !error && history.length > 0 && (
          <ul className="timeline">
            {history.map((h) => (
              <li key={h.id} className="timeline-item">
                <div className="timeline-dot" />
                <div className="timeline-body">
                  <div className="timeline-statuses">
                    <StatusBadge status={h.oldStatus || "—"} />
                    <span className="timeline-arrow">→</span>
                    <StatusBadge status={h.newStatus} />
                  </div>
                  <p className="timeline-meta">
                    by <strong>{h.changedBy}</strong>
                    {h.changedAt && (
                      <> · {new Date(h.changedAt).toLocaleString()}</>
                    )}
                  </p>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

export default HistoryModal;
