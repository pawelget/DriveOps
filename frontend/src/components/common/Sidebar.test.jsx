import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import Sidebar from "./Sidebar";
import { getAlerts } from "../../api/AlertsApi";
import { removeToken } from "../../utils/token.jsx";

jest.mock("../../api/AlertsApi", () => ({
  getAlerts: jest.fn(),
}));

jest.mock("../../utils/token.jsx", () => ({
  removeToken: jest.fn(),
}));

describe("Sidebar", () => {
  beforeEach(() => {
    jest.clearAllMocks();

    delete window.location;
    window.location = { href: "" };
  });

  test("renderuje elementy menu", async () => {
    getAlerts.mockResolvedValue({ total: 5 });

    render(<Sidebar userName="Jan Kowalski" onNavigate={jest.fn()} />);

    expect(screen.getAllByText("DriveOps")[0]).toBeInTheDocument();
    expect(screen.getAllByText("Dashboard")[0]).toBeInTheDocument();
    expect(screen.getAllByText("Pojazdy")[0]).toBeInTheDocument();
    expect(screen.getAllByText("Serwisy")[0]).toBeInTheDocument();

    expect(await screen.findByText("5")).toBeInTheDocument();
  });

  test("wywoluje onNavigate po kliknieciu w menu", async () => {
    getAlerts.mockResolvedValue({ total: 0 });
    const onNavigate = jest.fn();

    render(<Sidebar onNavigate={onNavigate} />);

    await userEvent.click(screen.getAllByText("Pojazdy")[1]);

    expect(onNavigate).toHaveBeenCalledWith("drives");
  });

  test("wylogowuje uzytkownika", async () => {
    getAlerts.mockResolvedValue({ total: 0 });

    render(<Sidebar />);

    await userEvent.click(screen.getByRole("button", { name: /wyloguj/i }));

    expect(removeToken).toHaveBeenCalled();
    expect(window.location.href).toBe("/logowanie");
  });
});