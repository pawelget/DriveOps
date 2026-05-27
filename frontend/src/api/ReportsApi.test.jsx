import {
  getReports,
  generateReport,
  sendReportEmail,
  deleteReport,
  downloadReportPdf,
} from "./ReportsApi";
import { apiFetch } from "./Client";

jest.mock("./Client", () => ({
  apiFetch: jest.fn(),
}));

describe("ReportsApi", () => {
  beforeEach(() => {
    jest.clearAllMocks();

    global.fetch = jest.fn();

    localStorage.clear();

    window.URL.createObjectURL = jest.fn(() => "blob:test-url");
    window.URL.revokeObjectURL = jest.fn();
  });

  test("getReports bez filtrow wysyla GET na /raporty", async () => {
    apiFetch.mockResolvedValue([]);

    await getReports();

    expect(apiFetch).toHaveBeenCalledWith("/raporty", {
      method: "GET",
    });
  });

  test("getReports z filtrami buduje query string", async () => {
    apiFetch.mockResolvedValue([]);

    await getReports({
      search: "olej",
      date_from: "2026-01-01",
      date_to: "2026-12-31",
      warsztat: "auto",
      cost_min: 100,
      cost_max: 500,
      samochod_id: 1,
    });

    expect(apiFetch).toHaveBeenCalledWith(
      "/raporty?search=olej&date_from=2026-01-01&date_to=2026-12-31&warsztat=auto&cost_min=100&cost_max=500&samochod_id=1",
      { method: "GET" }
    );
  });

  test("generateReport wysyla POST na /raporty/generuj/:id", async () => {
    apiFetch.mockResolvedValue({ id: 1 });

    await generateReport(7);

    expect(apiFetch).toHaveBeenCalledWith("/raporty/generuj/7", {
      method: "POST",
    });
  });

  test("sendReportEmail wysyla email gdy podano adres", async () => {
    apiFetch.mockResolvedValue({ message: "ok" });

    await sendReportEmail(3, "test@example.com");

    expect(apiFetch).toHaveBeenCalledWith("/raporty/3/wyslij-email", {
      method: "POST",
      body: JSON.stringify({ email: "test@example.com" }),
    });
  });

  test("sendReportEmail wysyla pusty body gdy brak adresu", async () => {
    apiFetch.mockResolvedValue({ message: "ok" });

    await sendReportEmail(3);

    expect(apiFetch).toHaveBeenCalledWith("/raporty/3/wyslij-email", {
      method: "POST",
      body: JSON.stringify({}),
    });
  });

  test("deleteReport wysyla DELETE na /raporty/:id", async () => {
    apiFetch.mockResolvedValue({ message: "ok" });

    await deleteReport(9);

    expect(apiFetch).toHaveBeenCalledWith("/raporty/9", {
      method: "DELETE",
    });
  });

  test("downloadReportPdf pobiera PDF i tworzy link", async () => {
    localStorage.setItem("token", "abc-token");

    const clickMock = jest.fn();

    const realLink = document.createElement("a");
    realLink.click = clickMock;

    jest.spyOn(document, "createElement").mockReturnValue(realLink);

    global.fetch.mockResolvedValue({
      ok: true,
      blob: jest.fn().mockResolvedValue(new Blob(["pdf"])),
    });

    await downloadReportPdf(4, "R/2026/00004");

    expect(global.fetch).toHaveBeenCalledWith(
      "http://127.0.0.1:5000/raporty/4/pdf",
      {
        headers: {
          Authorization: "Bearer abc-token",
        },
      }
    );

    expect(window.URL.createObjectURL).toHaveBeenCalled();
    expect(clickMock).toHaveBeenCalled();
    expect(window.URL.revokeObjectURL).toHaveBeenCalledWith("blob:test-url");

    document.createElement.mockRestore();
  });

  test("downloadReportPdf rzuca blad gdy backend zwroci error", async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      json: jest.fn().mockResolvedValue({
        error: "Brak pliku PDF",
      }),
    });

    await expect(downloadReportPdf(4, "R/2026/00004")).rejects.toThrow(
      "Brak pliku PDF"
    );
  });
});