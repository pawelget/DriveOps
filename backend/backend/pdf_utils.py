"""
Generowanie PDF-ow raportow serwisowych z uzyciem reportlab.

Modul rejestruje czcionki DejaVu Sans (z folderu ./fonts), dzieki czemu
PDF poprawnie wyswietla polskie znaki diakrytyczne. Jesli czcionki nie sa
dostepne, modul awaryjnie uzywa Helvetica (bez polskich znakow).
"""

import os
from datetime import datetime

from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import cm
from reportlab.lib import colors
from reportlab.platypus import (
    SimpleDocTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)
from reportlab.lib.enums import TA_CENTER, TA_RIGHT
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

from dotenv import load_dotenv

load_dotenv()


REPORTS_DIR = os.getenv("REPORTS_DIR", "./reports_storage")


# =========================
# REJESTRACJA CZCIONEK Z POLSKIMI ZNAKAMI
# =========================

# Folder z czcionkami obok tego pliku (backend/backend/fonts/)
_FONTS_DIR = os.path.join(os.path.dirname(__file__), "fonts")

# Nazwy czcionek uzywane w stylach
FONT_NORMAL = "DejaVuSans"
FONT_BOLD = "DejaVuSans-Bold"

try:
    pdfmetrics.registerFont(
        TTFont(FONT_NORMAL, os.path.join(_FONTS_DIR, "DejaVuSans.ttf"))
    )
    pdfmetrics.registerFont(
        TTFont(FONT_BOLD, os.path.join(_FONTS_DIR, "DejaVuSans-Bold.ttf"))
    )
    pdfmetrics.registerFontFamily(
        FONT_NORMAL,
        normal=FONT_NORMAL,
        bold=FONT_BOLD,
        italic=FONT_NORMAL,
        boldItalic=FONT_BOLD,
    )
    _FONTS_OK = True
except Exception as e:
    print(
        f"[pdf_utils] Ostrzezenie: nie udalo sie zaladowac czcionek "
        f"DejaVu: {e}"
    )
    print(
        "[pdf_utils] PDF bedzie uzywal Helvetica (bez polskich znakow). "
        "Skopiuj DejaVuSans.ttf i DejaVuSans-Bold.ttf do folderu fonts/."
    )
    FONT_NORMAL = "Helvetica"
    FONT_BOLD = "Helvetica-Bold"
    _FONTS_OK = False


# =========================
# HELPERY
# =========================

def generate_report_number(report_id: int, year: int) -> str:
    """Zwraca numer raportu w formacie R/2026/00001."""
    return f"R/{year}/{report_id:05d}"


def _build_styles():
    """Zwraca slownik stylow akapitow uzywanych w PDF."""
    base = getSampleStyleSheet()
    return {
        "title": ParagraphStyle(
            "Title",
            parent=base["Heading1"],
            fontName=FONT_BOLD,
            fontSize=20,
            textColor=colors.HexColor("#7c5cbf"),
            alignment=TA_CENTER,
            spaceAfter=12,
        ),
        "subtitle": ParagraphStyle(
            "Subtitle",
            parent=base["Normal"],
            fontName=FONT_NORMAL,
            fontSize=11,
            textColor=colors.grey,
            alignment=TA_CENTER,
            spaceAfter=20,
        ),
        "h2": ParagraphStyle(
            "H2",
            parent=base["Heading2"],
            fontName=FONT_BOLD,
            fontSize=13,
            textColor=colors.HexColor("#1e1b3a"),
            spaceAfter=8,
            spaceBefore=12,
        ),
        "body": ParagraphStyle(
            "Body",
            parent=base["Normal"],
            fontName=FONT_NORMAL,
            fontSize=10,
            leading=14,
        ),
        "label": ParagraphStyle(
            "Label",
            parent=base["Normal"],
            fontName=FONT_NORMAL,
            fontSize=9,
            textColor=colors.grey,
        ),
        "footer": ParagraphStyle(
            "Footer",
            parent=base["Normal"],
            fontName=FONT_NORMAL,
            fontSize=8,
            textColor=colors.grey,
            alignment=TA_CENTER,
        ),
        "total": ParagraphStyle(
            "Total",
            parent=base["Normal"],
            fontName=FONT_BOLD,
            fontSize=13,
            textColor=colors.HexColor("#7c5cbf"),
            alignment=TA_RIGHT,
        ),
    }


def _info_table(rows):
    """Buduje dwukolumnowa tabelke klucz-wartosc."""
    t = Table(rows, colWidths=[5 * cm, 11 * cm])
    t.setStyle(TableStyle([
        ("FONTNAME", (0, 0), (-1, -1), FONT_NORMAL),
        ("FONTSIZE", (0, 0), (-1, -1), 10),
        ("TEXTCOLOR", (0, 0), (0, -1), colors.grey),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
    ]))
    return t


def _data_table(header, rows, col_widths):
    """Buduje tabelke danych z naglowkiem i naprzemiennym tlem wierszy."""
    data = [header] + rows
    t = Table(data, colWidths=col_widths, repeatRows=1)
    t.setStyle(TableStyle([
        # Naglowek
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#1e1b3a")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), FONT_BOLD),
        ("FONTSIZE", (0, 0), (-1, 0), 10),
        ("ALIGN", (0, 0), (-1, 0), "LEFT"),
        ("BOTTOMPADDING", (0, 0), (-1, 0), 8),
        ("TOPPADDING", (0, 0), (-1, 0), 8),
        # Wiersze danych
        ("FONTNAME", (0, 1), (-1, -1), FONT_NORMAL),
        ("FONTSIZE", (0, 1), (-1, -1), 9),
        ("BOTTOMPADDING", (0, 1), (-1, -1), 6),
        ("TOPPADDING", (0, 1), (-1, -1), 6),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1),
         [colors.white, colors.HexColor("#f8f8fa")]),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("GRID", (0, 0), (-1, -1), 0.3, colors.HexColor("#cccccc")),
    ]))
    return t


# =========================
# GLOWNY GENERATOR
# =========================

def generate_service_report_pdf(report_data: dict, output_path: str) -> str:
    """
    Generuje PDF raportu serwisowego i zapisuje go pod output_path.

    Parametr report_data to slownik o strukturze:
    {
        "numer_raportu": "R/2026/00001",
        "wygenerowano_w": "2026-05-19 14:32",
        "uzytkownik": {"imie", "nazwisko", "email"},
        "samochod": {"marka", "model", "numer_rejestracyjny", "vin"},
        "wpis": {"data_serwisu", "nazwa_warsztatu", "adres_warsztatu",
                 "przebieg_przy_serwisie", "opis",
                 "rodzaj_serwisu_nazwa", "status"},
        "zadania": [{"nazwa", "opis", "koszt"}, ...],
        "czesci":  [{"nazwa", "producent", "ilosc",
                     "cena_jednostkowa", "suma"}, ...],
        "koszt_robocizny": float,
        "koszt_czesci": float,
        "koszt_calkowity": float
    }

    Zwraca sciezke do wygenerowanego pliku.
    """
    # Upewnij sie, ze katalog docelowy istnieje
    target_dir = os.path.dirname(output_path)
    if target_dir:
        os.makedirs(target_dir, exist_ok=True)

    doc = SimpleDocTemplate(
        output_path,
        pagesize=A4,
        leftMargin=2 * cm,
        rightMargin=2 * cm,
        topMargin=2 * cm,
        bottomMargin=2 * cm,
        title=f"Raport serwisowy {report_data.get('numer_raportu')}",
        author="DriveOps",
    )

    styles = _build_styles()
    story = []

    # ---- Naglowek ----
    story.append(Paragraph("DriveOps", styles["title"]))
    story.append(Paragraph(
        f"Raport serwisowy {report_data['numer_raportu']}",
        styles["subtitle"],
    ))

    # ---- Sekcja: pojazd ----
    story.append(Paragraph("Pojazd", styles["h2"]))

    samochod = report_data["samochod"]
    przebieg = report_data["wpis"].get("przebieg_przy_serwisie")
    przebieg_txt = (
        f"{przebieg:,} km".replace(",", " ")
        if przebieg
        else "-"
    )

    story.append(_info_table([
        ["Marka i model:", f"{samochod['marka']} {samochod['model']}"],
        ["Numer rejestracyjny:", samochod.get("numer_rejestracyjny") or "-"],
        ["VIN:", samochod.get("vin") or "-"],
        ["Przebieg przy serwisie:", przebieg_txt],
    ]))

    # ---- Sekcja: wlasciciel ----
    story.append(Paragraph("Wlasciciel", styles["h2"]))

    uz = report_data["uzytkownik"]
    story.append(_info_table([
        ["Imie i nazwisko:", f"{uz['imie']} {uz['nazwisko']}"],
        ["Email:", uz["email"]],
    ]))

    # ---- Sekcja: serwis ----
    story.append(Paragraph("Informacje o serwisie", styles["h2"]))

    wpis = report_data["wpis"]
    status_map = {
        "w_toku": "W toku",
        "zakonczony": "Zakonczony",
        "anulowany": "Anulowany",
    }

    story.append(_info_table([
        ["Data serwisu:", wpis.get("data_serwisu") or "-"],
        ["Rodzaj serwisu:", wpis.get("rodzaj_serwisu_nazwa") or "-"],
        ["Warsztat:", wpis.get("nazwa_warsztatu") or "-"],
        ["Adres warsztatu:", wpis.get("adres_warsztatu") or "-"],
        ["Status:",
         status_map.get(wpis.get("status"), wpis.get("status") or "-")],
        ["Opis:", wpis.get("opis") or "-"],
    ]))

    # ---- Sekcja: czynnosci ----
    story.append(Paragraph("Wykonane czynnosci", styles["h2"]))

    zadania = report_data.get("zadania") or []

    if zadania:
        rows = [
            [
                Paragraph(z.get("nazwa") or "-", styles["body"]),
                Paragraph(z.get("opis") or "-", styles["body"]),
                f"{z.get('koszt', 0):.2f} zl",
            ]
            for z in zadania
        ]
        story.append(_data_table(
            header=["Nazwa", "Opis", "Koszt"],
            rows=rows,
            col_widths=[5 * cm, 8 * cm, 3 * cm],
        ))
    else:
        story.append(Paragraph(
            "Brak wykonanych czynnosci.",
            styles["body"],
        ))

    # ---- Sekcja: czesci ----
    story.append(Paragraph("Uzyte czesci", styles["h2"]))

    czesci = report_data.get("czesci") or []

    if czesci:
        rows = [
            [
                Paragraph(c.get("nazwa") or "-", styles["body"]),
                Paragraph(c.get("producent") or "-", styles["body"]),
                f"{c.get('ilosc', 0):.2f}",
                f"{c.get('cena_jednostkowa', 0):.2f} zl",
                f"{c.get('suma', 0):.2f} zl",
            ]
            for c in czesci
        ]
        story.append(_data_table(
            header=["Nazwa", "Producent", "Ilosc", "Cena j.", "Suma"],
            rows=rows,
            col_widths=[5 * cm, 4 * cm, 2 * cm, 2.5 * cm, 2.5 * cm],
        ))
    else:
        story.append(Paragraph(
            "Brak uzytych czesci.",
            styles["body"],
        ))

    # ---- Podsumowanie kosztow ----
    story.append(Spacer(1, 18))

    podsumowanie = Table([
        ["Koszt robocizny:",
         f"{report_data['koszt_robocizny']:.2f} zl"],
        ["Koszt czesci:",
         f"{report_data['koszt_czesci']:.2f} zl"],
        ["RAZEM:",
         f"{report_data['koszt_calkowity']:.2f} zl"],
    ], colWidths=[12 * cm, 4 * cm])

    podsumowanie.setStyle(TableStyle([
        ("FONTNAME", (0, 0), (-1, -1), FONT_NORMAL),
        ("FONTSIZE", (0, 0), (-1, -1), 11),
        ("ALIGN", (0, 0), (0, -1), "RIGHT"),
        ("ALIGN", (1, 0), (1, -1), "RIGHT"),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("FONTNAME", (0, 2), (-1, 2), FONT_BOLD),
        ("FONTSIZE", (0, 2), (-1, 2), 13),
        ("TEXTCOLOR", (0, 2), (-1, 2), colors.HexColor("#7c5cbf")),
        ("LINEABOVE", (0, 2), (-1, 2), 1.5, colors.HexColor("#7c5cbf")),
    ]))
    story.append(podsumowanie)

    # ---- Stopka ----
    story.append(Spacer(1, 30))
    story.append(Paragraph(
        f"Raport {report_data['numer_raportu']} | "
        f"Wygenerowano: {report_data['wygenerowano_w']} | "
        f"DriveOps Vehicle Management System",
        styles["footer"],
    ))

    doc.build(story)
    return output_path
