import { apiFetch } from "./Client";

export function getAlerts(filters = {}) {
  const params = new URLSearchParams();

  if (filters.typ) params.append("typ", filters.typ);
  if (filters.priorytet) params.append("priorytet", filters.priorytet);

  const query = params.toString();
  const endpoint = query ? `/alerts?${query}` : "/alerts";

  return apiFetch(endpoint, { method: "GET" });
}
