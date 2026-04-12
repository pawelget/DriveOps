const API_URL = process.env.REACT_APP_API_URL || "http://127.0.0.1:5000";

export async function apiFetch(endpoint, options = {}) {
  const token = localStorage.getItem("token");

  const config = {
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {}),
    },
    ...options,
  };

  const response = await fetch(`${API_URL}${endpoint}`, config);

  const data = await response.json().catch(() => ({}));

  if (!response.ok) {
    throw new Error(data.error || "Wystapil blad");
  }

  return data;
}