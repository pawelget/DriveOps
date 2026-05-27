import { apiFetch } from "./Client";
import { updateProfile, changePassword, deactivateAccount } from "./UserApi";

jest.mock("./Client", () => ({
  apiFetch: jest.fn(),
}));

describe("UserApi", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("updateProfile wysyla PATCH na /auth/me", async () => {
    const payload = {
      imie: "Jan",
      nazwisko: "Kowalski",
      email: "jan@example.com",
    };

    apiFetch.mockResolvedValue({ message: "ok" });

    await updateProfile(payload);

    expect(apiFetch).toHaveBeenCalledWith("/auth/me", {
      method: "PATCH",
      body: JSON.stringify(payload),
    });
  });

  test("changePassword wysyla POST na /auth/change-password", async () => {
    const payload = {
      obecne_haslo: "Stare123!",
      nowe_haslo: "Nowe123!",
    };

    apiFetch.mockResolvedValue({ message: "ok" });

    await changePassword(payload);

    expect(apiFetch).toHaveBeenCalledWith("/auth/change-password", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  });

  test("deactivateAccount wysyla POST na /auth/deactivate", async () => {
    const payload = {
      haslo: "Testowe123!",
    };

    apiFetch.mockResolvedValue({ message: "ok" });

    await deactivateAccount(payload);

    expect(apiFetch).toHaveBeenCalledWith("/auth/deactivate", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  });
});