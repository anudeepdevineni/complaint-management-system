import { useState, useEffect } from "react";
import { Bar, Pie } from "react-chartjs-2";
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
} from "chart.js";
import {
  getAnalytics,
  getCategorySummary,
  getComplaintsByCategory,
} from "../services/complaintService";
import { CATEGORIES } from "../constants";
import Navbar from "../components/Navbar";
import StatusBadge from "../components/StatusBadge";
import PriorityBadge from "../components/PriorityBadge";
import HistoryModal from "../components/HistoryModal";

// Chart.js needs these pieces registered once before drawing charts.
ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend
);

function AdminDashboard() {
  const [analytics, setAnalytics] = useState(null);
  const [categorySummary, setCategorySummary] = useState(null);
  const [loading, setLoading] = useState(true);

  // Category filter part
  const [selectedCategory, setSelectedCategory] = useState("WIFI");
  const [filtered, setFiltered] = useState([]);

  // Which complaint's history popup is open (null = none).
  const [historyFor, setHistoryFor] = useState(null);

  // Load the two dashboard numbers when the page opens.
  useEffect(() => {
    fetchDashboard();
  }, []);

  // Re-fetch the filtered list whenever the chosen category changes.
  useEffect(() => {
    fetchByCategory();
  }, [selectedCategory]);

  const fetchDashboard = async () => {
    try {
      const [analyticsData, categoryData] = await Promise.all([
        getAnalytics(),
        getCategorySummary(),
      ]);
      setAnalytics(analyticsData);
      setCategorySummary(categoryData);
    } catch (err) {
      console.error("Could not load dashboard:", err);
    } finally {
      setLoading(false);
    }
  };

  const fetchByCategory = async () => {
    try {
      const data = await getComplaintsByCategory(selectedCategory);
      setFiltered(data);
    } catch (err) {
      console.error("Could not filter by category:", err);
    }
  };

  if (loading) return <p style={{ padding: 20 }}>Loading dashboard...</p>;
  if (!analytics) return <p style={{ padding: 20 }}>Could not load data.</p>;

  // Data for the status bar chart.
  const statusChartData = {
    labels: ["Open", "In Progress", "Resolved", "Escalated"],
    datasets: [
      {
        label: "Complaints",
        data: [
          analytics.open,
          analytics.in_progress,
          analytics.resolved,
          analytics.escalated,
        ],
        backgroundColor: ["#f59e0b", "#3b82f6", "#16a34a", "#ef4444"],
      },
    ],
  };

  // Data for the category pie chart.
  const categoryChartData = {
    labels: Object.keys(categorySummary),
    datasets: [
      {
        data: Object.values(categorySummary),
        backgroundColor: [
          "#FF6384",
          "#36A2EB",
          "#FFCE56",
          "#4BC0C0",
          "#9966FF",
        ],
      },
    ],
  };

  const resolutionRate =
    analytics.total > 0
      ? Math.round((analytics.resolved / analytics.total) * 100)
      : 0;

  return (
    <div>
      <Navbar />
      <div className="page">
        <h1>Admin Dashboard</h1>

        {/* --- Summary number cards --- */}
        <div className="stats-grid">
          <div className="stat-card">
            <h3>Total</h3>
            <p className="stat-number">{analytics.total}</p>
          </div>
          <div className="stat-card">
            <h3>Open</h3>
            <p className="stat-number" style={{ color: "#f59e0b" }}>
              {analytics.open}
            </p>
          </div>
          <div className="stat-card">
            <h3>In Progress</h3>
            <p className="stat-number" style={{ color: "#3b82f6" }}>
              {analytics.in_progress}
            </p>
          </div>
          <div className="stat-card">
            <h3>Resolved</h3>
            <p className="stat-number" style={{ color: "#16a34a" }}>
              {analytics.resolved}
            </p>
          </div>
          <div className="stat-card">
            <h3>Escalated</h3>
            <p className="stat-number" style={{ color: "#ef4444" }}>
              {analytics.escalated}
            </p>
          </div>
          <div className="stat-card">
            <h3>Resolution Rate</h3>
            <p className="stat-number">{resolutionRate}%</p>
          </div>
        </div>

        {/* --- Charts --- */}
        <div className="charts-grid">
          <div className="chart-container">
            <h3>Status Distribution</h3>
            <Bar data={statusChartData} options={{ responsive: true }} />
          </div>
          <div className="chart-container">
            <h3>Category Breakdown</h3>
            <Pie data={categoryChartData} options={{ responsive: true }} />
          </div>
        </div>

        {/* --- Filter complaints by category --- */}
        <h2 style={{ marginTop: 30 }}>Filter by Category</h2>
        <div className="category-tabs">
          {CATEGORIES.map((cat) => (
            <button
              key={cat}
              className={selectedCategory === cat ? "active" : ""}
              onClick={() => setSelectedCategory(cat)}
            >
              {cat}
            </button>
          ))}
        </div>

        {filtered.length === 0 ? (
          <p>No complaints in this category.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Title</th>
                <th>Student</th>
                <th>Assigned Staff</th>
                <th>Department</th>
                <th>Status</th>
                <th>Priority</th>
                <th>History</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((c) => (
                <tr key={c.id}>
                  <td>{c.id}</td>
                  <td>{c.title}</td>
                  <td>{c.studentName}</td>
                  <td>{c.assignedStaffName || "Not assigned"}</td>
                  <td>{c.assignedStaffDepartment || "-"}</td>
                  <td>
                    <StatusBadge status={c.status} />
                  </td>
                  <td>
                    <PriorityBadge priority={c.priority} />
                  </td>
                  <td>
                    <button
                      className="btn-gray btn-small"
                      onClick={() => setHistoryFor(c)}
                    >
                      View
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {historyFor && (
        <HistoryModal
          complaint={historyFor}
          onClose={() => setHistoryFor(null)}
        />
      )}
    </div>
  );
}

export default AdminDashboard;
