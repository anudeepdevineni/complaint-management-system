// A small colored pill that shows the complaint priority.

function PriorityBadge({ priority }) {
  const colors = {
    HIGH: "#dc2626", // red
    MEDIUM: "#ea580c", // orange
    LOW: "#16a34a", // green
  };

  const color = colors[priority] || "#6b7280";

  return (
    <span className="badge" style={{ backgroundColor: color }}>
      {priority}
    </span>
  );
}

export default PriorityBadge;
