import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import RegisterForm from "./RegisterForm";
import { registerUser } from "../../api/AuthApi";
import { saveToken } from "../../utils/token";

const mockNavigate = jest.fn();

jest.mock(
  "react-router-dom",
  () => ({
    useNavigate: () => mockNavigate,
  }),
  { virtual: true }
);

jest.mock("../../api/AuthApi", () => ({
  registerUser: jest.fn(),
}));

jest.mock("../../utils/token", () => ({
  saveToken: jest.fn(),
}));

describe("RegisterForm", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("renderuje formularz rejestracji", () => {
    render(<RegisterForm />);

    expect(screen.getByText("Rejestracja")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /zarejestruj/i })).toBeInTheDocument();
  });

  test("pokazuje blad gdy hasla sa rozne", async () => {
    render(<RegisterForm />);

    await userEvent.type(screen.getByPlaceholderText(/podaj imię/i), "Jan");
    await userEvent.type(screen.getByPlaceholderText(/podaj nazwisko/i), "Kowalski");
    await userEvent.type(screen.getByPlaceholderText(/podaj email/i), "jan@example.com");
    await userEvent.type(screen.getByPlaceholderText(/podaj hasło/i), "Testowe123!");
    await userEvent.type(screen.getByPlaceholderText(/powtórz hasło/i), "Inne123!");

    await userEvent.click(screen.getByRole("button", { name: /zarejestruj/i }));

    expect(screen.getByText("Hasła nie są takie same")).toBeInTheDocument();
    expect(registerUser).not.toHaveBeenCalled();
  });

  test("rejestruje uzytkownika poprawnie", async () => {
    registerUser.mockResolvedValue({
      token: "fake-token",
    });

    render(<RegisterForm />);

    await userEvent.type(screen.getByPlaceholderText(/podaj imię/i), "Jan");
    await userEvent.type(screen.getByPlaceholderText(/podaj nazwisko/i), "Kowalski");
    await userEvent.type(screen.getByPlaceholderText(/podaj email/i), "jan@example.com");
    await userEvent.type(screen.getByPlaceholderText(/podaj hasło/i), "Testowe123!");
    await userEvent.type(screen.getByPlaceholderText(/powtórz hasło/i), "Testowe123!");
    await userEvent.type(screen.getByPlaceholderText(/podaj telefon/i), "123456789");

    await userEvent.click(screen.getByRole("button", { name: /zarejestruj/i }));

    await waitFor(() => {
        expect(registerUser).toHaveBeenCalled();
    });

    expect(saveToken).toHaveBeenCalledWith("fake-token");

    expect(mockNavigate).toHaveBeenCalledWith("/profil");
  });
});