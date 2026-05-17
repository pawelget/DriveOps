import { apiFetch } from "./Client";

export function updateProfile(payload) {
  return apiFetch("/auth/me", {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function changePassword(payload) {
  return apiFetch("/auth/change-password", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function deactivateAccount(payload) {
  return apiFetch("/auth/deactivate", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}
