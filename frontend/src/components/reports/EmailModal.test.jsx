import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import EmailModal from "./EmailModal";
import { sendReportEmail } from "../../api/ReportsApi";

jest.mock("../../api/ReportsApi", () => ({
  sendReportEmail: jest.fn(),
}));

const report = {
  id: 5,
  numer_raportu: "R/2026/00005",
};

describe("EmailModal", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("renderuje modal wysylki emaila", () => {
    render(
      <EmailModal
        report={report}
        defaultEmail="jan@example.com"
        onClose={jest.fn()}
        onSent={jest.fn()}
      />
    );

    expect(screen.getByText("Wyslij raport emailem")).toBeInTheDocument();
    expect(screen.getByText("R/2026/00005")).toBeInTheDocument();
    expect(screen.getByDisplayValue("jan@example.com")).toBeInTheDocument();
  });

  test("zamyka modal po kliknieciu anuluj", async () => {
    const onClose = jest.fn();

    render(
      <EmailModal
        report={report}
        defaultEmail="jan@example.com"
        onClose={onClose}
        onSent={jest.fn()}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: /anuluj/i }));

    expect(onClose).toHaveBeenCalled();
  });

  test("wysyla raport na domyslny email jako null", async () => {
    sendReportEmail.mockResolvedValue({
      message: "Email wyslany",
    });

    const onSent = jest.fn();

    render(
      <EmailModal
        report={report}
        defaultEmail="jan@example.com"
        onClose={jest.fn()}
        onSent={onSent}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: /wyslij/i }));

    await waitFor(() => {
      expect(sendReportEmail).toHaveBeenCalledWith(5, null);
    });

    expect(onSent).toHaveBeenCalledWith("Email wyslany");
  });

  test("wysyla raport na wpisany email", async () => {
    sendReportEmail.mockResolvedValue({
      message: "Email wyslany",
    });

    render(
      <EmailModal
        report={report}
        defaultEmail="jan@example.com"
        onClose={jest.fn()}
        onSent={jest.fn()}
      />
    );

    const input = screen.getByDisplayValue("jan@example.com");

    await userEvent.clear(input);
    await userEvent.type(input, "warsztat@example.com");

    await userEvent.click(screen.getByRole("button", { name: /wyslij/i }));

    await waitFor(() => {
      expect(sendReportEmail).toHaveBeenCalledWith(5, "warsztat@example.com");
    });
  });

  test("blokuje wysylke gdy email jest pusty", async () => {
    render(
      <EmailModal
        report={report}
        defaultEmail=""
        onClose={jest.fn()}
        onSent={jest.fn()}
      />
    );

    expect(screen.getByRole("button", { name: /wyslij/i })).toBeDisabled();
  });

  test("pokazuje blad gdy wysylka sie nie powiedzie", async () => {
    sendReportEmail.mockRejectedValue(new Error("Blad wysylki"));

    render(
      <EmailModal
        report={report}
        defaultEmail="jan@example.com"
        onClose={jest.fn()}
        onSent={jest.fn()}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: /wyslij/i }));

    expect(await screen.findByText("Blad wysylki")).toBeInTheDocument();
  });
});