import sys
import os
# Wymuszenie poprawnej ścieżki dla importów
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from dotenv import load_dotenv

from app import app as flask_app
from db import Base

load_dotenv()

# Pobieramy dane z Twojego pliku .env
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASSWORD = os.getenv("DB_PASSWORD", "admin")
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
# Wymuszamy dedykowaną bazę testową
TEST_DB_NAME = "CarManagementDB_test"

TEST_DATABASE_URL = f"postgresql+psycopg2://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{TEST_DB_NAME}"

@pytest.fixture(scope="session")
def test_engine():
    engine = create_engine(TEST_DATABASE_URL)
    yield engine
    engine.dispose()

@pytest.fixture(scope="function", autouse=True)
def clean_db(test_engine):
    """Zrzuca i tworzy tabele od nowa przed KAZDYM testem."""
    Base.metadata.drop_all(bind=test_engine)
    Base.metadata.create_all(bind=test_engine)
    yield
    Base.metadata.drop_all(bind=test_engine)

@pytest.fixture(scope="function")
def db_session(test_engine, monkeypatch):
    """Tworzy odizolowaną sesję i transakcję dla każdego testu."""
    TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=test_engine)
    session = TestingSessionLocal()

    # Podmieniamy oryginalną sesję z app.py na naszą testową pod Postgresa
    monkeypatch.setattr("app.SessionLocal", lambda: session)

    yield session

    # Po każdym teście cofamy zmiany (rollback), dzięki czemu testy nie brudzą sobie nawzajem
    session.rollback()
    session.close()

@pytest.fixture
def client(db_session):
    flask_app.config["TESTING"] = True
    with flask_app.test_client() as client:
        yield client