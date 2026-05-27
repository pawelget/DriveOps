import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import LoginForm from './LoginForm';

jest.mock("../../api/AuthApi", () => ({
  loginUser: jest.fn(() =>
    Promise.reject(new Error("Nieprawidłowe dane logowania"))
  ),
}));

// Oszukujemy hooki nawigacyjne
jest.mock('react-router-dom', () => ({
  useNavigate: () => jest.fn(),
  Link: ({ children }) => <a>{children}</a>
}), { virtual: true });

// Oszukujemy animację dymu w Canvas
HTMLCanvasElement.prototype.getContext = () => ({
  clearRect: jest.fn(), fillRect: jest.fn(), save: jest.fn(), translate: jest.fn(),
  rotate: jest.fn(), restore: jest.fn(), beginPath: jest.fn(), arc: jest.fn(),
  fill: jest.fn(), stroke: jest.fn(), closePath: jest.fn(), moveTo: jest.fn(), lineTo: jest.fn()
});

describe('LoginForm', () => {
  // Test nr 1 (nasz stary, sprawdzający czy działa)
  test('renderuje formularz logowania bez błędów', () => {
    render(<LoginForm />);
    const buttonElement = screen.getByRole('button', { name: /zaloguj/i });
    expect(buttonElement).toBeInTheDocument();
  });

  // Test nr 2 (NOWY: symulacja klawiatury i kliknięcia)
  test('symuluje wpisanie zlych danych i klikniecie zaloguj', async () => {
    // Inicjujemy naszego wirtualnego użytkownika
    render(<LoginForm />);

    // 1. Szukamy pól tekstowych. Zazwyczaj najłatwiej znaleźć je po szarym tekście (Placeholder)
    // UWAGA: Jeśli w swoim komponencie masz inne placeholdery, np. "Wpisz adres email",
    // zmień odpowiednio te słowa między ukośnikami /.../i
    const emailInput = screen.getByPlaceholderText(/email/i);
    const passwordInput = screen.getByPlaceholderText(/hasło/i);
    const submitButton = screen.getByRole('button', { name: /zaloguj/i });

    // 2. Symulujemy prawdziwe wpisywanie tekstu klawisz po klawiszu
    await userEvent.type(emailInput, 'kompletnie@zly.email.pl');
    await userEvent.type(passwordInput, 'Bledne123!');

    // 3. Upewniamy się, że wirtualny React zareagował i wpisał te wartości do pól
    expect(emailInput).toHaveValue('kompletnie@zly.email.pl');
    expect(passwordInput).toHaveValue('Bledne123!');

    // 4. Klikamy "Zaloguj"
    await userEvent.click(submitButton);


  });
});