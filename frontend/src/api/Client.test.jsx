import { apiFetch } from "./Client";

describe("Client apiFetch", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();

    global.fetch = jest.fn();
  });

  test("wysyla request bez tokena", async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ message: "ok" }),
    });

    const result = await apiFetch("/test");

    expect(global.fetch).toHaveBeenCalledWith("http://127.0.0.1:5000/test", {
      headers: {
        "Content-Type": "application/json",
      },
    });

    expect(result).toEqual({ message: "ok" });
  });

  test("dodaje token do naglowka Authorization", async () => {
    localStorage.setItem("token", "abc-token");

    global.fetch.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ user: { id: 1 } }),
    });

    await apiFetch("/auth/me");

    expect(global.fetch).toHaveBeenCalledWith("http://127.0.0.1:5000/auth/me", {
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer abc-token",
      },
    });
  });

  test("przekazuje metode, body i dodatkowe naglowki", async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ created: true }),
    });

    const body = JSON.stringify({ name: "test" });

    await apiFetch("/items", {
      method: "POST",
      body,
      headers: {
        "X-Test": "123",
      },
    });

    expect(global.fetch).toHaveBeenCalledWith("http://127.0.0.1:5000/items", {
    method: "POST",
    body,
    headers: {
        "X-Test": "123",
        },
    });
  });

  test("rzuca blad z backendu gdy response.ok jest false", async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      json: jest.fn().mockResolvedValue({
        error: "Nieprawidlowe dane",
      }),
    });

    await expect(apiFetch("/error")).rejects.toThrow("Nieprawidlowe dane");
  });

  test("rzuca domyslny blad gdy backend nie zwroci error", async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      json: jest.fn().mockResolvedValue({}),
    });

    await expect(apiFetch("/error")).rejects.toThrow("Wystapil blad");
  });

  test("dziala gdy odpowiedz nie ma poprawnego JSON", async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      json: jest.fn().mockRejectedValue(new Error("Invalid JSON")),
    });

    const result = await apiFetch("/empty");

    expect(result).toEqual({});
  });
});