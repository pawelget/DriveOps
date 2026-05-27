import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import ProfileForm from "./ProfileForm";
import { getCurrentUser } from "../../api/AuthApi";
import { updateProfile } from "../../api/UserApi";

jest.mock("../../api/AuthApi", () => ({
  getCurrentUser: jest.fn(),
}));

jest.mock("../../api/UserApi", () => ({
  updateProfile: jest.fn(),
}));

describe("ProfileForm", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("pokazuje ladowanie danych", () => {
    getCurrentUser.mockReturnValue(new Promise(() => {}));

    render(<ProfileForm />);

    expect(screen.getByText("Ladowanie danych...")).toBeInTheDocument();
  });

  test("wyswietla dane uzytkownika", async () => {
    getCurrentUser.mockResolvedValue({
      imie: "Jan",
      nazwisko: "Kowalski",
      email: "jan@example.com",
      telefon: "123456789",
    });

    render(<ProfileForm />);

    expect(await screen.findByDisplayValue("Jan")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Kowalski")).toBeInTheDocument();
    expect(screen.getByDisplayValue("jan@example.com")).toBeInTheDocument();
    expect(screen.getByDisplayValue("123456789")).toBeInTheDocument();
  });

  test("aktualizuje profil", async () => {
    getCurrentUser.mockResolvedValue({
      imie: "Jan",
      nazwisko: "Kowalski",
      email: "jan@example.com",
      telefon: "123456789",
    });

    updateProfile.mockResolvedValue({});

    render(<ProfileForm />);

    const imieInput = await screen.findByDisplayValue("Jan");

    await userEvent.clear(imieInput);
    await userEvent.type(imieInput, "Adam");

    await userEvent.click(screen.getByRole("button", { name: /zapisz zmiany/i }));

    await waitFor(() => {
      expect(updateProfile).toHaveBeenCalledWith({
        imie: "Adam",
        nazwisko: "Kowalski",
        email: "jan@example.com",
        telefon: "123456789",
      });
    });
    expect(screen.getByText("Dane zostaly zaktualizowane")).toBeInTheDocument();
  });
});