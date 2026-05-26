import { apiFetch } from './Client';
import { getCars } from './CarsApi';

// 1.MOCK: Zlecenie dla Jesta, żeby podmienił funkcję apiFetch
jest.mock('./Client', () => ({
  apiFetch: jest.fn(),
}));

describe('CarsApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('getCars wysyła poprawne zapytanie GET pod adres /samochody', async () => {
    // 2. Przygotowujemy sztuczną odpowiedź
    const mockResponse = [{ id: 1, marka: 'Toyota', model: 'Yaris' }];

    // 3. Nasz oszukany apiFetch zwróci przygotowane dane
    apiFetch.mockResolvedValue(mockResponse);

    // 4. Uruchamiamy prawdziwą funkcję z Twojego pliku
    await getCars();

    // 5. Sprawdzamy, czy funkcja strzeliła pod endpoint z wlasciwa metodą
    expect(apiFetch).toHaveBeenCalledWith('/samochody', {
      method: 'GET',
    });
  });
});