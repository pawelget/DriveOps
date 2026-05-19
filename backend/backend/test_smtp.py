import os
import smtplib
from email.mime.text import MIMEText
from dotenv import load_dotenv

load_dotenv()

HOST = os.getenv("SMTP_HOST")
PORT = int(os.getenv("SMTP_PORT", "2525"))
USER = os.getenv("SMTP_USER")
PASSWORD = os.getenv("SMTP_PASSWORD")

print(f"Probuje polaczyc z {HOST}:{PORT} jako {USER}")

msg = MIMEText("Test wiadomosci z DriveOps")
msg["Subject"] = "Test SMTP"
msg["From"] = "test@driveops.local"
msg["To"] = "odbiorca@example.com"

try:
    with smtplib.SMTP(HOST, PORT) as server:
        server.set_debuglevel(1)
        server.starttls()
        server.login(USER, PASSWORD)
        server.sendmail("test@driveops.local", ["odbiorca@example.com"], msg.as_string())
    print("\nSukces — sprawdz Mailtrap inbox")
except Exception as e:
    print(f"\nBlad: {type(e).__name__}: {e}")
