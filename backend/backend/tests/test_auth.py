# tests/test_auth.py

def test_rejestracja_sukces(client):
    """Testuje poprawną rejestrację nowego użytkownika."""
    response = client.post("/auth/register", json={
        "imie": "Jan",
        "nazwisko": "Kowalski",
        "email": "jan@example.com",
        "haslo": "superhaslo123",
        "telefon": "123456789"
    })

    # DODAJEMY TĘ LINIJKĘ:
    print("\n--- ZAPIS BŁĘDU Z SERWERA ---")
    print(response.get_json())
    print("------------------------------\n")

    assert response.status_code == 201
    data = response.get_json()
    assert "token" in data
    assert data["user"]["email"] == "jan@example.com"
    assert data["message"] == "Rejestracja zakonczona sukcesem"

def test_rejestracja_brak_danych(client):
    """Testuje rejestrację bez wymaganych pól (np. hasła)."""
    response = client.post("/auth/register", json={
        "imie": "Jan",
        "email": "jan_bez_hasla@example.com"
    })

    assert response.status_code == 400
    assert "Wymagane pola" in response.get_json()["error"]

def test_logowanie_sukces(client):
    """Testuje poprawne logowanie wcześniej zarejestrowanego użytkownika."""
    # Najpierw tworzymy użytkownika w bazie testowej
    client.post("/auth/register", json={
        "imie": "Anna",
        "nazwisko": "Nowak",
        "email": "anna@example.com",
        "haslo": "testowehaslo"
    })

    # Próbujemy się zalogować
    response = client.post("/auth/login", json={
        "email": "anna@example.com",
        "haslo": "testowehaslo"
    })

    assert response.status_code == 200
    assert "token" in response.get_json()

def test_dostep_do_chronionego_endpointu(client):
    """Testuje czy /auth/me wpuszcza tylko z poprawnym tokenem."""
    # 1. Brak tokenu - powinno odrzucić
    response_no_token = client.get("/auth/me")
    assert response_no_token.status_code == 401

    # 2. Logujemy się, żeby zdobyć token
    client.post("/auth/register", json={
        "imie": "Maks",
        "nazwisko": "Testowy",
        "email": "maks@example.com",
        "haslo": "haslomaksa"
    })
    login_response = client.post("/auth/login", json={
        "email": "maks@example.com",
        "haslo": "haslomaksa"
    })
    token = login_response.get_json()["token"]

    # 3. Używamy tokenu, żeby dostać się do profilu
    response_with_token = client.get(
        "/auth/me",
        headers={"Authorization": f"Bearer {token}"}
    )

    assert response_with_token.status_code == 200
    assert response_with_token.get_json()["email"] == "maks@example.com"