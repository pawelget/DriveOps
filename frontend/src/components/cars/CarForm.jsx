import { useState, useEffect } from "react";
import Input from "../ui/Input";
import Button from "../ui/Button";
import MessageBox from "../ui/MessageBox";
import { createCar, updateCar } from "../../api/CarsApi";

const EMPTY_FORM = {
  marka: "",
  model: "",
  numer_rejestracyjny: "",
  vin: "",
  rok_produkcji: "",
  pojemnosc_cm3: "",
  moc_km: "",
  paliwo: "",
  przebieg: "",
  kolor: "",
};

function CarForm({ car, onClose, onSaved }) {
  const isEdit = !!car;

  const [form, setForm] = useState(EMPTY_FORM);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (car) {
      setForm({
        marka: car.marka || "",
        model: car.model || "",
        numer_rejestracyjny: car.numer_rejestracyjny || "",
        vin: car.vin || "",
        rok_produkcji: car.rok_produkcji || "",
        pojemnosc_cm3: car.pojemnosc_cm3 || "",
        moc_km: car.moc_km || "",
        paliwo: car.paliwo || "",
        przebieg: car.przebieg ?? "",
        kolor: car.kolor || "",
      });
    }
  }, [car]);

  function handleChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");

    const vin = form.vin.trim();

    if (vin && vin.length !== 17) {
      setError("VIN musi mieć dokładnie 17 znaków");
      return;
    }

    setLoading(true);

    const payload = {
      ...form,
      rok_produkcji: form.rok_produkcji ? parseInt(form.rok_produkcji, 10) : null,
      pojemnosc_cm3: form.pojemnosc_cm3 ? parseInt(form.pojemnosc_cm3, 10) : null,
      moc_km: form.moc_km ? parseInt(form.moc_km, 10) : null,
      przebieg: form.przebieg ? parseInt(form.przebieg, 10) : 0,
      paliwo: form.paliwo || null,
      vin: form.vin || null,
      kolor: form.kolor || null,
    };

    try {
      if (isEdit) {
        await updateCar(car.id, payload);
      } else {
        await createCar(payload);
      }
      onSaved();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="car-modal__overlay" onClick={onClose}>
      <div className="car-modal" onClick={(e) => e.stopPropagation()}>
        <h2>{isEdit ? "Edytuj pojazd" : "Dodaj pojazd"}</h2>

        <form onSubmit={handleSubmit} className="auth-form">
          <MessageBox type="error" message={error} />

          <div className="car-modal__row">
            <Input
              label="Marka *"
              name="marka"
              value={form.marka}
              onChange={handleChange}
              placeholder="np. Volkswagen"
            />
            <Input
              label="Model *"
              name="model"
              value={form.model}
              onChange={handleChange}
              placeholder="np. Golf"
            />
          </div>

          <Input
            label="Numer rejestracyjny *"
            name="numer_rejestracyjny"
            value={form.numer_rejestracyjny}
            onChange={handleChange}
            placeholder="np. ZS12345"
          />

          <Input
            label="VIN"
            name="vin"
            value={form.vin}
            onChange={handleChange}
            placeholder="17 znakow"
          />

          <div className="car-modal__row">
            <Input
              label="Rok produkcji"
              type="number"
              name="rok_produkcji"
              value={form.rok_produkcji}
              onChange={handleChange}
              placeholder="2020"
            />
            <Input
              label="Przebieg (km)"
              type="number"
              name="przebieg"
              value={form.przebieg}
              onChange={handleChange}
              placeholder="50000"
            />
          </div>

          <div className="car-modal__row">
            <Input
              label="Pojemnosc (cm3)"
              type="number"
              name="pojemnosc_cm3"
              value={form.pojemnosc_cm3}
              onChange={handleChange}
              placeholder="1998"
            />
            <Input
              label="Moc (KM)"
              type="number"
              name="moc_km"
              value={form.moc_km}
              onChange={handleChange}
              placeholder="150"
            />
          </div>

          <div className="form-group">
            <label>Paliwo</label>
            <select
              name="paliwo"
              value={form.paliwo}
              onChange={handleChange}
              style={{
                width: "100%",
                padding: "10px 12px",
                background: "var(--bg-hover)",
                color: "var(--text-primary)",
                border: "1px solid var(--border)",
                borderRadius: "8px",
                fontSize: "14px",
              }}
            >
              <option value="">-- wybierz --</option>
              <option value="benzyna">Benzyna</option>
              <option value="diesel">Diesel</option>
              <option value="elektryczny">Elektryczny</option>
              <option value="hybryda">Hybryda</option>
              <option value="benzyna_gaz">Benzyna + LPG</option>
            </select>
          </div>

          <Input
            label="Kolor"
            name="kolor"
            value={form.kolor}
            onChange={handleChange}
            placeholder="np. czarny"
          />

          <div className="car-modal__actions">
            <Button
              type="button"
              className="auth-button car-modal__cancel"
              onClick={onClose}
            >
              Anuluj
            </Button>
            <Button
              type="submit"
              disabled={loading}
              className="auth-button"
            >
              {loading ? (
                <span className="button-loader"></span>
              ) : isEdit ? (
                "Zapisz"
              ) : (
                "Dodaj"
              )}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default CarForm;
