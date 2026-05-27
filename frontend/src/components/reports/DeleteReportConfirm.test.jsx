import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import DeleteReportConfirm from "./DeleteReportConfirm";
import { deleteReport } from "../../api/ReportsApi";

jest.mock("../../api/ReportsApi", () => ({
  deleteReport: jest.fn(),
}));

const report = {
  id: 7,
  numer_raportu: "R/2026/00007",
};

describe("DeleteReportConfirm", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("renderuje modal potwierdzenia usuniecia", () => {
    render(
      <DeleteReportConfirm
        report={report}
        onClose={jest.fn()}
        onDeleted={jest.fn()}
      />
    );

    expect(screen.getByText("Usun raport")).toBeInTheDocument();
    expect(screen.getByText("R/2026/00007")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /anuluj/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /usun/i })).toBeInTheDocument();
  });

  test("zamyka modal po kliknieciu anuluj", async () => {
    const onClose = jest.fn();

    render(
      <DeleteReportConfirm
        report={report}
        onClose={onClose}
        onDeleted={jest.fn()}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: /anuluj/i }));

    expect(onClose).toHaveBeenCalled();
  });

  test("usuwa raport poprawnie", async () => {
    deleteReport.mockResolvedValue({ message: "Usunieto" });
    const onDeleted = jest.fn();

    render(
      <DeleteReportConfirm
        report={report}
        onClose={jest.fn()}
        onDeleted={onDeleted}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: /usun/i }));

    await waitFor(() => {
      expect(deleteReport).toHaveBeenCalledWith(7);
    });

    expect(onDeleted).toHaveBeenCalled();
  });

  test("pokazuje blad gdy usuwanie sie nie powiedzie", async () => {
    deleteReport.mockRejectedValue(new Error("Blad usuwania"));

    render(
      <DeleteReportConfirm
        report={report}
        onClose={jest.fn()}
        onDeleted={jest.fn()}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: /usun/i }));

    expect(await screen.findByText("Blad usuwania")).toBeInTheDocument();
  });
});