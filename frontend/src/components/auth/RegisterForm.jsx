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

  function handleChange(e) {
    setForm((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);

    try {
      const data = await registerUser(form);
      saveToken(data.token);
      setSuccess("Rejestracja zakonczona sukcesem");
      navigate("/profil");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="card">
      <h2>Rejestracja</h2>

      <MessageBox type="error" message={error} />
      <MessageBox type="success" message={success} />

      <Input
        label="Imie"
        name="imie"
        value={form.imie}
        onChange={handleChange}
        placeholder="Podaj imie"
      />
      <Input
        label="Nazwisko"
        name="nazwisko"
        value={form.nazwisko}
        onChange={handleChange}
        placeholder="Podaj nazwisko"
      />
      <Input
        label="Email"
        type="email"
        name="email"
        value={form.email}
        onChange={handleChange}
        placeholder="Podaj email"
      />
      <Input
        label="Haslo"
        type="password"
        name="haslo"
        value={form.haslo}
        onChange={handleChange}
        placeholder="Podaj haslo"
      />
      <Input
        label="Telefon"
        name="telefon"
        value={form.telefon}
        onChange={handleChange}
        placeholder="Podaj telefon"
      />

      <Button type="submit" disabled={loading}>
        {loading ? "Rejestrowanie..." : "Zarejestruj sie"}
      </Button>
    </form>
  );
}

export default RegisterForm;