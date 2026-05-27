import { useState } from "react";
import Input from "../ui/Input";
import Button from "../ui/Button";
import MessageBox from "../ui/MessageBox";
import { changePassword } from "../../api/UserApi";

function PasswordForm() {
  const [form, setForm] = useState({
    obecne_haslo: "",
    nowe_haslo: "",
    nowe_haslo_powtorz: "",
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  function handleChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setSuccess("");

  if (form.nowe_haslo !== form.nowe_haslo_powtorz) {
    setError("Nowe hasla nie sa identyczne");
    return;
}

  if (form.nowe_haslo.length < 8) {
    setError("Nowe haslo musi miec co najmniej 8 znakow");
    return;
  }

  if (!/[A-Z]/.test(form.nowe_haslo)) {
    setError("Haslo musi zawierac duza litere");
    return;
  }

  if (!/\d/.test(form.nowe_haslo)) {
    setError("Haslo musi zawierac cyfre");
    return;
  }

  if (!/[!@#$%^&*]/.test(form.nowe_haslo)) {
    setError("Haslo musi zawierac znak specjalny");
    return;
  }

    setLoading(true);
    try {
      await changePassword({
        obecne_haslo: form.obecne_haslo,
        nowe_haslo: form.nowe_haslo,
      });
      setSuccess("Haslo zostalo zmienione");
      setForm({ obecne_haslo: "", nowe_haslo: "", nowe_haslo_powtorz: "" });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="card">
      <h2>Zmiana hasla</h2>
      <form onSubmit={handleSubmit} className="auth-form">
        <MessageBox type="error" message={error} />
        <MessageBox type="success" message={success} />
        <Input
          label="Obecne haslo"
          type="password"
          name="obecne_haslo"
          value={form.obecne_haslo}
          onChange={handleChange}
          placeholder="Obecne haslo"
        />
        <Input
          label="Nowe haslo"
          type="password"
          name="nowe_haslo"
          value={form.nowe_haslo}
          onChange={handleChange}
          placeholder="Nowe haslo (min. 8 znakow, znak specjalny oraz cyfra)"
        />
        <Input
          label="Powtorz nowe haslo"
          type="password"
          name="nowe_haslo_powtorz"
          value={form.nowe_haslo_powtorz}
          onChange={handleChange}
          placeholder="Powtorz nowe haslo"
        />
        <Button
          type="submit"
          disabled={loading}
          className="auth-button"
        >
          {loading ? <span className="button-loader"></span> : "Zmien haslo"}
        </Button>
      </form>
    </div>
  );
}

export default PasswordForm;
