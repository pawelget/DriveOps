import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import PasswordForm from "./PasswordForm";
import { changePassword } from "../../api/UserApi";


jest.mock("../../api/UserApi", () => ({
  changePassword: jest.fn(),
}));

describe("PasswordForm", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("renderuje formularz zmiany hasla", () => {
    render(<PasswordForm />);

    expect(screen.getByText("Zmiana hasla")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /zmien haslo/i })).toBeInTheDocument();
  });

  test("pokazuje blad gdy nowe hasla sa rozne", async () => {
    render(<PasswordForm />);

    const obecneHasloInput = screen.getByPlaceholderText(/^Obecne haslo$/i);
    const noweHasloInput = screen.getByPlaceholderText(/^Nowe haslo/i);
    const powtorzHasloInput = screen.getByPlaceholderText(/^Powtorz nowe haslo$/i);

    await userEvent.type(obecneHasloInput, "Stare123!");
    await userEvent.type(noweHasloInput, "Nowe123!");
    await userEvent.type(powtorzHasloInput, "Inne123!");

    await userEvent.click(screen.getByRole("button", { name: /zmien haslo/i }));

    expect(screen.getByText("Nowe hasla nie sa identyczne")).toBeInTheDocument();
    expect(changePassword).not.toHaveBeenCalled();
  });

  test("zmienia haslo poprawnie", async () => {
    changePassword.mockResolvedValue({});

    render(<PasswordForm />);

   const obecneHasloInput = screen.getByPlaceholderText(/^Obecne haslo$/i);
    const noweHasloInput = screen.getByPlaceholderText(/^Nowe haslo/i);
    const powtorzHasloInput = screen.getByPlaceholderText(/^Powtorz nowe haslo$/i);

    await userEvent.type(obecneHasloInput, "Stare123!");
    await userEvent.type(noweHasloInput, "Nowe123!");
    await userEvent.type(powtorzHasloInput, "Nowe123!");

    await userEvent.click(screen.getByRole("button", { name: /zmien haslo/i }));

    await waitFor(() => {
        expect(changePassword).toHaveBeenCalledWith({
            obecne_haslo: "Stare123!",
            nowe_haslo: "Nowe123!",
        });
    });

    expect(screen.getByText("Haslo zostalo zmienione")).toBeInTheDocument();
  });
});