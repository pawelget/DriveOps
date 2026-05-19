import { apiFetch } from "./Client";

const API_URL = process.env.REACT_APP_API_URL || "http://127.0.0.1:5000";

export function getReports(filters = {}) {
  const params = new URLSearchParams();

  if (filters.search) params.append("search", filters.search);
  if (filters.date_from) params.append("date_from", filters.date_from);
  if (filters.date_to) params.append("date_to", filters.date_to);
  if (filters.warsztat) params.append("warsztat", filters.warsztat);
  if (filters.cost_min) params.append("cost_min", filters.cost_min);
  if (filters.cost_max) params.append("cost_max", filters.cost_max);
  if (filters.samochod_id) params.append("samochod_id", filters.samochod_id);

  const query = params.toString();
  const endpoint = query ? `/raporty?${query}` : "/raporty";

  return apiFetch(endpoint, { method: "GET" });
}

export function generateReport(wpisId) {
  return apiFetch(`/raporty/generuj/${wpisId}`, {
    method: "POST",
  });
}

export function sendReportEmail(reportId, email = null) {
  return apiFetch(`/raporty/${reportId}/wyslij-email`, {
    method: "POST",
    body: JSON.stringify(email ? { email } : {}),
  });
}

export function deleteReport(reportId) {
  return apiFetch(`/raporty/${reportId}`, {
    method: "DELETE",
  });
}

/**
 * Pobiera PDF jako blob i triggeruje pobieranie w przeglądarce.
 * Nie używa apiFetch (bo to nie JSON).
 */
export async function downloadReportPdf(reportId, reportNumber) {
  const token = localStorage.getItem("token");

  const response = await fetch(`${API_URL}/raporty/${reportId}/pdf`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    throw new Error(data.error || "Nie udalo sie pobrac PDF");
  }

  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `raport_${(reportNumber || reportId).toString().replace(/\//g, "_")}.pdf`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
