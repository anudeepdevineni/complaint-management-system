// A small colored pill that shows the complaint status.

function StatusBadge({ status }) {
  const colors = {
    OPEN: "#f59e0b", // orange
    IN_PROGRESS: "#3b82f6", // blue
    RESOLVED: "#16a34a", // green
    ESCALATED: "#ef4444", // red
  };

  const color = colors[status] || "#6b7280";

  return (
    <span className="badge" style={{ backgroundColor: color }}>
      {status}
    </span>
  );
}

export default StatusBadge;
