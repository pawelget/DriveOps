import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import ReportCard from "./ReportCard";

const report = {
  id: 1,
  numer_raportu: "R/2026/00001",
  wygenerowano_w: "2026-05-27T12:00:00",
  samochod: {
    marka: "Toyota",
    model: "Corolla",
    numer_rejestracyjny: "WA12345",
  },
  wpis_serwisowy: {
    rodzaj_serwisu_nazwa: "Wymiana oleju",
    nazwa_warsztatu: "Auto Serwis",
    data_serwisu: "2026-05-20",
    koszt_calkowity: 350,
  },
  nazwy_czynnosci: ["Wymiana oleju", "Wymiana filtra"],
};

describe("ReportCard", () => {
  test("wyswietla dane raportu", () => {
    render(<ReportCard report={report} />);

    expect(screen.getByText("R/2026/00001")).toBeInTheDocument();
    expect(screen.getByText("Toyota Corolla (WA12345)")).toBeInTheDocument();
    expect(screen.getByText(/Auto Serwis/i)).toBeInTheDocument();
    expect(screen.getByText(/350.00 zl/i)).toBeInTheDocument();
    expect(screen.getByText(/Wymiana oleju, Wymiana filtra/i)).toBeInTheDocument();
  });

  test("wywoluje akcje przyciskow", async () => {
    const onDownload = jest.fn();
    const onSendEmail = jest.fn();
    const onDelete = jest.fn();

    render(
      <ReportCard
        report={report}
        onDownload={onDownload}
        onSendEmail={onSendEmail}
        onDelete={onDelete}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: /pobierz pdf/i }));
    await userEvent.click(screen.getByRole("button", { name: /wyslij email/i }));
    await userEvent.click(screen.getByRole("button", { name: /usun/i }));

    expect(onDownload).toHaveBeenCalledWith(report);
    expect(onSendEmail).toHaveBeenCalledWith(report);
    expect(onDelete).toHaveBeenCalledWith(report);
  });
});