from flask import Flask, jsonify, request
from flask_cors import CORS

import bcrypt
import jwt
import os
import datetime

from functools import wraps
from dotenv import load_dotenv

from sqlalchemy import text

from db import SessionLocal

from models import (
    Uzytkownik,
    Samochod,
    RodzajSerwisu,
    WpisSerwisowy,
    ZadanieSerwisowe,
    Czesc,
    UzytaCzesc,
    PrzegladTechniczny,
    Raport,
    PowiadomienieEmail,
    WpisSerwisowyZKosztem
)

load_dotenv()

app = Flask(__name__)

CORS(app, origins=[
    "http://localhost:3000",
    "http://127.0.0.1:3000"
])

JWT_SECRET = os.getenv("JWT_SECRET", "zapasowy_JWT")

JWT_EXPIRES_HOURS = int(
    os.getenv("JWT_EXPIRES_HOURS", "12")
)


# =========================
# HELPERS
# =========================

def hash_password(password: str) -> str:
    hashed = bcrypt.hashpw(
        password.encode("utf-8"),
        bcrypt.gensalt()
    )

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
        "exp": datetime.datetime.utcnow()
        + datetime.timedelta(hours=JWT_EXPIRES_HOURS)
    }

    return jwt.encode(
        payload,
        JWT_SECRET,
        algorithm="HS256"
    )


def token_required(route_func):
    @wraps(route_func)
    def wrapper(*args, **kwargs):

        auth_header = request.headers.get(
            "Authorization",
            ""
        )

        if not auth_header.startswith("Bearer "):
            return jsonify({
                "error": "Brak tokenu autoryzacji"
            }), 401

        token = auth_header.split(" ", 1)[1].strip()

        try:
            payload = jwt.decode(
                token,
                JWT_SECRET,
                algorithms=["HS256"]
            )

            request.user = payload

        except jwt.ExpiredSignatureError:
            return jsonify({
                "error": "Token wygasl"
            }), 401

        except jwt.InvalidTokenError:
            return jsonify({
                "error": "Nieprawidlowy token"
            }), 401

        return route_func(*args, **kwargs)

    return wrapper


# =========================
# SERIALIZERS
# =========================

def user_to_dict(user: Uzytkownik):
    return {
        "id": user.id,
        "imie": user.imie,
        "nazwisko": user.nazwisko,
        "email": user.email,
        "telefon": user.telefon,
        "czy_aktywny": user.czy_aktywny,
        "utworzono_w": user.utworzono_w,
        "zaktualizowano_w": user.zaktualizowano_w
    }


def car_to_dict(car: Samochod):
    return {
        "id": car.id,
        "uzytkownik_id": car.uzytkownik_id,
        "vin": car.vin,
        "numer_rejestracyjny": car.numer_rejestracyjny,
        "marka": car.marka,
        "model": car.model,
        "rok_produkcji": car.rok_produkcji,
        "pojemnosc_cm3": car.pojemnosc_cm3,
        "moc_km": car.moc_km,
        "paliwo": car.paliwo,
        "przebieg": car.przebieg,
        "kolor": car.kolor,
        "utworzono_w": car.utworzono_w,
        "zaktualizowano_w": car.zaktualizowano_w
    }


def wpis_to_dict(wpis: WpisSerwisowy):
    return {
        "id": wpis.id,
        "samochod_id": wpis.samochod_id,
        "rodzaj_serwisu_id": wpis.rodzaj_serwisu_id,
        "data_serwisu": wpis.data_serwisu,
        "nazwa_warsztatu": wpis.nazwa_warsztatu,
        "adres_warsztatu": wpis.adres_warsztatu,
        "przebieg_przy_serwisie": wpis.przebieg_przy_serwisie,
        "nastepny_serwis_przebieg": wpis.nastepny_serwis_przebieg,
        "nastepna_data_serwisu": wpis.nastepna_data_serwisu,
        "opis": wpis.opis,
        "status": wpis.status,
        "utworzono_w": wpis.utworzono_w,
        "zaktualizowano_w": wpis.zaktualizowano_w
    }


def przeglad_to_dict(przeglad: PrzegladTechniczny):
    return {
        "id": przeglad.id,
        "samochod_id": przeglad.samochod_id,
        "data_wykonania": przeglad.data_wykonania,
        "data_waznosci": przeglad.data_waznosci,
        "wynik": przeglad.wynik,
        "stacja_kontroli": przeglad.stacja_kontroli,
        "przebieg": przeglad.przebieg,
        "uwagi": przeglad.uwagi,
        "utworzono_w": przeglad.utworzono_w,
        "zaktualizowano_w": przeglad.zaktualizowano_w
    }


# =========================
# BASIC
# =========================

@app.route("/")
def home():
    return jsonify({
        "message": "DriveOps API dziala"
    })


@app.route("/health", methods=["GET"])
def health():

    db = SessionLocal()

    try:
        result = db.execute(
            text("""
                SELECT current_database() AS db,
                       current_user AS user_name;
            """)
        )

        row = result.mappings().first()

        return jsonify({
            "status": "ok",
            "db_info": dict(row)
        }), 200

    except Exception as e:
        return jsonify({
            "status": "error",
            "message": str(e)
        }), 500

    finally:
        db.close()


# =========================
# AUTH REGISTER
# =========================

@app.route("/auth/register", methods=["POST"])
def register():

    db = SessionLocal()

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

        existing_user = (
            db.query(Uzytkownik)
            .filter(Uzytkownik.email == email)
            .first()
        )

        if existing_user:
            return jsonify({
                "error": "Uzytkownik z tym adresem email juz istnieje"
            }), 409

        created_user = Uzytkownik(
            imie=imie,
            nazwisko=nazwisko,
            email=email,
            haslo_hash=hash_password(haslo),
            telefon=telefon
        )

        db.add(created_user)

        db.commit()

        db.refresh(created_user)

        token = generate_token(
            created_user.id,
            created_user.email
        )

        return jsonify({
            "message": "Rejestracja zakonczona sukcesem",
            "token": token,
            "user": user_to_dict(created_user)
        }), 201

    except Exception as e:

        db.rollback()

        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


# =========================
# AUTH LOGIN
# =========================

@app.route("/auth/login", methods=["POST"])
def login():

    db = SessionLocal()

    data = request.get_json(silent=True) or {}

    email = (data.get("email") or "").strip().lower()

    haslo = data.get("haslo") or ""

    if not email or not haslo:
        return jsonify({
            "error": "Wymagane pola: email, haslo"
        }), 400

    try:

        user = (
            db.query(Uzytkownik)
            .filter(Uzytkownik.email == email)
            .first()
        )

        if not user:
            return jsonify({
                "error": "Nieprawidlowy email lub haslo"
            }), 401

        if not user.czy_aktywny:
            return jsonify({
                "error": "Konto jest nieaktywne"
            }), 403

        if not verify_password(
            haslo,
            user.haslo_hash
        ):
            return jsonify({
                "error": "Nieprawidlowy email lub haslo"
            }), 401

        token = generate_token(
            user.id,
            user.email
        )

        return jsonify({
            "message": "Logowanie poprawne",
            "token": token,
            "user": user_to_dict(user)
        }), 200

    except Exception as e:
        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


# =========================
# AUTH ME
# =========================

@app.route("/auth/me", methods=["GET"])
@token_required
def me():

    db = SessionLocal()

    user_id = request.user["user_id"]

    try:

        user = (
            db.query(Uzytkownik)
            .filter(Uzytkownik.id == user_id)
            .first()
        )

        if not user:
            return jsonify({
                "error": "Uzytkownik nie istnieje"
            }), 404

        return jsonify(
            user_to_dict(user)
        ), 200

    except Exception as e:
        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()

# =========================
# AUTH UPDATE ME
# =========================

@app.route("/auth/me", methods=["PATCH"])
@token_required
def update_me():

    db = SessionLocal()

    user_id = request.user["user_id"]

    data = request.get_json(silent=True) or {}

    try:

        user = (
            db.query(Uzytkownik)
            .filter(Uzytkownik.id == user_id)
            .first()
        )

        if not user:
            return jsonify({
                "error": "Uzytkownik nie istnieje"
            }), 404

        if "imie" in data:
            imie = (data.get("imie") or "").strip()

            if not imie:
                return jsonify({
                    "error": "Imie nie moze byc puste"
                }), 400

            user.imie = imie

        if "nazwisko" in data:
            nazwisko = (data.get("nazwisko") or "").strip()

            if not nazwisko:
                return jsonify({
                    "error": "Nazwisko nie moze byc puste"
                }), 400

            user.nazwisko = nazwisko

        if "email" in data:
            email = (data.get("email") or "").strip().lower()

            if not email:
                return jsonify({
                    "error": "Email nie moze byc pusty"
                }), 400

            if email != user.email:

                existing = (
                    db.query(Uzytkownik)
                    .filter(Uzytkownik.email == email)
                    .first()
                )

                if existing:
                    return jsonify({
                        "error": "Email jest juz zajety"
                    }), 409

                user.email = email

        if "telefon" in data:
            telefon = (data.get("telefon") or "").strip() or None

            user.telefon = telefon

        user.zaktualizowano_w = datetime.datetime.utcnow()

        db.commit()

        db.refresh(user)

        return jsonify({
            "message": "Dane zaktualizowane",
            "user": user_to_dict(user)
        }), 200

    except Exception as e:

        db.rollback()

        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


# =========================
# AUTH CHANGE PASSWORD
# =========================

@app.route("/auth/change-password", methods=["POST"])
@token_required
def change_password():

    db = SessionLocal()

    user_id = request.user["user_id"]

    data = request.get_json(silent=True) or {}

    obecne_haslo = data.get("obecne_haslo") or ""

    nowe_haslo = data.get("nowe_haslo") or ""

    if not obecne_haslo or not nowe_haslo:
        return jsonify({
            "error": "Wymagane pola: obecne_haslo, nowe_haslo"
        }), 400

    if len(nowe_haslo) < 6:
        return jsonify({
            "error": "Nowe haslo musi miec co najmniej 6 znakow"
        }), 400

    if obecne_haslo == nowe_haslo:
        return jsonify({
            "error": "Nowe haslo musi byc rozne od obecnego"
        }), 400

    try:

        user = (
            db.query(Uzytkownik)
            .filter(Uzytkownik.id == user_id)
            .first()
        )

        if not user:
            return jsonify({
                "error": "Uzytkownik nie istnieje"
            }), 404

        if not verify_password(obecne_haslo, user.haslo_hash):
            return jsonify({
                "error": "Obecne haslo jest nieprawidlowe"
            }), 401

        user.haslo_hash = hash_password(nowe_haslo)

        user.zaktualizowano_w = datetime.datetime.utcnow()

        db.commit()

        return jsonify({
            "message": "Haslo zostalo zmienione"
        }), 200

    except Exception as e:

        db.rollback()

        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


# =========================
# AUTH DEACTIVATE
# =========================

@app.route("/auth/deactivate", methods=["POST"])
@token_required
def deactivate_account():

    db = SessionLocal()

    user_id = request.user["user_id"]

    data = request.get_json(silent=True) or {}

    haslo = data.get("haslo") or ""

    if not haslo:
        return jsonify({
            "error": "Wymagane pole: haslo"
        }), 400

    try:

        user = (
            db.query(Uzytkownik)
            .filter(Uzytkownik.id == user_id)
            .first()
        )

        if not user:
            return jsonify({
                "error": "Uzytkownik nie istnieje"
            }), 404

        if not verify_password(haslo, user.haslo_hash):
            return jsonify({
                "error": "Nieprawidlowe haslo"
            }), 401

        user.czy_aktywny = False

        user.zaktualizowano_w = datetime.datetime.utcnow()

        db.commit()

        return jsonify({
            "message": "Konto zostalo dezaktywowane"
        }), 200

    except Exception as e:

        db.rollback()

        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


# =========================
# UZYTKOWNICY
# =========================

@app.route("/uzytkownicy", methods=["GET"])
def get_uzytkownicy():

    db = SessionLocal()

    try:

        users = (
            db.query(Uzytkownik)
            .order_by(Uzytkownik.id)
            .all()
        )

        return jsonify([
            user_to_dict(user)
            for user in users
        ]), 200

    except Exception as e:
        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


# =========================
# SAMOCHODY
# =========================

@app.route("/samochody", methods=["GET"])
def get_samochody():

    db = SessionLocal()

    try:

        cars = (
            db.query(Samochod)
            .order_by(Samochod.id)
            .all()
        )

        return jsonify([
            {
                **car_to_dict(car),
                "imie": car.uzytkownik.imie,
                "nazwisko": car.uzytkownik.nazwisko
            }
            for car in cars
        ]), 200

    except Exception as e:
        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


@app.route("/samochody", methods=["POST"])
def create_samochod():

    db = SessionLocal()

    data = request.get_json(silent=True) or {}

    required_fields = [
        "uzytkownik_id",
        "numer_rejestracyjny",
        "marka",
        "model"
    ]

    missing = [
        field
        for field in required_fields
        if data.get(field) is None
    ]

    if missing:
        return jsonify({
            "error": f"Brak pol: {', '.join(missing)}"
        }), 400

    try:

        created = Samochod(
            uzytkownik_id=data.get("uzytkownik_id"),
            vin=data.get("vin"),
            numer_rejestracyjny=data.get("numer_rejestracyjny"),
            marka=data.get("marka"),
            model=data.get("model"),
            rok_produkcji=data.get("rok_produkcji"),
            pojemnosc_cm3=data.get("pojemnosc_cm3"),
            moc_km=data.get("moc_km"),
            paliwo=data.get("paliwo"),
            przebieg=data.get("przebieg", 0),
            kolor=data.get("kolor")
        )

        db.add(created)

        db.commit()

        db.refresh(created)

        return jsonify(
            car_to_dict(created)
        ), 201

    except Exception as e:

        db.rollback()

        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


# =========================
# WPISY SERWISOWE
# =========================

@app.route("/wpisy-serwisowe", methods=["GET"])
def get_wpisy_serwisowe():

    db = SessionLocal()

    try:

        rows = (
            db.query(WpisSerwisowy)
            .order_by(
                WpisSerwisowy.data_serwisu.desc(),
                WpisSerwisowy.id.desc()
            )
            .all()
        )

        return jsonify([
            wpis_to_dict(row)
            for row in rows
        ]), 200

    except Exception as e:
        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


@app.route("/wpisy-serwisowe", methods=["POST"])
def create_wpis_serwisowy():

    db = SessionLocal()

    data = request.get_json(silent=True) or {}

    if (
        data.get("samochod_id") is None
        or data.get("data_serwisu") is None
    ):
        return jsonify({
            "error": "Wymagane pola: samochod_id, data_serwisu"
        }), 400

    try:

        created = WpisSerwisowy(
            samochod_id=data.get("samochod_id"),
            rodzaj_serwisu_id=data.get("rodzaj_serwisu_id"),
            data_serwisu=data.get("data_serwisu"),
            nazwa_warsztatu=data.get("nazwa_warsztatu"),
            adres_warsztatu=data.get("adres_warsztatu"),
            przebieg_przy_serwisie=data.get("przebieg_przy_serwisie"),
            nastepny_serwis_przebieg=data.get("nastepny_serwis_przebieg"),
            nastepna_data_serwisu=data.get("nastepna_data_serwisu"),
            opis=data.get("opis"),
            status=data.get("status", "w_toku")
        )

        db.add(created)

        db.commit()

        db.refresh(created)

        return jsonify(
            wpis_to_dict(created)
        ), 201

    except Exception as e:

        db.rollback()

        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


# =========================
# PRZEGLADY
# =========================

@app.route("/przeglady", methods=["GET"])
def get_przeglady():

    db = SessionLocal()

    try:

        rows = (
            db.query(PrzegladTechniczny)
            .order_by(
                PrzegladTechniczny.data_waznosci.asc()
            )
            .all()
        )

        return jsonify([
            przeglad_to_dict(row)
            for row in rows
        ]), 200

    except Exception as e:
        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


@app.route("/przeglady", methods=["POST"])
def create_przeglad():

    db = SessionLocal()

    data = request.get_json(silent=True) or {}

    required_fields = [
        "samochod_id",
        "data_wykonania",
        "data_waznosci"
    ]

    missing = [
        field
        for field in required_fields
        if data.get(field) is None
    ]

    if missing:
        return jsonify({
            "error": f"Brak pol: {', '.join(missing)}"
        }), 400

    try:

        created = PrzegladTechniczny(
            samochod_id=data.get("samochod_id"),
            data_wykonania=data.get("data_wykonania"),
            data_waznosci=data.get("data_waznosci"),
            wynik=data.get("wynik", "pozytywny"),
            stacja_kontroli=data.get("stacja_kontroli"),
            przebieg=data.get("przebieg"),
            uwagi=data.get("uwagi")
        )

        db.add(created)

        db.commit()

        db.refresh(created)

        return jsonify(
            przeglad_to_dict(created)
        ), 201

    except Exception as e:

        db.rollback()

        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


# =========================
# KOSZTY SERWISOW
# =========================

@app.route("/koszty-serwisow", methods=["GET"])
def get_koszty_serwisow():

    db = SessionLocal()

    try:

        rows = (
            db.query(WpisSerwisowyZKosztem)
            .order_by(
                WpisSerwisowyZKosztem.data_serwisu.desc(),
                WpisSerwisowyZKosztem.id.desc()
            )
            .all()
        )

        return jsonify([
            {
                "id": row.id,
                "samochod_id": row.samochod_id,
                "data_serwisu": row.data_serwisu,
                "nazwa_warsztatu": row.nazwa_warsztatu,
                "status": row.status,
                "calkowity_koszt": float(row.calkowity_koszt)
                if row.calkowity_koszt is not None
                else 0
            }
            for row in rows
        ]), 200

    except Exception as e:
        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


if __name__ == "__main__":
    app.run(
        host="0.0.0.0",
        port=5000,
        debug=True
    )