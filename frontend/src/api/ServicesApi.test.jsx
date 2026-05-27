import { apiFetch } from "./Client";
import {
  getServices,
  getService,
  createService,
  getServiceTypes,
} from "./ServicesApi";

jest.mock("./Client", () => ({
  apiFetch: jest.fn(),
}));

describe("ServicesApi", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("getServices wysyla GET na /wpisy-serwisowe", async () => {
    apiFetch.mockResolvedValue([]);

    await getServices();

    expect(apiFetch).toHaveBeenCalledWith("/wpisy-serwisowe", {
      method: "GET",
    });
  });

  test("getService wysyla GET na /wpisy-serwisowe/:id", async () => {
    apiFetch.mockResolvedValue({ id: 5 });

    await getService(5);

    expect(apiFetch).toHaveBeenCalledWith("/wpisy-serwisowe/5", {
      method: "GET",
    });
  });

  test("createService wysyla POST na /wpisy-serwisowe", async () => {
    const payload = {
      samochod_id: 1,
      data_serwisu: "2026-05-27",
      opis: "Test",
    };

    apiFetch.mockResolvedValue({ id: 10 });

    await createService(payload);

    expect(apiFetch).toHaveBeenCalledWith("/wpisy-serwisowe", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  });

  test("getServiceTypes wysyla GET na /rodzaje-serwisu", async () => {
    apiFetch.mockResolvedValue([]);

    await getServiceTypes();

    expect(apiFetch).toHaveBeenCalledWith("/rodzaje-serwisu", {
      method: "GET",
    });
  });
});