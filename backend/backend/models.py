from sqlalchemy import (
    Column,
    Integer,
    String,
    Boolean,
    Text,
    Date,
    TIMESTAMP,
    ForeignKey,
    Numeric,
    Enum,
    text
)

from sqlalchemy.orm import relationship

from db import Base


# =========================
# UZYTKOWNICY
# =========================

class Uzytkownik(Base):
    __tablename__ = "uzytkownicy"

    id = Column(Integer, primary_key=True)

    imie = Column(String(100), nullable=False)

    nazwisko = Column(String(100), nullable=False)

    email = Column(String(255), nullable=False, unique=True)

    haslo_hash = Column(Text, nullable=False)

    telefon = Column(String(20))

    czy_aktywny = Column(
        Boolean,
        nullable=False,
        server_default=text("TRUE")
    )

    utworzono_w = Column(
        TIMESTAMP,
        nullable=False,
        server_default=text("NOW()")
    )

    zaktualizowano_w = Column(
        TIMESTAMP,
        nullable=False,
        server_default=text("NOW()")
    )

    samochody = relationship(
        "Samochod",
        back_populates="uzytkownik",
        cascade="all, delete"
    )

    powiadomienia = relationship(
        "PowiadomienieEmail",
        back_populates="uzytkownik",
        cascade="all, delete"
    )


# =========================
# SAMOCHODY
# =========================

class Samochod(Base):
    __tablename__ = "samochody"

    id = Column(Integer, primary_key=True)

    uzytkownik_id = Column(
        Integer,
        ForeignKey("uzytkownicy.id", ondelete="CASCADE"),
        nullable=False
    )

    vin = Column(String(17), unique=True)

    numer_rejestracyjny = Column(
        String(20),
        nullable=False,
        unique=True
    )

    marka = Column(String(100), nullable=False)

    model = Column(String(100), nullable=False)

    rok_produkcji = Column(Integer)

    pojemnosc_cm3 = Column(Integer)

    moc_km = Column(Integer)

    paliwo = Column(
        Enum(
            "benzyna",
            "diesel",
            "elektryczny",
            "hybryda",
            "benzyna_gaz",
            name="rodzaj_paliwa"
        )
    )

    przebieg = Column(
        Integer,
        server_default=text("0")
    )

    kolor = Column(String(50))

    utworzono_w = Column(
        TIMESTAMP,
        nullable=False,
        server_default=text("NOW()")
    )

    zaktualizowano_w = Column(
        TIMESTAMP,
        nullable=False,
        server_default=text("NOW()")
    )

    uzytkownik = relationship(
        "Uzytkownik",
        back_populates="samochody"
    )

    wpisy_serwisowe = relationship(
        "WpisSerwisowy",
        back_populates="samochod",
        cascade="all, delete"
    )

    przeglady = relationship(
        "PrzegladTechniczny",
        back_populates="samochod",
        cascade="all, delete"
    )

    powiadomienia = relationship(
        "PowiadomienieEmail",
        back_populates="samochod"
    )


# =========================
# RODZAJE SERWISU
# =========================

class RodzajSerwisu(Base):
    __tablename__ = "rodzaje_serwisu"

    id = Column(Integer, primary_key=True)

    nazwa = Column(
        String(100),
        nullable=False,
        unique=True
    )

    opis = Column(Text)

    utworzono_w = Column(
        TIMESTAMP,
        nullable=False,
        server_default=text("NOW()")
    )

    zaktualizowano_w = Column(
        TIMESTAMP,
        nullable=False,
        server_default=text("NOW()")
    )

    wpisy_serwisowe = relationship(
        "WpisSerwisowy",
        back_populates="rodzaj_serwisu"
    )


# =========================
# WPISY SERWISOWE
# =========================

class WpisSerwisowy(Base):
    __tablename__ = "wpisy_serwisowe"

    id = Column(Integer, primary_key=True)

    samochod_id = Column(
        Integer,
        ForeignKey("samochody.id", ondelete="CASCADE"),
        nullable=False
    )

    rodzaj_serwisu_id = Column(
        Integer,
        ForeignKey("rodzaje_serwisu.id", ondelete="SET NULL")
    )

    data_serwisu = Column(Date, nullable=False)

    nazwa_warsztatu = Column(String(255))

    adres_warsztatu = Column(Text)

    przebieg_przy_serwisie = Column(Integer)

    nastepny_serwis_przebieg = Column(Integer)

    nastepna_data_serwisu = Column(Date)

    opis = Column(Text)

    status = Column(
        Enum(
            "w_toku",
            "zakonczony",
            "anulowany",
            name="status_wpisu"
        ),
        nullable=False,
        server_default=text("'w_toku'")
    )

    utworzono_w = Column(
        TIMESTAMP,
        nullable=False,
        server_default=text("NOW()")
    )

    zaktualizowano_w = Column(
        TIMESTAMP,
        nullable=False,
        server_default=text("NOW()")
    )

    samochod = relationship(
        "Samochod",
        back_populates="wpisy_serwisowe"
    )

    rodzaj_serwisu = relationship(
        "RodzajSerwisu",
        back_populates="wpisy_serwisowe"
    )

    zadania = relationship(
        "ZadanieSerwisowe",
        back_populates="wpis_serwisowy",
        cascade="all, delete"
    )

    uzyte_czesci = relationship(
        "UzytaCzesc",
        back_populates="wpis_serwisowy",
        cascade="all, delete"
    )

    raport = relationship(
        "Raport",
        back_populates="wpis_serwisowy",
        uselist=False,
        cascade="all, delete"
    )


# =========================
# ZADANIA SERWISOWE
# =========================

class ZadanieSerwisowe(Base):
    __tablename__ = "zadania_serwisowe"

    id = Column(Integer, primary_key=True)

    wpis_serwisowy_id = Column(
        Integer,
        ForeignKey("wpisy_serwisowe.id", ondelete="CASCADE"),
        nullable=False
    )

    nazwa_zadania = Column(
        String(255),
        nullable=False
    )

    opis = Column(Text)

    koszt_robocizny = Column(
        Numeric(10, 2),
        nullable=False,
        server_default=text("0")
    )

    wpis_serwisowy = relationship(
        "WpisSerwisowy",
        back_populates="zadania"
    )


# =========================
# CZESCI
# =========================

class Czesc(Base):
    __tablename__ = "czesci"

    id = Column(Integer, primary_key=True)

    nazwa = Column(
        String(255),
        nullable=False
    )

    producent = Column(String(255))

    numer_czesci = Column(String(100))

    utworzono_w = Column(
        TIMESTAMP,
        nullable=False,
        server_default=text("NOW()")
    )

    zaktualizowano_w = Column(
        TIMESTAMP,
        nullable=False,
        server_default=text("NOW()")
    )

    uzycia = relationship(
        "UzytaCzesc",
        back_populates="czesc"
    )


# =========================
# UZYTE CZESCI
# =========================

class UzytaCzesc(Base):
    __tablename__ = "uzyte_czesci"

    id = Column(Integer, primary_key=True)

    wpis_serwisowy_id = Column(
        Integer,
        ForeignKey("wpisy_serwisowe.id", ondelete="CASCADE"),
        nullable=False
    )

    czesc_id = Column(
        Integer,
        ForeignKey("czesci.id", ondelete="RESTRICT"),
        nullable=False
    )

    ilosc = Column(
        Numeric(10, 2),
        nullable=False,
        server_default=text("1")
    )

    cena_jednostkowa = Column(
        Numeric(10, 2),
        nullable=False,
        server_default=text("0")
    )

    wpis_serwisowy = relationship(
        "WpisSerwisowy",
        back_populates="uzyte_czesci"
    )

    czesc = relationship(
        "Czesc",
        back_populates="uzycia"
    )


# =========================
# PRZEGLADY TECHNICZNE
# =========================

class PrzegladTechniczny(Base):
    __tablename__ = "przeglady_techniczne"

    id = Column(Integer, primary_key=True)

    samochod_id = Column(
        Integer,
        ForeignKey("samochody.id", ondelete="CASCADE"),
        nullable=False
    )

    data_wykonania = Column(
        Date,
        nullable=False
    )

    data_waznosci = Column(
        Date,
        nullable=False
    )

    wynik = Column(
        Enum(
            "pozytywny",
            "negatywny",
            name="wynik_przegladu"
        ),
        nullable=False,
        server_default=text("'pozytywny'")
    )

    stacja_kontroli = Column(String(255))

    przebieg = Column(Integer)

    uwagi = Column(Text)

    utworzono_w = Column(
        TIMESTAMP,
        nullable=False,
        server_default=text("NOW()")
    )

    zaktualizowano_w = Column(
        TIMESTAMP,
        nullable=False,
        server_default=text("NOW()")
    )

    samochod = relationship(
        "Samochod",
        back_populates="przeglady"
    )

    powiadomienia = relationship(
        "PowiadomienieEmail",
        back_populates="przeglad"
    )


# =========================
# RAPORTY
# =========================

class Raport(Base):
    __tablename__ = "raporty"

    id = Column(Integer, primary_key=True)

    wpis_serwisowy_id = Column(
        Integer,
        ForeignKey("wpisy_serwisowe.id", ondelete="CASCADE"),
        nullable=False,
        unique=True
    )

    numer_raportu = Column(
        String(50),
        nullable=False,
        unique=True
    )

    sciezka_do_pliku = Column(Text)

    wygenerowano_w = Column(
        TIMESTAMP,
        nullable=False,
        server_default=text("NOW()")
    )

    wpis_serwisowy = relationship(
        "WpisSerwisowy",
        back_populates="raport"
    )

    powiadomienia = relationship(
        "PowiadomienieEmail",
        back_populates="raport"
    )


# =========================
# POWIADOMIENIA EMAIL
# =========================

class PowiadomienieEmail(Base):
    __tablename__ = "powiadomienia_email"

    id = Column(Integer, primary_key=True)

    uzytkownik_id = Column(
        Integer,
        ForeignKey("uzytkownicy.id", ondelete="CASCADE"),
        nullable=False
    )

    samochod_id = Column(
        Integer,
        ForeignKey("samochody.id", ondelete="SET NULL")
    )

    raport_id = Column(
        Integer,
        ForeignKey("raporty.id", ondelete="SET NULL")
    )

    przeglad_id = Column(
        Integer,
        ForeignKey("przeglady_techniczne.id", ondelete="SET NULL")
    )

    adres_email = Column(
        String(255),
        nullable=False
    )

    temat = Column(
        String(255),
        nullable=False
    )

    wiadomosc = Column(Text)

    status = Column(
        Enum(
            "oczekujace",
            "wyslane",
            "blad",
            name="status_powiadomienia"
        ),
        nullable=False,
        server_default=text("'oczekujace'")
    )

    utworzono_w = Column(
        TIMESTAMP,
        nullable=False,
        server_default=text("NOW()")
    )

    wyslano_w = Column(TIMESTAMP)

    uzytkownik = relationship(
        "Uzytkownik",
        back_populates="powiadomienia"
    )

    samochod = relationship(
        "Samochod",
        back_populates="powiadomienia"
    )

    raport = relationship(
        "Raport",
        back_populates="powiadomienia"
    )

    przeglad = relationship(
        "PrzegladTechniczny",
        back_populates="powiadomienia"
    )


# =========================
# WIDOK Z KOSZTAMI
# =========================

class WpisSerwisowyZKosztem(Base):
    __tablename__ = "v_wpisy_serwisowe_z_kosztem"

    id = Column(Integer, primary_key=True)

    samochod_id = Column(Integer)

    rodzaj_serwisu_id = Column(Integer)

    data_serwisu = Column(Date)

    nazwa_warsztatu = Column(String(255))

    adres_warsztatu = Column(Text)

    przebieg_przy_serwisie = Column(Integer)

    nastepny_serwis_przebieg = Column(Integer)

    nastepna_data_serwisu = Column(Date)

    opis = Column(Text)

    status = Column(String)

    utworzono_w = Column(TIMESTAMP)

    zaktualizowano_w = Column(TIMESTAMP)

    calkowity_koszt = Column(Numeric(10, 2))