from flask import Flask, jsonify, request
from flask_cors import CORS
from db import get_cursor
import bcrypt
import jwt
import os
import datetime
from functools import wraps
from dotenv import load_dotenv

load_dotenv();

app = Flask(__name__)
CORS(app, origins=["http://localhost:3000", "http://127.0.0.1:3000"])

JWT_SECRET = os.getenv("JWT_SECRET", "zapasowy_JWT") # zapasowy_JWT działa gdy nie ma .env
JWT_EXPIRES_HOURS = int(os.getenv("JWT_EXPIRES_HOURS", "12")) # rzutowanie na int bo .env zwraca stringi


def hash_password(password: str) -> str:
    hashed = bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt())
    return hashed.decode("utf-8")


def verify_password(password: str, password_hash: str) -> bool:
    return bcrypt.checkpw(
        password.encode("utf-8"),
        password_hash.encode("utf-8")
    )


def generate_token(user_id: int, email: str) -> str:
    payload = {
        "user_id": user_id,
        "email": email,
        "exp": datetime.datetime.utcnow() + datetime.timedelta(hours=JWT_EXPIRES_HOURS)
    }
    return jwt.encode(payload, JWT_SECRET, algorithm="HS256")


def token_required(route_func):
    @wraps(route_func)
    def wrapper(*args, **kwargs):
        auth_header = request.headers.get("Authorization", "")

        if not auth_header.startswith("Bearer "):
            return jsonify({"error": "Brak tokenu autoryzacji"}), 401

        token = auth_header.split(" ", 1)[1].strip()

        if not token:
            return jsonify({"error": "Brak tokenu autoryzacji"}), 401

        try:
            payload = jwt.decode(token, JWT_SECRET, algorithms=["HS256"])
            request.user = payload
        except jwt.ExpiredSignatureError:
            return jsonify({"error": "Token wygasl"}), 401
        except jwt.InvalidTokenError:
            return jsonify({"error": "Nieprawidlowy token"}), 401

        return route_func(*args, **kwargs)

    return wrapper


@app.route("/")
def home():
    return jsonify({
        "message": "DriveOps API dziala",
        "endpoints": [
            "GET /health",
            "POST /auth/register",
            "POST /auth/login",
            "GET /auth/me",
            "GET /uzytkownicy",
            "POST /uzytkownicy",
            "GET /samochody",
            "POST /samochody",
            "GET /samochody/<id>",
            "GET /wpisy-serwisowe",
            "POST /wpisy-serwisowe",
            "GET /przeglady",
            "POST /przeglady"
        ]
    })


@app.route("/health", methods=["GET"])
def health():
    try:
        with get_cursor() as cur:
            cur.execute("SELECT current_database() AS db, current_user AS user_name;")
            result = cur.fetchone()
        return jsonify({"status": "ok", "db_info": result}), 200
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500


# =========================
# AUTH
# =========================

@app.route("/auth/register", methods=["POST"])
def register():
    data = request.get_json(silent=True) or {}

    imie = (data.get("imie") or "").strip()
    nazwisko = (data.get("nazwisko") or "").strip()
    email = (data.get("email") or "").strip().lower()
    haslo = data.get("haslo") or ""
    telefon = (data.get("telefon") or "").strip() or None

    if not imie or not nazwisko or not email or not haslo:
        return jsonify({
            "error": "Wymagane pola: imie, nazwisko, email, haslo"
        }), 400

    if len(haslo) < 6:
        return jsonify({
            "error": "Haslo musi miec co najmniej 6 znakow"
        }), 400

    try:
        with get_cursor(commit=True) as cur:
            cur.execute("""
                SELECT id
                FROM uzytkownicy
                WHERE email = %s;
            """, (email,))
            existing_user = cur.fetchone()

            if existing_user:
                return jsonify({
                    "error": "Uzytkownik z tym adresem email juz istnieje"
                }), 409

            haslo_hash = hash_password(haslo)

            cur.execute("""
                INSERT INTO uzytkownicy (
                    imie,
                    nazwisko,
                    email,
                    haslo_hash,
                    telefon
                )
                VALUES (%s, %s, %s, %s, %s)
                RETURNING
                    id,
                    imie,
                    nazwisko,
                    email,
                    telefon,
                    czy_aktywny,
                    utworzono_w,
                    zaktualizowano_w;
            """, (imie, nazwisko, email, haslo_hash, telefon))

            created_user = cur.fetchone()

        token = generate_token(created_user["id"], created_user["email"])

        return jsonify({
            "message": "Rejestracja zakonczona sukcesem",
            "token": token,
            "user": created_user
        }), 201

    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/auth/login", methods=["POST"])
def login():
    data = request.get_json(silent=True) or {}

    email = (data.get("email") or "").strip().lower()
    haslo = data.get("haslo") or ""

    if not email or not haslo:
        return jsonify({
            "error": "Wymagane pola: email, haslo"
        }), 400

    try:
        with get_cursor() as cur:
            cur.execute("""
                SELECT
                    id,
                    imie,
                    nazwisko,
                    email,
                    haslo_hash,
                    telefon,
                    czy_aktywny,
                    utworzono_w,
                    zaktualizowano_w
                FROM uzytkownicy
                WHERE email = %s;
            """, (email,))
            user = cur.fetchone()

        if not user:
            return jsonify({
                "error": "Nieprawidlowy email lub haslo"
            }), 401

        if not user["czy_aktywny"]:
            return jsonify({
                "error": "Konto jest nieaktywne"
            }), 403

        if not verify_password(haslo, user["haslo_hash"]):
            return jsonify({
                "error": "Nieprawidlowy email lub haslo"
            }), 401

        token = generate_token(user["id"], user["email"])

        safe_user = {
            "id": user["id"],
            "imie": user["imie"],
            "nazwisko": user["nazwisko"],
            "email": user["email"],
            "telefon": user["telefon"],
            "czy_aktywny": user["czy_aktywny"],
            "utworzono_w": user["utworzono_w"],
            "zaktualizowano_w": user["zaktualizowano_w"]
        }

        return jsonify({
            "message": "Logowanie poprawne",
            "token": token,
            "user": safe_user
        }), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/auth/me", methods=["GET"])
@token_required
def me():
    user_payload = request.user
    user_id = user_payload["user_id"]

    try:
        with get_cursor() as cur:
            cur.execute("""
                SELECT
                    id,
                    imie,
                    nazwisko,
                    email,
                    telefon,
                    czy_aktywny,
                    utworzono_w,
                    zaktualizowano_w
                FROM uzytkownicy
                WHERE id = %s;
            """, (user_id,))
            user = cur.fetchone()

        if not user:
            return jsonify({"error": "Uzytkownik nie istnieje"}), 404

        return jsonify(user), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500


# =========================
# UZYTKOWNICY
# =========================

@app.route("/uzytkownicy", methods=["GET"])
def get_uzytkownicy():
    try:
        with get_cursor() as cur:
            cur.execute("""
                SELECT id, imie, nazwisko, email, telefon, czy_aktywny,
                       utworzono_w, zaktualizowano_w
                FROM uzytkownicy
                ORDER BY id;
            """)
            users = cur.fetchall()
        return jsonify(users), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/uzytkownicy", methods=["POST"])
def create_uzytkownik():
    data = request.get_json(silent=True) or {}

    imie = data.get("imie")
    nazwisko = data.get("nazwisko")
    email = data.get("email")
    haslo_hash = data.get("haslo_hash")
    telefon = data.get("telefon")

    if not all([imie, nazwisko, email, haslo_hash]):
        return jsonify({
            "error": "Wymagane pola: imie, nazwisko, email, haslo_hash"
        }), 400

    try:
        with get_cursor(commit=True) as cur:
            cur.execute("""
                INSERT INTO uzytkownicy (imie, nazwisko, email, haslo_hash, telefon)
                VALUES (%s, %s, %s, %s, %s)
                RETURNING id, imie, nazwisko, email, telefon, czy_aktywny,
                          utworzono_w, zaktualizowano_w;
            """, (imie, nazwisko, email, haslo_hash, telefon))
            created = cur.fetchone()
        return jsonify(created), 201
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# =========================
# SAMOCHODY
# =========================

@app.route("/samochody", methods=["GET"])
def get_samochody():
    try:
        with get_cursor() as cur:
            cur.execute("""
                SELECT
                    s.id,
                    s.uzytkownik_id,
                    u.imie,
                    u.nazwisko,
                    s.vin,
                    s.numer_rejestracyjny,
                    s.marka,
                    s.model,
                    s.rok_produkcji,
                    s.pojemnosc_cm3,
                    s.moc_km,
                    s.paliwo,
                    s.przebieg,
                    s.kolor,
                    s.utworzono_w,
                    s.zaktualizowano_w
                FROM samochody s
                JOIN uzytkownicy u ON u.id = s.uzytkownik_id
                ORDER BY s.id;
            """)
            cars = cur.fetchall()
        return jsonify(cars), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/samochody/<int:samochod_id>", methods=["GET"])
def get_samochod(samochod_id):
    try:
        with get_cursor() as cur:
            cur.execute("""
                SELECT *
                FROM samochody
                WHERE id = %s;
            """, (samochod_id,))
            car = cur.fetchone()

        if not car:
            return jsonify({"error": "Samochod nie istnieje"}), 404

        return jsonify(car), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/samochody", methods=["POST"])
def create_samochod():
    data = request.get_json(silent=True) or {}

    required_fields = ["uzytkownik_id", "numer_rejestracyjny", "marka", "model"]
    missing = [field for field in required_fields if data.get(field) is None]

    if missing:
        return jsonify({"error": f"Brak pol: {', '.join(missing)}"}), 400

    try:
        with get_cursor(commit=True) as cur:
            cur.execute("""
                INSERT INTO samochody (
                    uzytkownik_id, vin, numer_rejestracyjny, marka, model,
                    rok_produkcji, pojemnosc_cm3, moc_km, paliwo, przebieg, kolor
                )
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                RETURNING *;
            """, (
                data.get("uzytkownik_id"),
                data.get("vin"),
                data.get("numer_rejestracyjny"),
                data.get("marka"),
                data.get("model"),
                data.get("rok_produkcji"),
                data.get("pojemnosc_cm3"),
                data.get("moc_km"),
                data.get("paliwo"),
                data.get("przebieg", 0),
                data.get("kolor")
            ))
            created = cur.fetchone()
        return jsonify(created), 201
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# =========================
# WPISY SERWISOWE
# =========================

@app.route("/wpisy-serwisowe", methods=["GET"])
def get_wpisy_serwisowe():
    try:
        with get_cursor() as cur:
            cur.execute("""
                SELECT
                    ws.id,
                    ws.samochod_id,
                    s.marka,
                    s.model,
                    ws.rodzaj_serwisu_id,
                    rs.nazwa AS rodzaj_serwisu,
                    ws.data_serwisu,
                    ws.nazwa_warsztatu,
                    ws.przebieg_przy_serwisie,
                    ws.nastepny_serwis_przebieg,
                    ws.nastepna_data_serwisu,
                    ws.opis,
                    ws.status,
                    ws.utworzono_w,
                    ws.zaktualizowano_w
                FROM wpisy_serwisowe ws
                JOIN samochody s ON s.id = ws.samochod_id
                LEFT JOIN rodzaje_serwisu rs ON rs.id = ws.rodzaj_serwisu_id
                ORDER BY ws.data_serwisu DESC, ws.id DESC;
            """)
            rows = cur.fetchall()
        return jsonify(rows), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/wpisy-serwisowe", methods=["POST"])
def create_wpis_serwisowy():
    data = request.get_json(silent=True) or {}

    if data.get("samochod_id") is None or data.get("data_serwisu") is None:
        return jsonify({
            "error": "Wymagane pola: samochod_id, data_serwisu"
        }), 400

    try:
        with get_cursor(commit=True) as cur:
            cur.execute("""
                INSERT INTO wpisy_serwisowe (
                    samochod_id,
                    rodzaj_serwisu_id,
                    data_serwisu,
                    nazwa_warsztatu,
                    adres_warsztatu,
                    przebieg_przy_serwisie,
                    nastepny_serwis_przebieg,
                    nastepna_data_serwisu,
                    opis,
                    status
                )
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                RETURNING *;
            """, (
                data.get("samochod_id"),
                data.get("rodzaj_serwisu_id"),
                data.get("data_serwisu"),
                data.get("nazwa_warsztatu"),
                data.get("adres_warsztatu"),
                data.get("przebieg_przy_serwisie"),
                data.get("nastepny_serwis_przebieg"),
                data.get("nastepna_data_serwisu"),
                data.get("opis"),
                data.get("status", "w_toku")
            ))
            created = cur.fetchone()
        return jsonify(created), 201
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# =========================
# PRZEGLADY TECHNICZNE
# =========================

@app.route("/przeglady", methods=["GET"])
def get_przeglady():
    try:
        with get_cursor() as cur:
            cur.execute("""
                SELECT
                    pt.id,
                    pt.samochod_id,
                    s.marka,
                    s.model,
                    s.numer_rejestracyjny,
                    pt.data_wykonania,
                    pt.data_waznosci,
                    pt.wynik,
                    pt.stacja_kontroli,
                    pt.przebieg,
                    pt.uwagi,
                    pt.utworzono_w,
                    pt.zaktualizowano_w
                FROM przeglady_techniczne pt
                JOIN samochody s ON s.id = pt.samochod_id
                ORDER BY pt.data_waznosci ASC;
            """)
            rows = cur.fetchall()
        return jsonify(rows), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/przeglady", methods=["POST"])
def create_przeglad():
    data = request.get_json(silent=True) or {}

    required_fields = ["samochod_id", "data_wykonania", "data_waznosci"]
    missing = [field for field in required_fields if data.get(field) is None]

    if missing:
        return jsonify({"error": f"Brak pol: {', '.join(missing)}"}), 400

    try:
        with get_cursor(commit=True) as cur:
            cur.execute("""
                INSERT INTO przeglady_techniczne (
                    samochod_id, data_wykonania, data_waznosci, wynik,
                    stacja_kontroli, przebieg, uwagi
                )
                VALUES (%s, %s, %s, %s, %s, %s, %s)
                RETURNING *;
            """, (
                data.get("samochod_id"),
                data.get("data_wykonania"),
                data.get("data_waznosci"),
                data.get("wynik", "pozytywny"),
                data.get("stacja_kontroli"),
                data.get("przebieg"),
                data.get("uwagi")
            ))
            created = cur.fetchone()
        return jsonify(created), 201
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# =========================
# WIDOK Z KOSZTAMI
# =========================

@app.route("/koszty-serwisow", methods=["GET"])
def get_koszty_serwisow():
    try:
        with get_cursor() as cur:
            cur.execute("""
                SELECT *
                FROM v_wpisy_serwisowe_z_kosztem
                ORDER BY data_serwisu DESC, id DESC;
            """)
            rows = cur.fetchall()
        return jsonify(rows), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500


if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000)