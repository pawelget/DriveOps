import { apiFetch } from "./Client";
import { registerUser, loginUser, getCurrentUser } from "./AuthApi";

jest.mock("./Client", () => ({
  apiFetch: jest.fn(),
}));

describe("AuthApi", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("registerUser wysyla POST na /auth/register", async () => {
    const payload = {
      imie: "Jan",
      nazwisko: "Kowalski",
      email: "jan@example.com",
      haslo: "Testowe123!",
    };

    apiFetch.mockResolvedValue({ token: "abc" });

    await registerUser(payload);

    expect(apiFetch).toHaveBeenCalledWith("/auth/register", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  });

  test("loginUser wysyla POST na /auth/login", async () => {
    const payload = {
      email: "jan@example.com",
      haslo: "Testowe123!",
    };

    apiFetch.mockResolvedValue({ token: "abc" });

    await loginUser(payload);

    expect(apiFetch).toHaveBeenCalledWith("/auth/login", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  });

  test("getCurrentUser wysyla GET na /auth/me", async () => {
    apiFetch.mockResolvedValue({ id: 1 });

    await getCurrentUser();

    expect(apiFetch).toHaveBeenCalledWith("/auth/me", {
      method: "GET",
    });
  });
});