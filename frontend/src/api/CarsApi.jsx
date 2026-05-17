import { apiFetch } from "./Client";

export function getCars() {
  return apiFetch("/samochody", {
    method: "GET",
  });
}

export function getCar(id) {
  return apiFetch(`/samochody/${id}`, {
    method: "GET",
  });
}

export function createCar(payload) {
  return apiFetch("/samochody", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateCar(id, payload) {
  return apiFetch(`/samochody/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function deleteCar(id) {
  return apiFetch(`/samochody/${id}`, {
    method: "DELETE",
  });
}
