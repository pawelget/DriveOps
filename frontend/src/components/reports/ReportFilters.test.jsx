import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import ReportFilters from "./ReportFilters";

const defaultFilters = {
  search: "",
  warsztat: "",
  samochod_id: "",
  date_from: "",
  date_to: "",
  cost_min: "",
  cost_max: "",
};

const cars = [
  {
    id: 1,
    marka: "Toyota",
    model: "Corolla",
    numer_rejestracyjny: "WA12345",
  },
  {
    id: 2,
    marka: "Honda",
    model: "Civic",
    numer_rejestracyjny: "KR54321",
  },
];

describe("ReportFilters", () => {
  test("renderuje pola filtrowania i pojazdy", () => {
    render(
      <ReportFilters
        filters={defaultFilters}
        cars={cars}
        onChange={jest.fn()}
        onClear={jest.fn()}
      />
    );

    expect(screen.getByPlaceholderText(/szukaj/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/warsztat/i)).toBeInTheDocument();
    expect(screen.getByText("Toyota Corolla (WA12345)")).toBeInTheDocument();
    expect(screen.getByText("Honda Civic (KR54321)")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /wyczysc filtry/i })).toBeInTheDocument();
  });

  test("wywoluje onChange po wpisaniu tekstu w search", async () => {
    const onChange = jest.fn();

    render(
      <ReportFilters
        filters={defaultFilters}
        cars={cars}
        onChange={onChange}
        onClear={jest.fn()}
      />
    );

    await userEvent.type(screen.getByPlaceholderText(/szukaj/i), "olej");

    expect(onChange).toHaveBeenCalled();
    expect(onChange).toHaveBeenLastCalledWith({
      ...defaultFilters,
      search: "j",
    });
  });

  test("wywoluje onChange po zmianie warsztatu", async () => {
    const onChange = jest.fn();

    render(
      <ReportFilters
        filters={defaultFilters}
        cars={cars}
        onChange={onChange}
        onClear={jest.fn()}
      />
    );

    await userEvent.type(screen.getByPlaceholderText(/warsztat/i), "Auto");

    expect(onChange).toHaveBeenCalled();
  });

  test("wywoluje onChange po wyborze pojazdu", async () => {
    const onChange = jest.fn();

    render(
      <ReportFilters
        filters={defaultFilters}
        cars={cars}
        onChange={onChange}
        onClear={jest.fn()}
      />
    );

    const select = screen.getByDisplayValue("Wszystkie pojazdy");

    await userEvent.selectOptions(select, "1");

    expect(onChange).toHaveBeenCalledWith({
      ...defaultFilters,
      samochod_id: "1",
    });
  });

  test("wywoluje onChange dla dat i kosztow", async () => {
    const onChange = jest.fn();

    render(
      <ReportFilters
        filters={defaultFilters}
        cars={cars}
        onChange={onChange}
        onClear={jest.fn()}
      />
    );

    await userEvent.type(screen.getByPlaceholderText(/koszt od/i), "100");
    await userEvent.type(screen.getByPlaceholderText(/koszt do/i), "500");

    expect(onChange).toHaveBeenCalled();
  });

  test("wywoluje onClear po kliknieciu wyczysc", async () => {
    const onClear = jest.fn();

    render(
      <ReportFilters
        filters={defaultFilters}
        cars={cars}
        onChange={jest.fn()}
        onClear={onClear}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: /wyczysc filtry/i }));

    expect(onClear).toHaveBeenCalled();
  });
});