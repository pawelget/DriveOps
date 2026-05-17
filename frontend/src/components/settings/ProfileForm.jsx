import { useEffect, useState } from "react";
import Input from "../ui/Input";
import Button from "../ui/Button";
import MessageBox from "../ui/MessageBox";
import { getCurrentUser } from "../../api/AuthApi";
import { updateProfile } from "../../api/UserApi";

function ProfileForm() {
  const [form, setForm] = useState({
    imie: "",
    nazwisko: "",
    email: "",
    telefon: "",
  });

  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    async function loadUser() {
      try {
        const data = await getCurrentUser();
        setForm({
          imie: data.imie || "",
          nazwisko: data.nazwisko || "",
          email: data.email || "",
          telefon: data.telefon || "",
        });
      } catch (err) {
        setError(err.message);
      } finally {
        setFetching(false);
      }
    }
    loadUser();
  }, []);

  function handleChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);
    try {
      await updateProfile(form);
      setSuccess("Dane zostaly zaktualizowane");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  if (fetching) {
    return <div className="card">Ladowanie danych...</div>;
  }

  return (
    <div className="card">
      <h2>Dane profilu</h2>
      <form onSubmit={handleSubmit} className="auth-form">
        <MessageBox type="error" message={error} />
        <MessageBox type="success" message={success} />
        <div className="auth-form__row">
          <Input
            label="Imie"
            name="imie"
            value={form.imie}
            onChange={handleChange}
            placeholder="Imie"
          />
          <Input
            label="Nazwisko"
            name="nazwisko"
            value={form.nazwisko}
            onChange={handleChange}
            placeholder="Nazwisko"
          />
        </div>
        <Input
          label="Email"
          type="email"
          name="email"
          value={form.email}
          onChange={handleChange}
          placeholder="Email"
        />
        <Input
          label="Telefon"
          name="telefon"
          value={form.telefon}
          onChange={handleChange}
          placeholder="Telefon"
        />
        <Button
          type="submit"
          disabled={loading}
          className="auth-button"
        >
          {loading ? <span className="button-loader"></span> : "Zapisz zmiany"}
        </Button>
      </form>
    </div>
  );
}

export default ProfileForm;
