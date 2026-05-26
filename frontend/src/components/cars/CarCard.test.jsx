import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import CarCard from './CarCard';

// Jeśli w karcie używasz jakichś zewnętrznych ikon (np. z 'lucide-react' czy 'react-icons'),
// warto je zmockować, żeby JSDOM nie miał problemu z ich renderowaniem.
// (Jeśli ich nie masz, ten fragment po prostu zostanie zignorowany).
jest.mock('lucide-react', () => ({
  Edit: () => <span>Edit Icon</span>,
  Trash2: () => <span>Delete Icon</span>,
  Wrench: () => <span>Service Icon</span>
}), { virtual: true });

describe('CarCard', () => {
  // 1. Tworzymy sztuczny, zmockowany obiekt samochodu (Mock Data)
  const mockCar = {
    id: 1,
    marka: 'Toyota',
    model: 'Corolla',
    numer_rejestracyjny: 'WA 12345',
    rok_produkcji: 2020,
    przebieg: 45000
  };

  test('poprawnie renderuje dane samochodu z przekazanych propsów', () => {
    // 2. Renderujemy kartę, przekazując jej nasz sztuczny obiekt oraz puste funkcje dla przycisków
    render(
      <CarCard
        car={mockCar}
        onEdit={() => {}}
        onDelete={() => {}}
      />
    );

    // 3. Sprawdzamy, czy aplikacja poprawnie wyciągnęła dane i wyświetliła je na ekranie.
    // Używamy wyrażeń regularnych (/Toyota/i), żeby zignorować wielkość liter i dokładne dopasowanie
    expect(screen.getByText(/Toyota/i)).toBeInTheDocument();
    expect(screen.getByText(/Corolla/i)).toBeInTheDocument();
    expect(screen.getByText(/WA 12345/i)).toBeInTheDocument();
  });
});