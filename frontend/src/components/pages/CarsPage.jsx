import { useEffect, useState } from "react";
import { getCars } from "../../api/CarsApi";
import CarCard from "../cars/CarCard";
import CarForm from "../cars/CarForm";
import DeleteConfirm from "../cars/DeleteConfirm";
import MessageBox from "../ui/MessageBox";
import "../cars/Cars.css";

function CarsPage() {
  const [cars, setCars] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");

  const [showForm, setShowForm] = useState(false);
  const [editingCar, setEditingCar] = useState(null);
  const [deletingCar, setDeletingCar] = useState(null);

  async function loadCars() {
    setLoading(true);
    setError("");
    try {
      const data = await getCars();
      setCars(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadCars();
  }, []);

  const filtered = cars.filter((car) => {
    if (!search) return true;
    const q = search.toLowerCase();
    return (
      car.marka?.toLowerCase().includes(q) ||
      car.model?.toLowerCase().includes(q) ||
      car.numer_rejestracyjny?.toLowerCase().includes(q) ||
      car.vin?.toLowerCase().includes(q)
    );
  });

  function handleAdd() {
    setEditingCar(null);
    setShowForm(true);
  }

  function handleEdit(car) {
    setEditingCar(car);
    setShowForm(true);
  }

  function handleDelete(car) {
    setDeletingCar(car);
  }

  function handleSaved() {
    setShowForm(false);
    setEditingCar(null);
    loadCars();
  }

  function handleDeleted() {
    setDeletingCar(null);
    loadCars();
  }

  return (
    <div>
      <div className="cars-page__header">
        <h1>Pojazdy</h1>
        <button
          className="auth-button"
          onClick={handleAdd}
          style={{ maxWidth: "180px", padding: "10px 18px" }}
        >
          + Dodaj pojazd
        </button>
      </div>

      <div className="cars-search">
        <input
          type="text"
          placeholder="Szukaj po marce, modelu, nr rej. lub VIN..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      <MessageBox type="error" message={error} />

      {loading ? (
        <div className="card">Ladowanie pojazdow...</div>
      ) : filtered.length === 0 ? (
        <div className="cars-empty">
          {cars.length === 0
            ? "Nie masz jeszcze zadnych pojazdow. Dodaj pierwszy!"
            : "Brak pojazdow pasujacych do wyszukiwania."}
        </div>
      ) : (
        <div className="cars-grid">
          {filtered.map((car) => (
            <CarCard
              key={car.id}
              car={car}
              onEdit={handleEdit}
              onDelete={handleDelete}
            />
          ))}
        </div>
      )}

      {showForm && (
        <CarForm
          car={editingCar}
          onClose={() => {
            setShowForm(false);
            setEditingCar(null);
          }}
          onSaved={handleSaved}
        />
      )}

      {deletingCar && (
        <DeleteConfirm
          car={deletingCar}
          onClose={() => setDeletingCar(null)}
          onDeleted={handleDeleted}
        />
      )}
    </div>
  );
}

export default CarsPage;
