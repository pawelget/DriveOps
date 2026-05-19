import { apiFetch } from "./Client";

export function getServices() {
  return apiFetch("/wpisy-serwisowe", { method: "GET" });
}

export function getService(id) {
  return apiFetch(`/wpisy-serwisowe/${id}`, { method: "GET" });
}

export function createService(payload) {
  return apiFetch("/wpisy-serwisowe", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getServiceTypes() {
  return apiFetch("/rodzaje-serwisu", { method: "GET" });
}
