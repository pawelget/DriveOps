import React from "react";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import ServiceForm from "./ServiceForm";
import { createService, getServiceTypes } from "../../api/ServicesApi";
import { getCars } from "../../api/CarsApi";

jest.mock("../../api/ServicesApi", () => ({
  createService: jest.fn(),
  getServiceTypes: jest.fn(),
}));

jest.mock("../../api/CarsApi", () => ({
  getCars: jest.fn(),
}));

const carsMock = [
  {
    id: 1,
    marka: "Toyota",
    model: "Corolla",
    numer_rejestracyjny: "WA12345",
  },
];

const typesMock = [
  {
    id: 2,
    nazwa: "Wymiana oleju",
  },
];

describe("ServiceForm", () => {
  beforeEach(() => {
    jest.clearAllMocks();

    getCars.mockResolvedValue(carsMock);
    getServiceTypes.mockResolvedValue(typesMock);
  });

  test("renderuje formularz i pobiera pojazdy oraz rodzaje serwisu", async () => {
    render(<ServiceForm onClose={jest.fn()} onSaved={jest.fn()} />);

    expect(screen.getByText("Dodaj wpis serwisowy")).toBeInTheDocument();

    expect(await screen.findByText("Toyota Corolla (WA12345)")).toBeInTheDocument();
    expect(await screen.findByText("Wymiana oleju")).toBeInTheDocument();

    expect(getCars).toHaveBeenCalled();
    expect(getServiceTypes).toHaveBeenCalled();
  });

  test("pokazuje blad gdy nie wybrano pojazdu", async () => {
    render(<ServiceForm onClose={jest.fn()} onSaved={jest.fn()} />);

    await userEvent.click(screen.getByRole("button", { name: /zapisz serwis/i }));

    expect(screen.getByText("Wybierz pojazd")).toBeInTheDocument();
    expect(createService).not.toHaveBeenCalled();
  });

  test("dodaje i usuwa czynnosc", async () => {
    render(<ServiceForm onClose={jest.fn()} onSaved={jest.fn()} />);

    await userEvent.click(screen.getByRole("button", { name: /\+ dodaj czynnosc/i }));

    const taskNameInputs = screen.getAllByPlaceholderText(/np\. wymiana oleju/i);
    expect(taskNameInputs).toHaveLength(2);

    const removeButtons = screen.getAllByRole("button", { name: "×" });
    await userEvent.click(removeButtons[0]);

    expect(screen.getAllByPlaceholderText(/np\. wymiana oleju/i)).toHaveLength(1);
  });

  test("dodaje i usuwa czesc", async () => {
    render(<ServiceForm onClose={jest.fn()} onSaved={jest.fn()} />);

    expect(screen.getByText(/Brak czesci/i)).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: /\+ dodaj czesc/i }));

    expect(screen.getByPlaceholderText(/np\. olej 5w30/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/mobil/i)).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "×" }));

    expect(screen.getByText(/Brak czesci/i)).toBeInTheDocument();
  });

  test("zamyka formularz po kliknieciu anuluj", async () => {
    const onClose = jest.fn();

    render(<ServiceForm onClose={onClose} onSaved={jest.fn()} />);

    await userEvent.click(screen.getByRole("button", { name: /anuluj/i }));

    expect(onClose).toHaveBeenCalled();
  });

  test("zapisuje serwis z czynnoscia i czescia", async () => {
    createService.mockResolvedValue({ id: 10 });

    const onSaved = jest.fn();

    render(<ServiceForm onClose={jest.fn()} onSaved={onSaved} />);

    await screen.findByText("Toyota Corolla (WA12345)");

    const selects = screen.getAllByRole("combobox");

    await userEvent.selectOptions(selects[0], "1");
    await userEvent.selectOptions(selects[1], "2");


    await userEvent.type(screen.getByPlaceholderText("Auto-Serwis"), "Super Mechanik");
    await userEvent.type(screen.getByPlaceholderText("125000"), "150000");
    await userEvent.type(screen.getByPlaceholderText("Szczecin, ul. ..."), "Warszawa 1");
    await userEvent.type(screen.getByPlaceholderText("Krotki opis serwisu"), "Opis testowy");

    await userEvent.type(screen.getByPlaceholderText("np. Wymiana oleju"), "Wymiana oleju");
    await userEvent.type(screen.getByPlaceholderText(/^Opis$/i), "Opis czynnosci");
    await userEvent.type(screen.getByPlaceholderText(/^0$/i), "120");

    await userEvent.click(screen.getByRole("button", { name: /\+ dodaj czesc/i }));

    await userEvent.type(screen.getByPlaceholderText("np. Olej 5W30"), "Olej 5W30");
    await userEvent.type(screen.getByPlaceholderText("Mobil"), "Mobil");
    await userEvent.clear(screen.getByPlaceholderText(/^1$/i));
    await userEvent.type(screen.getByPlaceholderText(/^1$/i), "2");

    const zeroInputs = screen.getAllByPlaceholderText(/^0$/i);
    await userEvent.type(zeroInputs[1], "80");

    await userEvent.click(screen.getByRole("button", { name: /zapisz serwis/i }));

    await waitFor(() => {
      expect(createService).toHaveBeenCalled();
    });

    expect(createService).toHaveBeenCalledWith(
      expect.objectContaining({
        samochod_id: 1,
        rodzaj_serwisu_id: 2,
        nazwa_warsztatu: "Super Mechanik",
        adres_warsztatu: "Warszawa 1",
        przebieg_przy_serwisie: 150000,
        opis: "Opis testowy",
        status: "zakonczony",
        zadania: [
          {
            nazwa_zadania: "Wymiana oleju",
            opis: "Opis czynnosci",
            koszt_robocizny: 120,
          },
        ],
        uzyte_czesci: [
          {
            nazwa_czesci: "Olej 5W30",
            producent_czesci: "Mobil",
            ilosc: 2,
            cena_jednostkowa: 80,
          },
        ],
      })
    );

    expect(onSaved).toHaveBeenCalled();
  });

  test("pokazuje blad gdy pobieranie danych sie nie powiedzie", async () => {
    getCars.mockRejectedValue(new Error("Blad pobierania danych"));

    render(<ServiceForm onClose={jest.fn()} onSaved={jest.fn()} />);

    expect(await screen.findByText("Blad pobierania danych")).toBeInTheDocument();
  });

  test("pokazuje blad gdy zapis serwisu sie nie powiedzie", async () => {
    createService.mockRejectedValue(new Error("Blad zapisu"));

    render(<ServiceForm onClose={jest.fn()} onSaved={jest.fn()} />);

    await screen.findByText("Toyota Corolla (WA12345)");

    const selects = screen.getAllByRole("combobox");

    await userEvent.selectOptions(selects[0], "1");

    await userEvent.click(screen.getByRole("button", { name: /zapisz serwis/i }));

    expect(await screen.findByText("Blad zapisu")).toBeInTheDocument();
  });
});