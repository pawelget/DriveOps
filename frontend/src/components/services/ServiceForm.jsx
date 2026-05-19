import { useEffect, useState } from "react";
import Input from "../ui/Input";
import Button from "../ui/Button";
import MessageBox from "../ui/MessageBox";
import { createService, getServiceTypes } from "../../api/ServicesApi";
import { getCars } from "../../api/CarsApi";

const EMPTY_TASK = { nazwa_zadania: "", opis: "", koszt_robocizny: "" };
const EMPTY_PART = {
  nazwa_czesci: "",
  producent_czesci: "",
  ilosc: "1",
  cena_jednostkowa: "",
};

function ServiceForm({ onClose, onSaved }) {
  const [form, setForm] = useState({
    samochod_id: "",
    rodzaj_serwisu_id: "",
    data_serwisu: new Date().toISOString().slice(0, 10),
    nazwa_warsztatu: "",
    adres_warsztatu: "",
    przebieg_przy_serwisie: "",
    opis: "",
    status: "zakonczony",
  });

  const [tasks, setTasks] = useState([{ ...EMPTY_TASK }]);
  const [parts, setParts] = useState([]);
  const [cars, setCars] = useState([]);
  const [types, setTypes] = useState([]);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadData() {
      try {
        const [carsData, typesData] = await Promise.all([
          getCars(),
          getServiceTypes(),
        ]);
        setCars(carsData);
        setTypes(typesData);
      } catch (err) {
        setError(err.message);
      }
    }
    loadData();
  }, []);

  function handleFormChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  }

  function updateTask(index, field, value) {
    setTasks((prev) =>
      prev.map((t, i) => (i === index ? { ...t, [field]: value } : t))
    );
  }

  function addTask() {
    setTasks((prev) => [...prev, { ...EMPTY_TASK }]);
  }

  function removeTask(index) {
    setTasks((prev) => prev.filter((_, i) => i !== index));
  }

  function updatePart(index, field, value) {
    setParts((prev) =>
      prev.map((p, i) => (i === index ? { ...p, [field]: value } : p))
    );
  }

  function addPart() {
    setParts((prev) => [...prev, { ...EMPTY_PART }]);
  }

  function removePart(index) {
    setParts((prev) => prev.filter((_, i) => i !== index));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");

    if (!form.samochod_id) {
      setError("Wybierz pojazd");
      return;
    }

    setLoading(true);

    const payload = {
      samochod_id: parseInt(form.samochod_id, 10),
      rodzaj_serwisu_id: form.rodzaj_serwisu_id
        ? parseInt(form.rodzaj_serwisu_id, 10)
        : null,
      data_serwisu: form.data_serwisu,
      nazwa_warsztatu: form.nazwa_warsztatu || null,
      adres_warsztatu: form.adres_warsztatu || null,
      przebieg_przy_serwisie: form.przebieg_przy_serwisie
        ? parseInt(form.przebieg_przy_serwisie, 10)
        : null,
      opis: form.opis || null,
      status: form.status,
      zadania: tasks
        .filter((t) => t.nazwa_zadania.trim())
        .map((t) => ({
          nazwa_zadania: t.nazwa_zadania.trim(),
          opis: t.opis || null,
          koszt_robocizny: t.koszt_robocizny
            ? parseFloat(t.koszt_robocizny)
            : 0,
        })),
      uzyte_czesci: parts
        .filter((p) => p.nazwa_czesci.trim())
        .map((p) => ({
          nazwa_czesci: p.nazwa_czesci.trim(),
          producent_czesci: p.producent_czesci || null,
          ilosc: p.ilosc ? parseFloat(p.ilosc) : 1,
          cena_jednostkowa: p.cena_jednostkowa
            ? parseFloat(p.cena_jednostkowa)
            : 0,
        })),
    };

    try {
      await createService(payload);
      onSaved();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="car-modal__overlay" onClick={onClose}>
      <div
        className="car-modal service-modal"
        onClick={(e) => e.stopPropagation()}
      >
        <h2>Dodaj wpis serwisowy</h2>

        <form onSubmit={handleSubmit} className="auth-form">
          <MessageBox type="error" message={error} />

          <div className="form-group">
            <label>Pojazd *</label>
            <select
              className="service-modal__select"
              name="samochod_id"
              value={form.samochod_id}
              onChange={handleFormChange}
            >
              <option value="">-- wybierz pojazd --</option>
              {cars.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.marka} {c.model} ({c.numer_rejestracyjny})
                </option>
              ))}
            </select>
          </div>

          <div className="car-modal__row">
            <div className="form-group" style={{ flex: 1 }}>
              <label>Rodzaj serwisu</label>
              <select
                className="service-modal__select"
                name="rodzaj_serwisu_id"
                value={form.rodzaj_serwisu_id}
                onChange={handleFormChange}
              >
                <option value="">-- wybierz --</option>
                {types.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.nazwa}
                  </option>
                ))}
              </select>
            </div>

            <Input
              label="Data serwisu *"
              type="date"
              name="data_serwisu"
              value={form.data_serwisu}
              onChange={handleFormChange}
            />
          </div>

          <div className="car-modal__row">
            <Input
              label="Warsztat"
              name="nazwa_warsztatu"
              value={form.nazwa_warsztatu}
              onChange={handleFormChange}
              placeholder="Auto-Serwis"
            />
            <Input
              label="Przebieg"
              type="number"
              name="przebieg_przy_serwisie"
              value={form.przebieg_przy_serwisie}
              onChange={handleFormChange}
              placeholder="125000"
            />
          </div>

          <Input
            label="Adres warsztatu"
            name="adres_warsztatu"
            value={form.adres_warsztatu}
            onChange={handleFormChange}
            placeholder="Szczecin, ul. ..."
          />

          <div className="form-group">
            <label>Opis</label>
            <textarea
              className="service-modal__textarea"
              name="opis"
              value={form.opis}
              onChange={handleFormChange}
              placeholder="Krotki opis serwisu"
            />
          </div>

          <div className="form-group">
            <label>Status</label>
            <select
              className="service-modal__select"
              name="status"
              value={form.status}
              onChange={handleFormChange}
            >
              <option value="zakonczony">Zakonczony</option>
              <option value="w_toku">W toku</option>
              <option value="anulowany">Anulowany</option>
            </select>
          </div>

          {/* CZYNNOSCI */}
          <div className="service-modal__section">
            <h3>Czynnosci</h3>
            {tasks.map((task, idx) => (
              <div className="service-modal__row" key={idx}>
                <Input
                  label={idx === 0 ? "Nazwa" : ""}
                  value={task.nazwa_zadania}
                  onChange={(e) =>
                    updateTask(idx, "nazwa_zadania", e.target.value)
                  }
                  placeholder="np. Wymiana oleju"
                />
                <Input
                  label={idx === 0 ? "Opis" : ""}
                  value={task.opis}
                  onChange={(e) => updateTask(idx, "opis", e.target.value)}
                  placeholder="Opis"
                />
                <Input
                  label={idx === 0 ? "Koszt (zl)" : ""}
                  type="number"
                  value={task.koszt_robocizny}
                  onChange={(e) =>
                    updateTask(idx, "koszt_robocizny", e.target.value)
                  }
                  placeholder="0"
                />
                {tasks.length > 1 && (
                  <button
                    type="button"
                    className="remove"
                    onClick={() => removeTask(idx)}
                  >
                    ×
                  </button>
                )}
              </div>
            ))}
            <button
              type="button"
              className="service-modal__add-btn"
              onClick={addTask}
            >
              + Dodaj czynnosc
            </button>
          </div>

          {/* CZESCI */}
          <div className="service-modal__section">
            <h3>Uzyte czesci</h3>
            {parts.length === 0 && (
              <div
                style={{
                  color: "var(--text-muted)",
                  fontSize: 13,
                  marginBottom: 8,
                }}
              >
                Brak czesci. Dodaj jesli sa potrzebne.
              </div>
            )}
            {parts.map((part, idx) => (
              <div className="service-modal__row" key={idx}>
                <Input
                  label={idx === 0 ? "Nazwa" : ""}
                  value={part.nazwa_czesci}
                  onChange={(e) =>
                    updatePart(idx, "nazwa_czesci", e.target.value)
                  }
                  placeholder="np. Olej 5W30"
                />
                <Input
                  label={idx === 0 ? "Producent" : ""}
                  value={part.producent_czesci}
                  onChange={(e) =>
                    updatePart(idx, "producent_czesci", e.target.value)
                  }
                  placeholder="Mobil"
                />
                <Input
                  label={idx === 0 ? "Ilosc" : ""}
                  type="number"
                  value={part.ilosc}
                  onChange={(e) => updatePart(idx, "ilosc", e.target.value)}
                  placeholder="1"
                />
                <Input
                  label={idx === 0 ? "Cena (zl)" : ""}
                  type="number"
                  value={part.cena_jednostkowa}
                  onChange={(e) =>
                    updatePart(idx, "cena_jednostkowa", e.target.value)
                  }
                  placeholder="0"
                />
                <button
                  type="button"
                  className="remove"
                  onClick={() => removePart(idx)}
                >
                  ×
                </button>
              </div>
            ))}
            <button
              type="button"
              className="service-modal__add-btn"
              onClick={addPart}
            >
              + Dodaj czesc
            </button>
          </div>

          <div className="car-modal__actions" style={{ marginTop: 20 }}>
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
              ) : (
                "Zapisz serwis"
              )}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default ServiceForm;
