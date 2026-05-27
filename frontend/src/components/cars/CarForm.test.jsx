import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import CarForm from "./CarForm";
import { createCar, updateCar } from "../../api/CarsApi";

jest.mock("../../api/CarsApi", () => ({
  createCar: jest.fn(),
  updateCar: jest.fn(),
}));

describe("CarForm", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("renderuje formularz dodawania pojazdu", () => {
    render(<CarForm onClose={jest.fn()} onSaved={jest.fn()} />);

    expect(screen.getByText("Dodaj pojazd")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /dodaj/i })).toBeInTheDocument();
  });

  test("renderuje formularz edycji pojazdu", () => {
    const car = {
      id: 1,
      marka: "Toyota",
      model: "Corolla",
      numer_rejestracyjny: "WA12345",
      vin: "123456789",
      rok_produkcji: 2020,
      przebieg: 50000,
      pojemnosc_cm3: 1800,
      moc_km: 140,
      paliwo: "benzyna",
      kolor: "czarny",
    };

    render(
      <CarForm
        car={car}
        onClose={jest.fn()}
        onSaved={jest.fn()}
      />
    );

    expect(screen.getByDisplayValue("Toyota")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Corolla")).toBeInTheDocument();
    expect(screen.getByDisplayValue("WA12345")).toBeInTheDocument();

    expect(screen.getByRole("button", { name: /zapisz/i })).toBeInTheDocument();
  });

  test("dodaje nowy pojazd", async () => {
    createCar.mockResolvedValue({});

    const onSaved = jest.fn();

    render(
      <CarForm
        onClose={jest.fn()}
        onSaved={onSaved}
      />
    );

    await userEvent.type(
      screen.getByPlaceholderText(/volkswagen/i),
      "Toyota"
    );

    await userEvent.type(
      screen.getByPlaceholderText(/golf/i),
      "Corolla"
    );

    await userEvent.type(
      screen.getByPlaceholderText(/zs12345/i),
      "WA12345"
    );

    await userEvent.type(
      screen.getByPlaceholderText(/17 znakow/i),
      "VIN123456789"
    );

    await userEvent.type(
      screen.getByPlaceholderText("2020"),
      "2022"
    );

    await userEvent.type(
      screen.getByPlaceholderText("50000"),
      "120000"
    );

    await userEvent.type(
      screen.getByPlaceholderText("1998"),
      "1800"
    );

    await userEvent.type(
      screen.getByPlaceholderText("150"),
      "140"
    );

    await userEvent.selectOptions(
      screen.getByRole("combobox"),
      "benzyna"
    );

    await userEvent.type(
      screen.getByPlaceholderText(/czarny/i),
      "bialy"
    );

    await userEvent.click(
      screen.getByRole("button", { name: /dodaj/i })
    );

    await waitFor(() => {
      expect(createCar).toHaveBeenCalled();
    });

    expect(createCar).toHaveBeenCalledWith({
      marka: "Toyota",
      model: "Corolla",
      numer_rejestracyjny: "WA12345",
      vin: "VIN123456789",
      rok_produkcji: 2022,
      pojemnosc_cm3: 1800,
      moc_km: 140,
      paliwo: "benzyna",
      przebieg: 120000,
      kolor: "bialy",
    });

    expect(onSaved).toHaveBeenCalled();
  });

  test("edytuje pojazd", async () => {
    updateCar.mockResolvedValue({});

    const onSaved = jest.fn();

    const car = {
      id: 5,
      marka: "BMW",
      model: "X5",
      numer_rejestracyjny: "WX12345",
      vin: "",
      rok_produkcji: 2018,
      przebieg: 100000,
      pojemnosc_cm3: 3000,
      moc_km: 250,
      paliwo: "diesel",
      kolor: "czarny",
    };

    render(
      <CarForm
        car={car}
        onClose={jest.fn()}
        onSaved={onSaved}
      />
    );

    const markaInput = screen.getByDisplayValue("BMW");

    await userEvent.clear(markaInput);
    await userEvent.type(markaInput, "Audi");

    await userEvent.click(
      screen.getByRole("button", { name: /zapisz/i })
    );

    await waitFor(() => {
      expect(updateCar).toHaveBeenCalled();
    });

    expect(updateCar).toHaveBeenCalledWith(
      5,
      expect.objectContaining({
        marka: "Audi",
      })
    );

    expect(onSaved).toHaveBeenCalled();
  });

  test("pokazuje blad gdy zapis sie nie powiedzie", async () => {
    createCar.mockRejectedValue(
      new Error("Blad zapisu")
    );

    render(
      <CarForm
        onClose={jest.fn()}
        onSaved={jest.fn()}
      />
    );

    await userEvent.type(
      screen.getByPlaceholderText(/volkswagen/i),
      "Toyota"
    );

    await userEvent.click(
      screen.getByRole("button", { name: /dodaj/i })
    );

    expect(
      await screen.findByText("Blad zapisu")
    ).toBeInTheDocument();
  });

  test("zamyka formularz po kliknieciu anuluj", async () => {
    const onClose = jest.fn();

    render(
      <CarForm
        onClose={onClose}
        onSaved={jest.fn()}
      />
    );

    await userEvent.click(
      screen.getByRole("button", { name: /anuluj/i })
    );

    expect(onClose).toHaveBeenCalled();
  });
});