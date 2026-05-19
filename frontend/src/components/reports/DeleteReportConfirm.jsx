import { useState } from "react";
import Button from "../ui/Button";
import MessageBox from "../ui/MessageBox";
import { deleteReport } from "../../api/ReportsApi";

function DeleteReportConfirm({ report, onClose, onDeleted }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleDelete() {
    setError("");
    setLoading(true);
    try {
      await deleteReport(report.id);
      onDeleted();
    } catch (err) {
      setError(err.message);
      setLoading(false);
    }
  }

  return (
    <div className="car-modal__overlay" onClick={onClose}>
      <div className="car-modal" onClick={(e) => e.stopPropagation()}>
        <h2>Usun raport</h2>
        <p style={{ color: "var(--text-secondary)" }}>
          Czy na pewno chcesz usunac raport{" "}
          <strong>{report.numer_raportu}</strong>? Plik PDF zostanie usuniety,
          ale wpis serwisowy pozostanie w bazie.
        </p>

        <MessageBox type="error" message={error} />

        <div className="car-modal__actions">
          <Button
            type="button"
            className="auth-button car-modal__cancel"
            onClick={onClose}
          >
            Anuluj
          </Button>
          <Button
            type="button"
            disabled={loading}
            className="auth-button"
            onClick={handleDelete}
          >
            {loading ? <span className="button-loader"></span> : "Usun"}
          </Button>
        </div>
      </div>
    </div>
  );
}

export default DeleteReportConfirm;
