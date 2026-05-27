import { apiFetch } from "./Client";
import { getAlerts } from "./AlertsApi";

jest.mock("./Client", () => ({
  apiFetch: jest.fn(),
}));

describe("AlertsApi", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("getAlerts bez filtrow wysyla GET na /alerts", async () => {
    apiFetch.mockResolvedValue({ alerts: [], total: 0 });

    await getAlerts();

    expect(apiFetch).toHaveBeenCalledWith("/alerts", {
      method: "GET",
    });
  });

  test("getAlerts z filtrami buduje query string", async () => {
    apiFetch.mockResolvedValue({ alerts: [], total: 0 });

    await getAlerts({
      typ: "serwis",
      priorytet: "critical",
    });

    expect(apiFetch).toHaveBeenCalledWith(
      "/alerts?typ=serwis&priorytet=critical",
      { method: "GET" }
    );
  });
});