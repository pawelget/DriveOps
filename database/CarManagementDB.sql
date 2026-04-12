CREATE TYPE status_powiadomienia AS ENUM ('oczekujace', 'wyslane', 'blad');
CREATE TYPE wynik_przegladu AS ENUM ('pozytywny', 'negatywny');
CREATE TYPE status_wpisu AS ENUM ('w_toku', 'zakonczony', 'anulowany');
CREATE TYPE rodzaj_paliwa AS ENUM ('benzyna', 'diesel', 'elektryczny', 'hybryda', 'benzyna_gaz');

-- funkcja do autoakualizacji - zaktualizowano_w

CREATE OR REPLACE FUNCTION ustaw_zaktualizowano_w()
RETURNS TRIGGER AS $$
BEGIN
    NEW.zaktualizowano_w = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TABLE uzytkownicy (
    id                SERIAL PRIMARY KEY,
    imie              VARCHAR(100) NOT NULL,
    nazwisko          VARCHAR(100) NOT NULL,
    email             VARCHAR(255) NOT NULL UNIQUE,
    haslo_hash        TEXT NOT NULL,
    telefon           VARCHAR(20),
    czy_aktywny       BOOLEAN NOT NULL DEFAULT TRUE,
    utworzono_w       TIMESTAMP NOT NULL DEFAULT NOW(),
    zaktualizowano_w  TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_uzytkownicy_email
        CHECK (position('@' in email) > 1)
);

CREATE TRIGGER trg_uzytkownicy_zaktualizowano_w
BEFORE UPDATE ON uzytkownicy
FOR EACH ROW
EXECUTE FUNCTION ustaw_zaktualizowano_w();


CREATE TABLE samochody (
    id                   SERIAL PRIMARY KEY,
    uzytkownik_id        INT NOT NULL REFERENCES uzytkownicy(id) ON DELETE CASCADE,
    vin                  VARCHAR(17) UNIQUE,
    numer_rejestracyjny  VARCHAR(20) NOT NULL UNIQUE,
    marka                VARCHAR(100) NOT NULL,
    model                VARCHAR(100) NOT NULL,
    rok_produkcji        INT,
    pojemnosc_cm3        INT,
    moc_km               INT,
    paliwo               rodzaj_paliwa,
    przebieg             INT DEFAULT 0,
    kolor                VARCHAR(50),
    utworzono_w          TIMESTAMP NOT NULL DEFAULT NOW(),
    zaktualizowano_w     TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_samochody_vin_dlugosc
        CHECK (char_length(vin) = 17),

    CONSTRAINT chk_samochody_rok
        CHECK (
            rok_produkcji IS NULL
            OR (rok_produkcji >= 1886 AND rok_produkcji <= EXTRACT(YEAR FROM CURRENT_DATE)::INT + 1)
        ),

    CONSTRAINT chk_samochody_pojemnosc
        CHECK (pojemnosc_cm3 IS NULL OR pojemnosc_cm3 > 0),

    CONSTRAINT chk_samochody_moc
        CHECK (moc_km IS NULL OR moc_km > 0),

    CONSTRAINT chk_samochody_przebieg
        CHECK (przebieg IS NULL OR przebieg >= 0)
);

CREATE TRIGGER trg_samochody_zaktualizowano_w
BEFORE UPDATE ON samochody
FOR EACH ROW
EXECUTE FUNCTION ustaw_zaktualizowano_w();


CREATE TABLE rodzaje_serwisu (
    id                SERIAL PRIMARY KEY,
    nazwa             VARCHAR(100) NOT NULL UNIQUE,
    opis              TEXT,
    utworzono_w       TIMESTAMP NOT NULL DEFAULT NOW(),
    zaktualizowano_w  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_rodzaje_serwisu_zaktualizowano_w
BEFORE UPDATE ON rodzaje_serwisu
FOR EACH ROW
EXECUTE FUNCTION ustaw_zaktualizowano_w();


CREATE TABLE wpisy_serwisowe (
    id                       SERIAL PRIMARY KEY,
    samochod_id              INT NOT NULL REFERENCES samochody(id) ON DELETE CASCADE,
    rodzaj_serwisu_id        INT REFERENCES rodzaje_serwisu(id) ON DELETE SET NULL,
    data_serwisu             DATE NOT NULL,
    nazwa_warsztatu          VARCHAR(255),
    adres_warsztatu          TEXT,
    przebieg_przy_serwisie   INT,
    nastepny_serwis_przebieg INT,
    nastepna_data_serwisu    DATE,
    opis                     TEXT,
    status                   status_wpisu NOT NULL DEFAULT 'w_toku',
    utworzono_w              TIMESTAMP NOT NULL DEFAULT NOW(),
    zaktualizowano_w         TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_wpisy_przebieg_przy_serwisie
        CHECK (przebieg_przy_serwisie IS NULL OR przebieg_przy_serwisie >= 0),

    CONSTRAINT chk_wpisy_nastepny_przebieg
        CHECK (nastepny_serwis_przebieg IS NULL OR nastepny_serwis_przebieg >= 0),

    CONSTRAINT chk_wpisy_nastepna_data
        CHECK (nastepna_data_serwisu IS NULL OR nastepna_data_serwisu >= data_serwisu)
);

CREATE TRIGGER trg_wpisy_serwisowe_zaktualizowano_w
BEFORE UPDATE ON wpisy_serwisowe
FOR EACH ROW
EXECUTE FUNCTION ustaw_zaktualizowano_w();


CREATE TABLE zadania_serwisowe (
    id                SERIAL PRIMARY KEY,
    wpis_serwisowy_id INT NOT NULL REFERENCES wpisy_serwisowe(id) ON DELETE CASCADE,
    nazwa_zadania     VARCHAR(255) NOT NULL,
    opis              TEXT,
    koszt_robocizny   NUMERIC(10,2) NOT NULL DEFAULT 0,

    CONSTRAINT chk_zadania_koszt
        CHECK (koszt_robocizny >= 0)
);


CREATE TABLE czesci (
    id                SERIAL PRIMARY KEY,
    nazwa             VARCHAR(255) NOT NULL,
    producent         VARCHAR(255),
    numer_czesci      VARCHAR(100),
    utworzono_w       TIMESTAMP NOT NULL DEFAULT NOW(),
    zaktualizowano_w  TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_czesci UNIQUE (nazwa, producent)
);

CREATE TRIGGER trg_czesci_zaktualizowano_w
BEFORE UPDATE ON czesci
FOR EACH ROW
EXECUTE FUNCTION ustaw_zaktualizowano_w();


CREATE TABLE uzyte_czesci (
    id                SERIAL PRIMARY KEY,
    wpis_serwisowy_id INT NOT NULL REFERENCES wpisy_serwisowe(id) ON DELETE CASCADE,
    czesc_id          INT NOT NULL REFERENCES czesci(id) ON DELETE RESTRICT,
    ilosc             NUMERIC(10,2) NOT NULL DEFAULT 1,
    cena_jednostkowa  NUMERIC(10,2) NOT NULL DEFAULT 0,

    CONSTRAINT chk_uzyte_czesci_ilosc
        CHECK (ilosc > 0),

    CONSTRAINT chk_uzyte_czesci_cena
        CHECK (cena_jednostkowa >= 0),

    CONSTRAINT uq_uzyte_czesci UNIQUE (wpis_serwisowy_id, czesc_id)
);


CREATE TABLE przeglady_techniczne (
    id                SERIAL PRIMARY KEY,
    samochod_id       INT NOT NULL REFERENCES samochody(id) ON DELETE CASCADE,
    data_wykonania    DATE NOT NULL,
    data_waznosci     DATE NOT NULL,
    wynik             wynik_przegladu NOT NULL DEFAULT 'pozytywny',
    stacja_kontroli   VARCHAR(255),
    przebieg          INT,
    uwagi             TEXT,
    utworzono_w       TIMESTAMP NOT NULL DEFAULT NOW(),
    zaktualizowano_w  TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_przeglady_data
        CHECK (data_waznosci > data_wykonania),

    CONSTRAINT chk_przeglady_przebieg
        CHECK (przebieg IS NULL OR przebieg >= 0)
);

CREATE TRIGGER trg_przeglady_techniczne_zaktualizowano_w
BEFORE UPDATE ON przeglady_techniczne
FOR EACH ROW
EXECUTE FUNCTION ustaw_zaktualizowano_w();


CREATE TABLE raporty (
    id                SERIAL PRIMARY KEY,
    wpis_serwisowy_id INT NOT NULL UNIQUE REFERENCES wpisy_serwisowe(id) ON DELETE CASCADE,
    numer_raportu     VARCHAR(50) NOT NULL UNIQUE,
    sciezka_do_pliku  TEXT,
    wygenerowano_w    TIMESTAMP NOT NULL DEFAULT NOW()
);


CREATE TABLE powiadomienia_email (
    id                SERIAL PRIMARY KEY,
    uzytkownik_id     INT NOT NULL REFERENCES uzytkownicy(id) ON DELETE CASCADE,
    samochod_id       INT REFERENCES samochody(id) ON DELETE SET NULL,
    raport_id         INT REFERENCES raporty(id) ON DELETE SET NULL,
    przeglad_id       INT REFERENCES przeglady_techniczne(id) ON DELETE SET NULL,
    adres_email       VARCHAR(255) NOT NULL,
    temat             VARCHAR(255) NOT NULL,
    wiadomosc         TEXT,
    status            status_powiadomienia NOT NULL DEFAULT 'oczekujace',
    utworzono_w       TIMESTAMP NOT NULL DEFAULT NOW(),
    wyslano_w         TIMESTAMP,

    CONSTRAINT chk_powiadomienia_email_format
        CHECK (position('@' in adres_email) > 1),

    CONSTRAINT chk_powiadomienia_jedno_zrodlo
    CHECK (
        (raport_id IS NOT NULL AND przeglad_id IS NULL)   -- powiadomienie o raporcie
        OR
        (raport_id IS NULL AND przeglad_id IS NOT NULL)   -- przypomnienie o przeglądzie
        OR
        (raport_id IS NULL AND przeglad_id IS NULL)       -- ogólne powiadomienie
    ),

    CONSTRAINT chk_powiadomienia_data_wysylki
        CHECK (wyslano_w IS NULL OR wyslano_w >= utworzono_w)
);


CREATE INDEX idx_samochody_uzytkownik
    ON samochody(uzytkownik_id);

CREATE INDEX idx_wpisy_serwisowe_samochod
    ON wpisy_serwisowe(samochod_id);

CREATE INDEX idx_wpisy_serwisowe_data
    ON wpisy_serwisowe(data_serwisu);

CREATE INDEX idx_wpisy_serwisowe_warsztat
    ON wpisy_serwisowe(nazwa_warsztatu);

CREATE INDEX idx_zadania_serwisowe_wpis
    ON zadania_serwisowe(wpis_serwisowy_id);

CREATE INDEX idx_uzyte_czesci_wpis
    ON uzyte_czesci(wpis_serwisowy_id);

CREATE INDEX idx_uzyte_czesci_czesc
    ON uzyte_czesci(czesc_id);

CREATE INDEX idx_czesci_nazwa
    ON czesci(nazwa);

CREATE INDEX idx_przeglady_techniczne_samochod
    ON przeglady_techniczne(samochod_id);

CREATE INDEX idx_przeglady_techniczne_data_waznosci
    ON przeglady_techniczne(data_waznosci);

CREATE INDEX idx_powiadomienia_email_uzytkownik
    ON powiadomienia_email(uzytkownik_id);

CREATE INDEX idx_powiadomienia_email_status
    ON powiadomienia_email(status);

CREATE INDEX idx_zadania_serwisowe_nazwa
    ON zadania_serwisowe(nazwa_zadania);

-- WIDOK Z KOSZTEM CAŁKOWITYM SERWISU
-- nie ma kosztu jako redundantnej kolumny
CREATE VIEW v_wpisy_serwisowe_z_kosztem AS
SELECT
    ws.id,
    ws.samochod_id,
    ws.rodzaj_serwisu_id,
    ws.data_serwisu,
    ws.nazwa_warsztatu,
    ws.adres_warsztatu,
    ws.przebieg_przy_serwisie,
    ws.nastepny_serwis_przebieg,
    ws.nastepna_data_serwisu,
    ws.opis,
    ws.status,
    ws.utworzono_w,
    ws.zaktualizowano_w,
    COALESCE(z.robocizna_suma, 0) + COALESCE(c.czesc_suma, 0) AS calkowity_koszt
FROM wpisy_serwisowe ws
LEFT JOIN (
    SELECT
        wpis_serwisowy_id,
        SUM(koszt_robocizny) AS robocizna_suma
    FROM zadania_serwisowe
    GROUP BY wpis_serwisowy_id
) z ON ws.id = z.wpis_serwisowy_id
LEFT JOIN (
    SELECT
        wpis_serwisowy_id,
        SUM(ilosc * cena_jednostkowa) AS czesc_suma
    FROM uzyte_czesci
    GROUP BY wpis_serwisowy_id
) c ON ws.id = c.wpis_serwisowy_id;

-- przykładowe rodzaje serwisu
INSERT INTO rodzaje_serwisu (nazwa, opis) VALUES
('Wymiana oleju', 'Wymiana oleju silnikowego i filtra oleju'),
('Wymiana hamulcow', 'Serwis ukladu hamulcowego'),
('Przeglad okresowy', 'Okresowy przeglad eksploatacyjny'),
('Diagnostyka', 'Diagnostyka komputerowa'),
('Naprawa zawieszenia', 'Naprawa elementow zawieszenia');
