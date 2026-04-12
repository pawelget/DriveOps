import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Input from "../ui/Input";
import Button from "../ui/Button";
import MessageBox from "../ui/MessageBox";
import { loginUser } from "../../api/AuthApi";
import { saveToken } from "../../utils/token";

function LoginForm() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    email: "",
    haslo: "",
  });

  const [error, setError] = useState("");
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
    setLoading(true);

    try {
      const data = await loginUser(form);
      saveToken(data.token);
      navigate("/profil");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="card">
      <h2>Logowanie</h2>

      <MessageBox type="error" message={error} />

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

      <Button type="submit" disabled={loading}>
        {loading ? "Logowanie..." : "Zaloguj sie"}
      </Button>
    </form>
  );
}

export default LoginForm;