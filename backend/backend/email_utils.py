"""
Wysylka email przez SMTP (Mailtrap w developmencie).
"""

import os
import smtplib
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.mime.application import MIMEApplication

from dotenv import load_dotenv

load_dotenv()


SMTP_HOST = os.getenv("SMTP_HOST", "sandbox.smtp.mailtrap.io")
SMTP_PORT = int(os.getenv("SMTP_PORT", "2525"))
SMTP_USER = os.getenv("SMTP_USER", "")
SMTP_PASSWORD = os.getenv("SMTP_PASSWORD", "")
SMTP_FROM = os.getenv("SMTP_FROM", "DriveOps <noreply@driveops.local>")


def send_report_email(
    to_email: str,
    report_number: str,
    pdf_path: str,
    car_info: str
) -> dict:
    """
    Wysyla email z zalaczonym PDF-em raportu.
    Zwraca slownik {'success': bool, 'message': str}.
    """
    if not SMTP_USER or not SMTP_PASSWORD:
        return {
            "success": False,
            "message": "SMTP nie jest skonfigurowany (brak SMTP_USER lub SMTP_PASSWORD)"
        }

    if not os.path.exists(pdf_path):
        return {
            "success": False,
            "message": f"Plik PDF nie istnieje: {pdf_path}"
        }

    try:
        msg = MIMEMultipart()
        msg["Subject"] = f"DriveOps - Raport serwisowy {report_number}"
        msg["From"] = SMTP_FROM
        msg["To"] = to_email

        body = f"""\
Dzien dobry,

w zalaczniku przesylamy raport serwisowy nr {report_number}
dla pojazdu: {car_info}.

Pozdrawiamy,
DriveOps Vehicle Management System
"""
        msg.attach(MIMEText(body, "plain", "utf-8"))

        # Dolacz PDF
        with open(pdf_path, "rb") as f:
            pdf_part = MIMEApplication(f.read(), _subtype="pdf")
            pdf_part.add_header(
                "Content-Disposition",
                "attachment",
                filename=f"raport_{report_number.replace('/', '_')}.pdf"
            )
            msg.attach(pdf_part)

        # Wyslij
        with smtplib.SMTP(SMTP_HOST, SMTP_PORT) as server:
            server.starttls()
            server.login(SMTP_USER, SMTP_PASSWORD)
            server.sendmail(SMTP_FROM, [to_email], msg.as_string())

        return {
            "success": True,
            "message": f"Email wyslany na {to_email}"
        }

    except Exception as e:
        return {
            "success": False,
            "message": f"Blad wysylki email: {str(e)}"
        }
