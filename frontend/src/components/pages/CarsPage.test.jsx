import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import CarsPage from "./CarsPage";
import { getCars } from "../../api/CarsApi";

jest.mock("../../api/CarsApi", () => ({
  getCars: jest.fn(),
}));

jest.mock("../cars/CarCard", () => ({ car, onEdit, onDelete }) => (
  <div data-testid="car-card">
    <span>{car.marka} {car.model}</span>
    <span>{car.numer_rejestracyjny}</span>
    <button onClick={() => onEdit(car)}>Edytuj</button>
    <button onClick={() => onDelete(car)}>Usun</button>
  </div>
));

jest.mock("../cars/CarForm", () => ({ car, onClose, onSaved }) => (
  <div data-testid="car-form">
    <p>{car ? `Edycja: ${car.marka}` : "Dodawanie pojazdu"}</p>
    <button onClick={onSaved}>Zapisz mock</button>
    <button onClick={onClose}>Zamknij mock</button>
  </div>
));

jest.mock("../cars/DeleteConfirm", () => ({ car, onClose, onDeleted }) => (
  <div data-testid="delete-confirm">
    <p>Usuwanie: {car.marka}</p>
    <button onClick={onDeleted}>Potwierdz mock</button>
    <button onClick={onClose}>Anuluj mock</button>
  </div>
));

const carsMock = [
  {
    id: 1,
    marka: "Toyota",
    model: "Corolla",
    numer_rejestracyjny: "WA12345",
    vin: "VINTOYOTA",
  },
  {
    id: 2,
    marka: "Honda",
    model: "Civic",
    numer_rejestracyjny: "KR54321",
    vin: "VINHONDA",
  },
];

describe("CarsPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("pokazuje ladowanie i wyswietla liste pojazdow", async () => {
    getCars.mockResolvedValue(carsMock);

    render(<CarsPage />);

    expect(screen.getByText("Ladowanie pojazdow...")).toBeInTheDocument();

    expect(await screen.findByText("Toyota Corolla")).toBeInTheDocument();
    expect(screen.getByText("Honda Civic")).toBeInTheDocument();

    expect(getCars).toHaveBeenCalled();
  });

  test("pokazuje komunikat gdy nie ma pojazdow", async () => {
    getCars.mockResolvedValue([]);

    render(<CarsPage />);

    expect(
      await screen.findByText("Nie masz jeszcze zadnych pojazdow. Dodaj pierwszy!")
    ).toBeInTheDocument();
  });

  test("filtruje pojazdy po wyszukiwaniu", async () => {
    getCars.mockResolvedValue(carsMock);

    render(<CarsPage />);

    expect(await screen.findByText("Toyota Corolla")).toBeInTheDocument();

    await userEvent.type(
      screen.getByPlaceholderText(/szukaj po marce/i),
      "Honda"
    );

    expect(screen.getByText("Honda Civic")).toBeInTheDocument();
    expect(screen.queryByText("Toyota Corolla")).not.toBeInTheDocument();
  });

  test("pokazuje komunikat gdy wyszukiwanie nie znajduje pojazdu", async () => {
    getCars.mockResolvedValue(carsMock);

    render(<CarsPage />);

    expect(await screen.findByText("Toyota Corolla")).toBeInTheDocument();

    await userEvent.type(
      screen.getByPlaceholderText(/szukaj po marce/i),
      "Mercedes"
    );

    expect(
      screen.getByText("Brak pojazdow pasujacych do wyszukiwania.")
    ).toBeInTheDocument();
  });

  test("otwiera formularz dodawania pojazdu", async () => {
    getCars.mockResolvedValue(carsMock);

    render(<CarsPage />);

    await screen.findByText("Toyota Corolla");

    await userEvent.click(screen.getByRole("button", { name: /\+ dodaj pojazd/i }));

    expect(screen.getByTestId("car-form")).toBeInTheDocument();
    expect(screen.getByText("Dodawanie pojazdu")).toBeInTheDocument();
  });

  test("otwiera formularz edycji pojazdu", async () => {
    getCars.mockResolvedValue(carsMock);

    render(<CarsPage />);

    expect(await screen.findByText("Toyota Corolla")).toBeInTheDocument();

    const editButtons = screen.getAllByRole("button", { name: /edytuj/i });
    await userEvent.click(editButtons[0]);

    expect(screen.getByTestId("car-form")).toBeInTheDocument();
    expect(screen.getByText("Edycja: Toyota")).toBeInTheDocument();
  });

  test("zamyka formularz i ponownie laduje dane po zapisie", async () => {
    getCars.mockResolvedValue(carsMock);

    render(<CarsPage />);

    await screen.findByText("Toyota Corolla");

    await userEvent.click(screen.getByRole("button", { name: /\+ dodaj pojazd/i }));
    await userEvent.click(screen.getByRole("button", { name: /zapisz mock/i }));

    await waitFor(() => {
      expect(getCars).toHaveBeenCalledTimes(2);
    });

    expect(screen.queryByTestId("car-form")).not.toBeInTheDocument();
  });

  test("otwiera potwierdzenie usuwania pojazdu", async () => {
    getCars.mockResolvedValue(carsMock);

    render(<CarsPage />);

    await screen.findByText("Toyota Corolla");

    const deleteButtons = screen.getAllByRole("button", { name: /usun/i });
    await userEvent.click(deleteButtons[0]);

    expect(screen.getByTestId("delete-confirm")).toBeInTheDocument();
    expect(screen.getByText("Usuwanie: Toyota")).toBeInTheDocument();
  });

  test("zamyka potwierdzenie usuwania i ponownie laduje dane po usunieciu", async () => {
    getCars.mockResolvedValue(carsMock);

    render(<CarsPage />);

    await screen.findByText("Toyota Corolla");

    const deleteButtons = screen.getAllByRole("button", { name: /usun/i });
    await userEvent.click(deleteButtons[0]);

    await userEvent.click(screen.getByRole("button", { name: /potwierdz mock/i }));

    await waitFor(() => {
      expect(getCars).toHaveBeenCalledTimes(2);
    });

    expect(screen.queryByTestId("delete-confirm")).not.toBeInTheDocument();
  });

  test("pokazuje blad gdy pobieranie pojazdow sie nie powiedzie", async () => {
    getCars.mockRejectedValue(new Error("Blad pobierania pojazdow"));

    render(<CarsPage />);

    expect(await screen.findByText("Blad pobierania pojazdow")).toBeInTheDocument();
  });
});