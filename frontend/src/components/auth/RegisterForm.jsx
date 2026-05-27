import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Input from "../ui/Input";
import Button from "../ui/Button";
import MessageBox from "../ui/MessageBox";
import { registerUser } from "../../api/AuthApi";
import { saveToken } from "../../utils/token";

function RegisterForm() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    imie: "",
    nazwisko: "",
    email: "",
    haslo: "",
    telefon: "",
  });

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);
  const [confirmPassword, setConfirmPassword] = useState("");

  function handleChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  }

  async function handleSubmit(e) {
  e.preventDefault();
  setError("");
  setSuccess("");

  // WALIDACJA

  if (form.haslo.length < 8) {
    setError("Hasło musi mieć co najmniej 8 znaków");
    return;
  }

  if (form.haslo !== confirmPassword) {
    setError("Hasła nie są takie same");
    return;
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  if (!emailRegex.test(form.email)) {
    setError("Podaj poprawny adres email");
    return;
  }

  if (!/[A-Z]/.test(form.haslo)) {
    setError("Hasło musi zawierać dużą literę");
    return;
  }

  if (!/\d/.test(form.haslo)) {
    setError("Hasło musi zawierać cyfrę");
    return;
  }

  if (!/[!@#$%^&*]/.test(form.haslo)) {
    setError("Hasło musi zawierać znak specjalny");
    return;
  }

  setLoading(true);

  try {
    const data = await registerUser(form);
    saveToken(data.token);
    setSuccess("Rejestracja zakończona sukcesem");
    navigate("/profil");
  } catch (err) {
    setError(err.message);
  } finally {
    setLoading(false);
  }
}

  return (
    <div className="login-page-wrapper">
      <div className="auth-card">
        <div className="auth-card__header">
          <h1 className="auth-card__title">Rejestracja</h1>
          <p className="auth-card__subtitle">Utwórz nowe konto</p>
        </div>
        <form onSubmit={handleSubmit} className="auth-form">
          <MessageBox type="error" message={error} />
          <MessageBox type="success" message={success} />
          <div className="auth-form__row">
            <Input
              label="Imię"
              name="imie"
              value={form.imie}
              onChange={handleChange}
              placeholder="Podaj imię"
            />
            <Input
              label="Nazwisko"
              name="nazwisko"
              value={form.nazwisko}
              onChange={handleChange}
              placeholder="Podaj nazwisko"
            />
          </div>
          <Input
            label="Email"
            type="email"
            name="email"
            value={form.email}
            onChange={handleChange}
            placeholder="Podaj email"
          />
          <Input
            label="Hasło"
            type="password"
            name="haslo"
            value={form.haslo}
            onChange={handleChange}
            placeholder="Podaj hasło (min. 8 znaków, znak specjalny i cyfra"
          />
          <Input
            label="Powtórz hasło"
            type="password"
            placeholder="Powtórz hasło"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
          />
          <Input
            label="Telefon"
            name="telefon"
            value={form.telefon}
            onChange={handleChange}
            placeholder="Podaj telefon"
          />
          <Button
            type="submit"
            disabled={loading}
            className="auth-button"
          >
            {loading ? <span className="button-loader"></span> : "Zarejestruj się"}
          </Button>
          <p className="auth-card__link">
            Masz już konto? <a href="/logowanie">Zaloguj się</a>
          </p>
        </form>
      </div>
    </div>
  );
}

export default RegisterForm;