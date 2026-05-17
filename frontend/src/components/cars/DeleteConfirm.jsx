import { useState } from "react";
import Button from "../ui/Button";
import MessageBox from "../ui/MessageBox";
import { deleteCar } from "../../api/CarsApi";

function DeleteConfirm({ car, onClose, onDeleted }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleDelete() {
    setError("");
    setLoading(true);
    try {
      await deleteCar(car.id);
      onDeleted();
    } catch (err) {
      setError(err.message);
      setLoading(false);
    }
  }

  return (
    <div className="car-modal__overlay" onClick={onClose}>
      <div className="car-modal" onClick={(e) => e.stopPropagation()}>
        <h2>Usun pojazd</h2>
        <p style={{ color: "var(--text-secondary)" }}>
          Czy na pewno chcesz usunac pojazd{" "}
          <strong>
            {car.marka} {car.model} ({car.numer_rejestracyjny})
          </strong>
          ? Wszystkie powiazane serwisy i przeglady zostana usuniete.
        </p>

        <MessageBox type="error" message={error} />

        <div className="car-modal__actions">
          <Button type="button" className="auth-button car-modal__cancel">
            <span onClick={onClose}>Anuluj</span>
          </Button>
          <Button
            type="button"
            disabled={loading}
            className="auth-button"
          >
            <span
              onClick={handleDelete}
              style={{ color: loading ? undefined : "#fff" }}
            >
              {loading ? (
                <span className="button-loader"></span>
              ) : (
                "Usun"
              )}
            </span>
          </Button>
        </div>
      </div>
    </div>
  );
}

export default DeleteConfirm;
