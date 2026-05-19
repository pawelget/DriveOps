from flask import Flask, jsonify, request
from flask_cors import CORS
from pdf_utils import generate_service_report_pdf, generate_report_number, REPORTS_DIR
from email_utils import send_report_email

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
# ALERTY
# =========================

@app.route("/alerts", methods=["GET"])
@token_required
def get_alerts():

    db = SessionLocal()

    typ_filter = request.args.get("typ")

    priorytet_filter = request.args.get("priorytet")

    try:

        today = datetime.date.today()

        soon_30 = today + datetime.timedelta(days=30)

        soon_14 = today + datetime.timedelta(days=14)

        alerts = []

        # 1) Przeglady wygasle
        wygasle = (
            db.query(PrzegladTechniczny)
            .filter(PrzegladTechniczny.data_waznosci < today)
            .all()
        )

        for p in wygasle:

            car = p.samochod

            alerts.append({
                "id": f"przeglad-wygasly-{p.id}",
                "typ": "przeglad",
                "priorytet": "critical",
                "tytul": "Przeglad techniczny wygasl",
                "opis": (
                    f"Auto {car.marka} {car.model} "
                    f"({car.numer_rejestracyjny}) ma wygasly przeglad "
                    f"od {p.data_waznosci.isoformat()}"
                ),
                "data": p.data_waznosci.isoformat(),
                "samochod_id": car.id,
                "samochod_info": (
                    f"{car.marka} {car.model} "
                    f"({car.numer_rejestracyjny})"
                )
            })

        # 2) Przeglady wygasajace w ciagu 30 dni
        wygasajace = (
            db.query(PrzegladTechniczny)
            .filter(PrzegladTechniczny.data_waznosci >= today)
            .filter(PrzegladTechniczny.data_waznosci <= soon_30)
            .all()
        )

        for p in wygasajace:

            car = p.samochod

            dni = (p.data_waznosci - today).days

            priorytet = "critical" if dni <= 7 else "warning"

            alerts.append({
                "id": f"przeglad-konczacy-{p.id}",
                "typ": "przeglad",
                "priorytet": priorytet,
                "tytul": f"Przeglad konczy sie za {dni} dni",
                "opis": (
                    f"Auto {car.marka} {car.model} "
                    f"({car.numer_rejestracyjny}) - przeglad wazny do "
                    f"{p.data_waznosci.isoformat()}"
                ),
                "data": p.data_waznosci.isoformat(),
                "samochod_id": car.id,
                "samochod_info": (
                    f"{car.marka} {car.model} "
                    f"({car.numer_rejestracyjny})"
                )
            })

        # 3) Przeglady z wynikiem negatywnym
        negatywne = (
            db.query(PrzegladTechniczny)
            .filter(PrzegladTechniczny.wynik == "negatywny")
            .all()
        )

        for p in negatywne:

            car = p.samochod

            alerts.append({
                "id": f"przeglad-negatywny-{p.id}",
                "typ": "przeglad",
                "priorytet": "critical",
                "tytul": "Negatywny wynik przegladu",
                "opis": (
                    f"Auto {car.marka} {car.model} "
                    f"({car.numer_rejestracyjny}) nie przeszlo przegladu "
                    f"({p.data_wykonania.isoformat()})"
                ),
                "data": p.data_wykonania.isoformat(),
                "samochod_id": car.id,
                "samochod_info": (
                    f"{car.marka} {car.model} "
                    f"({car.numer_rejestracyjny})"
                )
            })

        # 4) Nadchodzace serwisy (w ciagu 14 dni)
        nadchodzace_serwisy = (
            db.query(WpisSerwisowy)
            .filter(WpisSerwisowy.nastepna_data_serwisu != None)
            .filter(WpisSerwisowy.nastepna_data_serwisu >= today)
            .filter(WpisSerwisowy.nastepna_data_serwisu <= soon_14)
            .all()
        )

        for w in nadchodzace_serwisy:

            car = w.samochod

            dni = (w.nastepna_data_serwisu - today).days

            priorytet = "warning" if dni <= 7 else "info"

            alerts.append({
                "id": f"serwis-nadchodzacy-{w.id}",
                "typ": "serwis",
                "priorytet": priorytet,
                "tytul": f"Serwis za {dni} dni",
                "opis": (
                    f"Auto {car.marka} {car.model} "
                    f"({car.numer_rejestracyjny}) - planowany serwis "
                    f"{w.nastepna_data_serwisu.isoformat()}"
                ),
                "data": w.nastepna_data_serwisu.isoformat(),
                "samochod_id": car.id,
                "samochod_info": (
                    f"{car.marka} {car.model} "
                    f"({car.numer_rejestracyjny})"
                )
            })

        # 5) Zalegle serwisy (w toku z data juz minieta)
        zalegle_serwisy = (
            db.query(WpisSerwisowy)
            .filter(WpisSerwisowy.status == "w_toku")
            .filter(WpisSerwisowy.data_serwisu < today)
            .all()
        )

        for w in zalegle_serwisy:

            car = w.samochod

            dni = (today - w.data_serwisu).days

            alerts.append({
                "id": f"serwis-zalegly-{w.id}",
                "typ": "serwis",
                "priorytet": "critical",
                "tytul": f"Serwis w toku od {dni} dni",
                "opis": (
                    f"Auto {car.marka} {car.model} "
                    f"({car.numer_rejestracyjny}) - serwis rozpoczety "
                    f"{w.data_serwisu.isoformat()} nadal w toku"
                ),
                "data": w.data_serwisu.isoformat(),
                "samochod_id": car.id,
                "samochod_info": (
                    f"{car.marka} {car.model} "
                    f"({car.numer_rejestracyjny})"
                )
            })

        # Filtry
        if typ_filter:
            alerts = [
                a for a in alerts
                if a["typ"] == typ_filter
            ]

        if priorytet_filter:
            alerts = [
                a for a in alerts
                if a["priorytet"] == priorytet_filter
            ]

        # Sortowanie: critical > warning > info, potem po dacie
        priority_order = {
            "critical": 0,
            "warning": 1,
            "info": 2
        }

        alerts.sort(
            key=lambda a: (
                priority_order.get(a["priorytet"], 99),
                a["data"]
            )
        )

        return jsonify({
            "alerts": alerts,
            "total": len(alerts),
            "by_priority": {
                "critical": sum(
                    1 for a in alerts
                    if a["priorytet"] == "critical"
                ),
                "warning": sum(
                    1 for a in alerts
                    if a["priorytet"] == "warning"
                ),
                "info": sum(
                    1 for a in alerts
                    if a["priorytet"] == "info"
                )
            }
        }), 200

    except Exception as e:
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
@token_required
def get_samochody():

    db = SessionLocal()

    user_id = request.user["user_id"]

    try:

        cars = (
            db.query(Samochod)
            .filter(Samochod.uzytkownik_id == user_id)
            .order_by(Samochod.id)
            .all()
        )

        return jsonify([
            car_to_dict(car)
            for car in cars
        ]), 200

    except Exception as e:
        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


@app.route("/samochody", methods=["POST"])
@token_required
def create_samochod():

    db = SessionLocal()

    user_id = request.user["user_id"]

    data = request.get_json(silent=True) or {}

    required_fields = [
        "numer_rejestracyjny",
        "marka",
        "model"
    ]

    missing = [
        field
        for field in required_fields
        if not data.get(field)
    ]

    if missing:
        return jsonify({
            "error": f"Brak pol: {', '.join(missing)}"
        }), 400

    try:

        # Sprawdz czy numer rejestracyjny nie jest juz zajety
        existing = (
            db.query(Samochod)
            .filter(
                Samochod.numer_rejestracyjny ==
                data.get("numer_rejestracyjny")
            )
            .first()
        )

        if existing:
            return jsonify({
                "error": "Pojazd z tym numerem rejestracyjnym juz istnieje"
            }), 409

        # Sprawdz VIN jesli podany
        vin = data.get("vin")

        if vin:
            existing_vin = (
                db.query(Samochod)
                .filter(Samochod.vin == vin)
                .first()
            )

            if existing_vin:
                return jsonify({
                    "error": "Pojazd z tym numerem VIN juz istnieje"
                }), 409

        created = Samochod(
            uzytkownik_id=user_id,
            vin=vin or None,
            numer_rejestracyjny=data.get("numer_rejestracyjny"),
            marka=data.get("marka"),
            model=data.get("model"),
            rok_produkcji=data.get("rok_produkcji"),
            pojemnosc_cm3=data.get("pojemnosc_cm3"),
            moc_km=data.get("moc_km"),
            paliwo=data.get("paliwo"),
            przebieg=data.get("przebieg") or 0,
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
# SAMOCHODY: GET ONE
# =========================

@app.route("/samochody/<int:car_id>", methods=["GET"])
@token_required
def get_samochod(car_id):

    db = SessionLocal()

    user_id = request.user["user_id"]

    try:

        car = (
            db.query(Samochod)
            .filter(Samochod.id == car_id)
            .filter(Samochod.uzytkownik_id == user_id)
            .first()
        )

        if not car:
            return jsonify({
                "error": "Pojazd nie istnieje lub nie nalezy do Ciebie"
            }), 404

        return jsonify(
            car_to_dict(car)
        ), 200

    except Exception as e:
        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


# =========================
# SAMOCHODY: UPDATE
# =========================

@app.route("/samochody/<int:car_id>", methods=["PUT"])
@token_required
def update_samochod(car_id):

    db = SessionLocal()

    user_id = request.user["user_id"]

    data = request.get_json(silent=True) or {}

    try:

        car = (
            db.query(Samochod)
            .filter(Samochod.id == car_id)
            .filter(Samochod.uzytkownik_id == user_id)
            .first()
        )

        if not car:
            return jsonify({
                "error": "Pojazd nie istnieje lub nie nalezy do Ciebie"
            }), 404

        # Walidacja numeru rejestracyjnego
        if "numer_rejestracyjny" in data:

            new_nr = (
                data.get("numer_rejestracyjny") or ""
            ).strip()

            if not new_nr:
                return jsonify({
                    "error": "Numer rejestracyjny nie moze byc pusty"
                }), 400

            if new_nr != car.numer_rejestracyjny:

                existing = (
                    db.query(Samochod)
                    .filter(
                        Samochod.numer_rejestracyjny == new_nr
                    )
                    .first()
                )

                if existing:
                    return jsonify({
                        "error": "Pojazd z tym numerem rejestracyjnym juz istnieje"
                    }), 409

                car.numer_rejestracyjny = new_nr

        # Walidacja VIN
        if "vin" in data:

            new_vin = (data.get("vin") or "").strip() or None

            if new_vin and new_vin != car.vin:

                existing_vin = (
                    db.query(Samochod)
                    .filter(Samochod.vin == new_vin)
                    .first()
                )

                if existing_vin:
                    return jsonify({
                        "error": "Pojazd z tym numerem VIN juz istnieje"
                    }), 409

            car.vin = new_vin

        # Pozostale pola
        for field in [
            "marka",
            "model",
            "rok_produkcji",
            "pojemnosc_cm3",
            "moc_km",
            "paliwo",
            "przebieg",
            "kolor"
        ]:
            if field in data:
                setattr(car, field, data.get(field))

        car.zaktualizowano_w = datetime.datetime.utcnow()

        db.commit()

        db.refresh(car)

        return jsonify({
            "message": "Pojazd zaktualizowany",
            "car": car_to_dict(car)
        }), 200

    except Exception as e:

        db.rollback()

        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


# =========================
# SAMOCHODY: DELETE
# =========================

@app.route("/samochody/<int:car_id>", methods=["DELETE"])
@token_required
def delete_samochod(car_id):

    db = SessionLocal()

    user_id = request.user["user_id"]

    try:

        car = (
            db.query(Samochod)
            .filter(Samochod.id == car_id)
            .filter(Samochod.uzytkownik_id == user_id)
            .first()
        )

        if not car:
            return jsonify({
                "error": "Pojazd nie istnieje lub nie nalezy do Ciebie"
            }), 404

        db.delete(car)

        db.commit()

        return jsonify({
            "message": "Pojazd usuniety"
        }), 200

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
@token_required
def get_wpisy_serwisowe():

    db = SessionLocal()

    user_id = request.user["user_id"]

    try:

        rows = (
            db.query(WpisSerwisowy)
            .join(Samochod)
            .filter(Samochod.uzytkownik_id == user_id)
            .order_by(
                WpisSerwisowy.data_serwisu.desc(),
                WpisSerwisowy.id.desc()
            )
            .all()
        )

        return jsonify([
            wpis_full_to_dict(row)
            for row in rows
        ]), 200

    except Exception as e:
        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


@app.route("/wpisy-serwisowe/<int:wpis_id>", methods=["GET"])
@token_required
def get_wpis_serwisowy(wpis_id):

    db = SessionLocal()

    user_id = request.user["user_id"]

    try:

        wpis = (
            db.query(WpisSerwisowy)
            .join(Samochod)
            .filter(WpisSerwisowy.id == wpis_id)
            .filter(Samochod.uzytkownik_id == user_id)
            .first()
        )

        if not wpis:
            return jsonify({
                "error": "Wpis nie istnieje lub nie nalezy do Ciebie"
            }), 404

        return jsonify(
            wpis_full_to_dict(wpis)
        ), 200

    except Exception as e:
        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


@app.route("/wpisy-serwisowe", methods=["POST"])
@token_required
def create_wpis_serwisowy():

    db = SessionLocal()

    user_id = request.user["user_id"]

    data = request.get_json(silent=True) or {}

    if not data.get("samochod_id") or not data.get("data_serwisu"):
        return jsonify({
            "error": "Wymagane pola: samochod_id, data_serwisu"
        }), 400

    try:

        # Sprawdz czy auto nalezy do usera
        car = (
            db.query(Samochod)
            .filter(Samochod.id == data["samochod_id"])
            .filter(Samochod.uzytkownik_id == user_id)
            .first()
        )

        if not car:
            return jsonify({
                "error": "Pojazd nie istnieje lub nie nalezy do Ciebie"
            }), 403

        # Stworz wpis
        created = WpisSerwisowy(
            samochod_id=data.get("samochod_id"),
            rodzaj_serwisu_id=data.get("rodzaj_serwisu_id"),
            data_serwisu=data.get("data_serwisu"),
            nazwa_warsztatu=data.get("nazwa_warsztatu"),
            adres_warsztatu=data.get("adres_warsztatu"),
            przebieg_przy_serwisie=data.get("przebieg_przy_serwisie"),
            nastepny_serwis_przebieg=data.get("nastepny_serwis_przebieg"),
            nastepna_data_serwisu=data.get("nastepna_data_serwisu") or None,
            opis=data.get("opis"),
            status=data.get("status", "zakonczony")
        )

        db.add(created)
        db.flush()  # zeby miec ID wpisu

        # Dodaj czynnosci
        for zad_data in (data.get("zadania") or []):

            if not zad_data.get("nazwa_zadania"):
                continue

            zadanie = ZadanieSerwisowe(
                wpis_serwisowy_id=created.id,
                nazwa_zadania=zad_data.get("nazwa_zadania"),
                opis=zad_data.get("opis"),
                koszt_robocizny=zad_data.get("koszt_robocizny") or 0
            )

            db.add(zadanie)

        # Dodaj uzyte czesci
        for uc_data in (data.get("uzyte_czesci") or []):

            # Czesc moze byc nowa (po nazwie) albo istniejaca (po czesc_id)
            czesc_id = uc_data.get("czesc_id")

            if not czesc_id and uc_data.get("nazwa_czesci"):

                # Sprawdz czy taka czesc juz jest
                existing_czesc = (
                    db.query(Czesc)
                    .filter(Czesc.nazwa == uc_data["nazwa_czesci"])
                    .filter(
                        Czesc.numer_czesci == uc_data.get("numer_czesci")
                    )
                    .first()
                )

                if existing_czesc:
                    czesc_id = existing_czesc.id
                else:
                    nowa_czesc = Czesc(
                        nazwa=uc_data["nazwa_czesci"],
                        producent=uc_data.get("producent_czesci"),
                        numer_czesci=uc_data.get("numer_czesci")
                    )
                    db.add(nowa_czesc)
                    db.flush()
                    czesc_id = nowa_czesc.id

            if not czesc_id:
                continue

            uzyta = UzytaCzesc(
                wpis_serwisowy_id=created.id,
                czesc_id=czesc_id,
                ilosc=uc_data.get("ilosc") or 1,
                cena_jednostkowa=uc_data.get("cena_jednostkowa") or 0
            )

            db.add(uzyta)

        db.commit()
        db.refresh(created)

        return jsonify(
            wpis_full_to_dict(created)
        ), 201

    except Exception as e:

        db.rollback()

        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()

# =========================
# RODZAJE SERWISOW
# =========================

@app.route("/rodzaje-serwisu", methods=["GET"])
@token_required
def get_rodzaje_serwisu():

    db = SessionLocal()

    try:

        rows = (
            db.query(RodzajSerwisu)
            .order_by(RodzajSerwisu.nazwa)
            .all()
        )

        return jsonify([
            rodzaj_serwisu_to_dict(row)
            for row in rows
        ]), 200

    except Exception as e:
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

# =========================
# SERIALIZERS
# =========================

def zadanie_to_dict(z: ZadanieSerwisowe):
    return {
        "id": z.id,
        "wpis_serwisowy_id": z.wpis_serwisowy_id,
        "nazwa_zadania": z.nazwa_zadania,
        "opis": z.opis,
        "koszt_robocizny": float(z.koszt_robocizny or 0)
    }


def czesc_to_dict(c: Czesc):
    return {
        "id": c.id,
        "nazwa": c.nazwa,
        "producent": c.producent,
        "numer_czesci": c.numer_czesci
    }


def uzyta_czesc_to_dict(uc: UzytaCzesc):
    return {
        "id": uc.id,
        "wpis_serwisowy_id": uc.wpis_serwisowy_id,
        "czesc_id": uc.czesc_id,
        "ilosc": float(uc.ilosc or 0),
        "cena_jednostkowa": float(uc.cena_jednostkowa or 0),
        "suma": float((uc.ilosc or 0) * (uc.cena_jednostkowa or 0)),
        "czesc": czesc_to_dict(uc.czesc) if uc.czesc else None
    }


def rodzaj_serwisu_to_dict(rs: RodzajSerwisu):
    return {
        "id": rs.id,
        "nazwa": rs.nazwa,
        "opis": rs.opis
    }


def wpis_full_to_dict(w: WpisSerwisowy):
    """Wpis serwisowy z pelnymi danymi - czynnosci, czesci, koszt"""
    koszt_robocizny = sum(
        float(z.koszt_robocizny or 0)
        for z in w.zadania
    )
    koszt_czesci = sum(
        float((uc.ilosc or 0) * (uc.cena_jednostkowa or 0))
        for uc in w.uzyte_czesci
    )

    return {
        "id": w.id,
        "samochod_id": w.samochod_id,
        "rodzaj_serwisu_id": w.rodzaj_serwisu_id,
        "rodzaj_serwisu": (
            rodzaj_serwisu_to_dict(w.rodzaj_serwisu)
            if w.rodzaj_serwisu else None
        ),
        "data_serwisu": (
            w.data_serwisu.isoformat()
            if w.data_serwisu else None
        ),
        "nazwa_warsztatu": w.nazwa_warsztatu,
        "adres_warsztatu": w.adres_warsztatu,
        "przebieg_przy_serwisie": w.przebieg_przy_serwisie,
        "nastepny_serwis_przebieg": w.nastepny_serwis_przebieg,
        "nastepna_data_serwisu": (
            w.nastepna_data_serwisu.isoformat()
            if w.nastepna_data_serwisu else None
        ),
        "opis": w.opis,
        "status": w.status,
        "zadania": [zadanie_to_dict(z) for z in w.zadania],
        "uzyte_czesci": [
            uzyta_czesc_to_dict(uc) for uc in w.uzyte_czesci
        ],
        "koszt_robocizny": koszt_robocizny,
        "koszt_czesci": koszt_czesci,
        "koszt_calkowity": koszt_robocizny + koszt_czesci,
        "samochod": {
            "id": w.samochod.id,
            "marka": w.samochod.marka,
            "model": w.samochod.model,
            "numer_rejestracyjny": w.samochod.numer_rejestracyjny,
            "vin": w.samochod.vin
        } if w.samochod else None,
        "ma_raport": w.raport is not None,
        "raport_id": w.raport.id if w.raport else None
    }

# =========================
# RAPORTY: HELPERS
# =========================

def raport_to_dict(r: Raport):
    return {
        "id": r.id,
        "wpis_serwisowy_id": r.wpis_serwisowy_id,
        "numer_raportu": r.numer_raportu,
        "sciezka_do_pliku": r.sciezka_do_pliku,
        "wygenerowano_w": (
            r.wygenerowano_w.isoformat()
            if r.wygenerowano_w else None
        )
    }


def raport_full_to_dict(r: Raport):
    """Raport z danymi wpisu serwisowego dla listy."""
    wpis = r.wpis_serwisowy
    car = wpis.samochod if wpis else None

    koszt_robocizny = sum(
        float(z.koszt_robocizny or 0)
        for z in (wpis.zadania if wpis else [])
    )
    koszt_czesci = sum(
        float((uc.ilosc or 0) * (uc.cena_jednostkowa or 0))
        for uc in (wpis.uzyte_czesci if wpis else [])
    )

    return {
        **raport_to_dict(r),
        "wpis_serwisowy": {
            "id": wpis.id,
            "data_serwisu": (
                wpis.data_serwisu.isoformat()
                if wpis.data_serwisu else None
            ),
            "nazwa_warsztatu": wpis.nazwa_warsztatu,
            "opis": wpis.opis,
            "status": wpis.status,
            "rodzaj_serwisu_nazwa": (
                wpis.rodzaj_serwisu.nazwa
                if wpis.rodzaj_serwisu else None
            ),
            "koszt_calkowity": koszt_robocizny + koszt_czesci
        } if wpis else None,
        "samochod": {
            "id": car.id,
            "marka": car.marka,
            "model": car.model,
            "numer_rejestracyjny": car.numer_rejestracyjny
        } if car else None,
        "nazwy_czynnosci": [
            z.nazwa_zadania
            for z in (wpis.zadania if wpis else [])
        ]
    }


def build_report_data(wpis: WpisSerwisowy, raport: Raport):
    """Sklada slownik do generatora PDF."""
    user = wpis.samochod.uzytkownik

    koszt_robocizny = sum(
        float(z.koszt_robocizny or 0)
        for z in wpis.zadania
    )
    koszt_czesci = sum(
        float((uc.ilosc or 0) * (uc.cena_jednostkowa or 0))
        for uc in wpis.uzyte_czesci
    )

    return {
        "numer_raportu": raport.numer_raportu,
        "wygenerowano_w": (
            raport.wygenerowano_w.strftime("%Y-%m-%d %H:%M")
            if raport.wygenerowano_w
            else datetime.datetime.now().strftime("%Y-%m-%d %H:%M")
        ),
        "uzytkownik": {
            "imie": user.imie,
            "nazwisko": user.nazwisko,
            "email": user.email
        },
        "samochod": {
            "marka": wpis.samochod.marka,
            "model": wpis.samochod.model,
            "numer_rejestracyjny": wpis.samochod.numer_rejestracyjny,
            "vin": wpis.samochod.vin
        },
        "wpis": {
            "data_serwisu": (
                wpis.data_serwisu.isoformat()
                if wpis.data_serwisu else None
            ),
            "nazwa_warsztatu": wpis.nazwa_warsztatu,
            "adres_warsztatu": wpis.adres_warsztatu,
            "przebieg_przy_serwisie": wpis.przebieg_przy_serwisie,
            "opis": wpis.opis,
            "rodzaj_serwisu_nazwa": (
                wpis.rodzaj_serwisu.nazwa
                if wpis.rodzaj_serwisu else None
            ),
            "status": wpis.status
        },
        "zadania": [
            {
                "nazwa": z.nazwa_zadania,
                "opis": z.opis,
                "koszt": float(z.koszt_robocizny or 0)
            }
            for z in wpis.zadania
        ],
        "czesci": [
            {
                "nazwa": uc.czesc.nazwa if uc.czesc else "-",
                "producent": uc.czesc.producent if uc.czesc else None,
                "ilosc": float(uc.ilosc or 0),
                "cena_jednostkowa": float(uc.cena_jednostkowa or 0),
                "suma": float((uc.ilosc or 0) * (uc.cena_jednostkowa or 0))
            }
            for uc in wpis.uzyte_czesci
        ],
        "koszt_robocizny": koszt_robocizny,
        "koszt_czesci": koszt_czesci,
        "koszt_calkowity": koszt_robocizny + koszt_czesci
    }


# =========================
# RAPORTY: LISTA Z FILTRAMI
# =========================

@app.route("/raporty", methods=["GET"])
@token_required
def get_raporty():

    db = SessionLocal()

    user_id = request.user["user_id"]

    # Query params (filtry)
    search = (request.args.get("search") or "").strip().lower()
    date_from = request.args.get("date_from")
    date_to = request.args.get("date_to")
    warsztat = (request.args.get("warsztat") or "").strip().lower()
    cost_min = request.args.get("cost_min")
    cost_max = request.args.get("cost_max")
    samochod_id = request.args.get("samochod_id")

    try:

        query = (
            db.query(Raport)
            .join(WpisSerwisowy)
            .join(Samochod)
            .filter(Samochod.uzytkownik_id == user_id)
        )

        if date_from:
            query = query.filter(
                WpisSerwisowy.data_serwisu >= date_from
            )

        if date_to:
            query = query.filter(
                WpisSerwisowy.data_serwisu <= date_to
            )

        if samochod_id:
            query = query.filter(
                Samochod.id == int(samochod_id)
            )

        raporty = (
            query
            .order_by(Raport.wygenerowano_w.desc())
            .all()
        )

        # Filtry dzialajace po stronie Pythona
        # (wymagaja agregacji albo searcha w czynnosciach)
        result = []

        for r in raporty:

            wpis = r.wpis_serwisowy

            if not wpis:
                continue

            # Warsztat
            if warsztat and warsztat not in (
                (wpis.nazwa_warsztatu or "").lower()
            ):
                continue

            # Koszt
            koszt = sum(
                float(z.koszt_robocizny or 0)
                for z in wpis.zadania
            ) + sum(
                float((uc.ilosc or 0) * (uc.cena_jednostkowa or 0))
                for uc in wpis.uzyte_czesci
            )

            if cost_min and koszt < float(cost_min):
                continue

            if cost_max and koszt > float(cost_max):
                continue

            # Search po czynnosciach + opisie + numerze raportu
            if search:
                haystack_parts = [
                    r.numer_raportu or "",
                    wpis.opis or "",
                    wpis.nazwa_warsztatu or ""
                ]
                haystack_parts.extend(
                    (z.nazwa_zadania or "")
                    for z in wpis.zadania
                )
                haystack_parts.extend(
                    (uc.czesc.nazwa or "")
                    for uc in wpis.uzyte_czesci
                    if uc.czesc
                )

                haystack = " ".join(haystack_parts).lower()

                if search not in haystack:
                    continue

            result.append(raport_full_to_dict(r))

        return jsonify(result), 200

    except Exception as e:
        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


# =========================
# RAPORTY: GENEROWANIE
# =========================

@app.route(
    "/raporty/generuj/<int:wpis_id>",
    methods=["POST"]
)
@token_required
def generate_raport(wpis_id):

    db = SessionLocal()

    user_id = request.user["user_id"]

    try:

        wpis = (
            db.query(WpisSerwisowy)
            .join(Samochod)
            .filter(WpisSerwisowy.id == wpis_id)
            .filter(Samochod.uzytkownik_id == user_id)
            .first()
        )

        if not wpis:
            return jsonify({
                "error": "Wpis nie istnieje lub nie nalezy do Ciebie"
            }), 404

        # Sprawdz czy juz nie ma raportu (relacja 1:1)
        if wpis.raport:
            return jsonify({
                "error": "Raport dla tego wpisu juz istnieje",
                "raport_id": wpis.raport.id
            }), 409

        # Stworz wpis raportu (bez sciezki, bez numeru - dostaniemy ID)
        raport = Raport(
            wpis_serwisowy_id=wpis.id,
            numer_raportu="TEMP",
            sciezka_do_pliku=None
        )
        db.add(raport)
        db.flush()  # zeby miec ID

        # Wygeneruj numer raportu w formacie R/2026/00001
        year = datetime.datetime.now().year
        raport.numer_raportu = generate_report_number(raport.id, year)

        # Sciezka do PDF
        pdf_filename = (
            f"raport_{raport.numer_raportu.replace('/', '_')}.pdf"
        )
        pdf_path = os.path.join(REPORTS_DIR, pdf_filename)

        # Buduj dane dla generatora
        report_data = build_report_data(wpis, raport)

        # Generuj PDF
        generate_service_report_pdf(report_data, pdf_path)

        # Zapisz sciezke
        raport.sciezka_do_pliku = pdf_path

        db.commit()
        db.refresh(raport)

        return jsonify({
            "message": "Raport wygenerowany",
            "raport": raport_full_to_dict(raport)
        }), 201

    except Exception as e:

        db.rollback()

        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


# =========================
# RAPORTY: POBIERANIE PDF
# =========================

@app.route("/raporty/<int:raport_id>/pdf", methods=["GET"])
@token_required
def download_raport_pdf(raport_id):

    from flask import send_file

    db = SessionLocal()

    user_id = request.user["user_id"]

    try:

        raport = (
            db.query(Raport)
            .join(WpisSerwisowy)
            .join(Samochod)
            .filter(Raport.id == raport_id)
            .filter(Samochod.uzytkownik_id == user_id)
            .first()
        )

        if not raport:
            return jsonify({
                "error": "Raport nie istnieje lub nie nalezy do Ciebie"
            }), 404

        if not raport.sciezka_do_pliku or not os.path.exists(
            raport.sciezka_do_pliku
        ):
            return jsonify({
                "error": "Plik PDF nie istnieje na dysku"
            }), 404

        return send_file(
            raport.sciezka_do_pliku,
            mimetype="application/pdf",
            as_attachment=True,
            download_name=(
                f"raport_{raport.numer_raportu.replace('/', '_')}.pdf"
            )
        )

    except Exception as e:
        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


# =========================
# RAPORTY: WYSYLKA EMAIL
# =========================

@app.route(
    "/raporty/<int:raport_id>/wyslij-email",
    methods=["POST"]
)
@token_required
def send_raport_email(raport_id):

    db = SessionLocal()

    user_id = request.user["user_id"]

    data = request.get_json(silent=True) or {}

    # Opcjonalnie: inny email niz user-a (np. do warsztatu)
    custom_email = (data.get("email") or "").strip()

    try:

        raport = (
            db.query(Raport)
            .join(WpisSerwisowy)
            .join(Samochod)
            .filter(Raport.id == raport_id)
            .filter(Samochod.uzytkownik_id == user_id)
            .first()
        )

        if not raport:
            return jsonify({
                "error": "Raport nie istnieje lub nie nalezy do Ciebie"
            }), 404

        if not raport.sciezka_do_pliku or not os.path.exists(
            raport.sciezka_do_pliku
        ):
            return jsonify({
                "error": "Plik PDF nie istnieje na dysku"
            }), 404

        car = raport.wpis_serwisowy.samochod
        user = car.uzytkownik

        target_email = custom_email or user.email
        car_info = (
            f"{car.marka} {car.model} ({car.numer_rejestracyjny})"
        )

        # Wyslij email
        result = send_report_email(
            to_email=target_email,
            report_number=raport.numer_raportu,
            pdf_path=raport.sciezka_do_pliku,
            car_info=car_info
        )

        if not result["success"]:
            return jsonify({
                "error": result["message"]
            }), 500

        # Zapisz historie w powiadomienia_email
        powiadomienie = PowiadomienieEmail(
            uzytkownik_id=user.id,
            samochod_id=car.id,
            raport_id=raport.id,
            adres_email=target_email,
            temat=(
                f"DriveOps - Raport serwisowy {raport.numer_raportu}"
            ),
            wiadomosc=(
                f"Raport {raport.numer_raportu} dla {car_info}"
            ),
            status="wyslane",
            wyslano_w=datetime.datetime.utcnow()
        )
        db.add(powiadomienie)
        db.commit()

        return jsonify({
            "message": result["message"],
            "email": target_email
        }), 200

    except Exception as e:

        db.rollback()

        return jsonify({
            "error": str(e)
        }), 500

    finally:
        db.close()


# =========================
# RAPORTY: USUWANIE
# =========================

@app.route("/raporty/<int:raport_id>", methods=["DELETE"])
@token_required
def delete_raport(raport_id):

    db = SessionLocal()

    user_id = request.user["user_id"]

    try:

        raport = (
            db.query(Raport)
            .join(WpisSerwisowy)
            .join(Samochod)
            .filter(Raport.id == raport_id)
            .filter(Samochod.uzytkownik_id == user_id)
            .first()
        )

        if not raport:
            return jsonify({
                "error": "Raport nie istnieje lub nie nalezy do Ciebie"
            }), 404

        # Usun plik PDF
        if raport.sciezka_do_pliku and os.path.exists(
            raport.sciezka_do_pliku
        ):
            try:
                os.remove(raport.sciezka_do_pliku)
            except OSError:
                pass  # ignorujemy bledy IO

        db.delete(raport)
        db.commit()

        return jsonify({
            "message": "Raport usuniety"
        }), 200

    except Exception as e:

        db.rollback()

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