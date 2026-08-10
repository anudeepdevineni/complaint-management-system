import api from "../api/axios";

// All complaint-related API calls, grouped in one place.

// STUDENT: submit a new complaint. Returns the created complaint.
export async function submitComplaint(title, description) {
  const response = await api.post("/api/complaints", { title, description });
  return response.data;
}

// STUDENT: the complaints I submitted.
export async function getMyComplaints() {
  const response = await api.get("/api/complaints/my");
  return response.data;
}

// STAFF: the complaints assigned to me.
export async function getAssignedComplaints() {
  const response = await api.get("/api/complaints/assigned");
  return response.data;
}

// STAFF: change a complaint's status (id + new status go as query params).
export async function updateStatus(id, status) {
  const response = await api.put(
    `/api/complaints/status?id=${id}&status=${status}`
  );
  return response.data;
}

// ADMIN: dashboard numbers for the bar chart.
export async function getAnalytics() {
  const response = await api.get("/api/complaints/analytics");
  return response.data;
}

// ADMIN: per-category counts for the pie chart.
export async function getCategorySummary() {
  const response = await api.get("/api/complaints/categories/summary");
  return response.data;
}

// ADMIN: all complaints in one category.
export async function getComplaintsByCategory(category) {
  const response = await api.get(`/api/complaints/category/${category}`);
  return response.data;
}

// ADMIN: the status-change history (audit trail) for one complaint.
export async function getComplaintHistory(id) {
  const response = await api.get(`/api/complaints/${id}/history`);
  return response.data;
}
