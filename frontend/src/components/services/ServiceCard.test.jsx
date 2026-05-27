import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import ServiceCard from "./ServiceCard";

const service = {
  id: 1,
  data_serwisu: "2026-05-20",
  status: "zakonczony",
  opis: "Przeglad okresowy",
  samochod: {
    marka: "Honda",
    model: "Civic",
    numer_rejestracyjny: "WA11111",
  },
  rodzaj_serwisu: {
    nazwa: "Serwis okresowy",
  },
  nazwa_warsztatu: "Super Mechanik",
  zadania: [
    {
      id: 1,
      nazwa_zadania: "Wymiana klockow",
      koszt_robocizny: 150,
    },
  ],
  uzyte_czesci: [
    {
      id: 1,
      czesc: { nazwa: "Klocki hamulcowe" },
      ilosc: 1,
      suma: 200,
    },
  ],
  koszt_calkowity: 350,
  ma_raport: false,
  raport_id: null,
};

describe("ServiceCard", () => {
  test("wyswietla dane wpisu serwisowego", () => {
    render(<ServiceCard service={service} />);

    expect(screen.getByText("Honda Civic (WA11111)")).toBeInTheDocument();
    expect(screen.getByText("Serwis okresowy")).toBeInTheDocument();
    expect(screen.getByText(/Super Mechanik/i)).toBeInTheDocument();
    expect(screen.getByText("Zakonczony")).toBeInTheDocument();
    expect(screen.getByText(/Przeglad okresowy/i)).toBeInTheDocument();
    expect(screen.getByText(/Razem: 350.00 zl/i)).toBeInTheDocument();
  });

  test("wywoluje generowanie raportu", async () => {
    const onGenerateReport = jest.fn();

    render(<ServiceCard service={service} onGenerateReport={onGenerateReport} />);

    await userEvent.click(screen.getByRole("button", { name: /wygeneruj raport/i }));

    expect(onGenerateReport).toHaveBeenCalledWith(service);
  });

  test("pokazuje informacje gdy raport juz istnieje", () => {
    const serviceWithReport = {
      ...service,
      ma_raport: true,
      raport_id: 7,
    };

    render(<ServiceCard service={serviceWithReport} />);

    expect(screen.getByText(/Raport wygenerowany/i)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /wygeneruj raport/i })).not.toBeInTheDocument();
  });
});