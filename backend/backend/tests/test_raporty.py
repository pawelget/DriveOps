import os
import pytest
from unittest.mock import patch

@pytest.fixture
def test_data(client):
    """
    Fixtura przygotowująca środowisko: tworzy użytkownika, dodaje mu samochód,
    a następnie tworzy wpis serwisowy, dla którego będziemy generować raport.
    """
    # 1. Rejestracja i logowanie
    client.post("/auth/register", json={
        "imie": "Anna", "nazwisko": "Raportowa",
        "email": "anna_raport@example.com", "haslo": "Tajnehaslo123!"
    })
    log_res = client.post("/auth/login", json={
        "email": "anna_raport@example.com", "haslo": "Tajnehaslo123!"
    })
    token = log_res.get_json()["token"]
    headers = {"Authorization": f"Bearer {token}"}

    # 2. Dodanie pojazdu
    car_res = client.post("/samochody", headers=headers, json={
        "marka": "Honda", "model": "Civic", "numer_rejestracyjny": "WA11111"
    })
    car_id = car_res.get_json()["id"]

    # 3. Dodanie wpisu serwisowego
    wpis_res = client.post("/wpisy-serwisowe", headers=headers, json={
        "samochod_id": car_id,
        "data_serwisu": "2026-05-20",
        "nazwa_warsztatu": "Super Mechanik",
        "opis": "Wymiana klocków",
        "zadania": [{"nazwa_zadania": "Wymiana klocków przód", "koszt_robocizny": 150}]
    })
    wpis_id = wpis_res.get_json()["id"]

    return {"headers": headers, "wpis_id": wpis_id}

def test_generowanie_raportu_pdf(client, test_data):
    """Test sprawdza, czy API generuje PDF i zapisuje go na dysku."""
    headers = test_data["headers"]
    wpis_id = test_data["wpis_id"]

    # Wywołanie endpointu do generowania PDF
    response = client.post(f"/raporty/generuj/{wpis_id}", headers=headers)

    assert response.status_code == 201
    data = response.get_json()
    assert "raport" in data
    assert data["raport"]["numer_raportu"].startswith("R/")

    # Sprawdzenie czy plik PDF faktycznie istnieje na dysku
    sciezka = data["raport"]["sciezka_do_pliku"]
    assert os.path.exists(sciezka)

    # Grzeczne sprzątanie po teście - usuwamy wygenerowany plik z dysku
    if os.path.exists(sciezka):
        os.remove(sciezka)

# @patch pozwala nam "podmienić" prawdziwą funkcję w app.py na naszą testową atrapę
@patch("app.send_report_email")
def test_wysylka_email_z_raportem(mock_send_email, client, test_data):
    """Test sprawdza, czy API poprawnie wzywa funkcję do wysyłki e-mail."""
    # Każe naszej atrapie zwrócić udawany sukces (omijamy SMTP)
    mock_send_email.return_value = {"success": True, "message": "Email wyslany"}

    headers = test_data["headers"]
    wpis_id = test_data["wpis_id"]

    # Najpierw musimy wygenerować raport, żeby mieć co "wysłać"
    gen_res = client.post(f"/raporty/generuj/{wpis_id}", headers=headers)
    raport_id = gen_res.get_json()["raport"]["id"]
    sciezka = gen_res.get_json()["raport"]["sciezka_do_pliku"]

    # Próbujemy wysłać email (uderzamy w endpoint z app.py)
    email_res = client.post(
        f"/raporty/{raport_id}/wyslij-email",
        headers=headers,
        json={"email": "test@test.com"}
    )

    assert email_res.status_code == 200
    # SZYK ZAAWANSOWANEGO TESTOWANIA: Sprawdzamy czy aplikacja w ogóle próbowała użyć funkcji wysyłającej!
    assert mock_send_email.called

    # Sprzątanie po PDF
    if os.path.exists(sciezka):
        os.remove(sciezka)