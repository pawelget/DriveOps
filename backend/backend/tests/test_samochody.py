import pytest

@pytest.fixture
def auth_headers(client):
    """Fixtura tworząca użytkownika i zwracająca nagłówek z tokenem."""
    reg_response = client.post("/auth/register", json={
        "imie": "Jan",
        "nazwisko": "Kierowca",
        "email": "kierowca@example.com",
        "haslo": "Tajnehaslo123!"
    })
    # Upewniamy się, że rejestracja się udała!
    assert reg_response.status_code == 201

    log_response = client.post("/auth/login", json={
        "email": "kierowca@example.com",
        "haslo": "Tajnehaslo123!"
    })
    token = log_response.get_json()["token"]

    return {"Authorization": f"Bearer {token}"}

def test_dodanie_samochodu(client, auth_headers):
    """Testuje poprawne dodanie nowego pojazdu przez zalogowanego użytkownika."""
    response = client.post(
        "/samochody",
        headers=auth_headers,
        json={
            "marka": "Toyota",
            "model": "Corolla",
            "numer_rejestracyjny": "WA12345",
            "rok_produkcji": 2020,
            "pojemnosc_cm3": 1798,
            "moc_km": 122,
            "paliwo": "hybryda",
            "przebieg": 45000,
            "kolor": "srebrny"
        }
    )

    assert response.status_code == 201
    data = response.get_json()
    assert data["marka"] == "Toyota"
    assert data["numer_rejestracyjny"] == "WA12345"
    assert "id" in data

def test_pobranie_listy_samochodow(client, auth_headers):
    """Testuje czy użytkownik widzi dodane przez siebie auta."""
    # Dodajemy jedno auto
    client.post(
        "/samochody",
        headers=auth_headers,
        json={
            "marka": "Ford",
            "model": "Focus",
            "numer_rejestracyjny": "KR54321"
        }
    )

    # Pobieramy listę
    response = client.get("/samochody", headers=auth_headers)

    assert response.status_code == 200
    data = response.get_json()
    assert type(data) == list
    assert len(data) == 1
    assert data[0]["marka"] == "Ford"

def test_dodanie_samochodu_brak_danych(client, auth_headers):
    """Testuje czy API odrzuca próbę dodania auta bez wymaganych pól (np. modelu)."""
    response = client.post(
        "/samochody",
        headers=auth_headers,
        json={
            "marka": "BMW",
            "numer_rejestracyjny": "PO99999"
            # Brakuje pola 'model'
        }
    )

    assert response.status_code == 400
    assert "Brak pol" in response.get_json()["error"]