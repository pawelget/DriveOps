import { useState } from "react";
import Input from "../ui/Input";
import Button from "../ui/Button";
import MessageBox from "../ui/MessageBox";
import { sendReportEmail } from "../../api/ReportsApi";

function EmailModal({ report, defaultEmail, onClose, onSent }) {
  const [email, setEmail] = useState(defaultEmail || "");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSend(e) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const customEmail = email.trim() !== defaultEmail ? email.trim() : null;
      const result = await sendReportEmail(report.id, customEmail);
      onSent(result.message);
    } catch (err) {
      setError(err.message);
      setLoading(false);
    }
  }

  return (
    <div className="car-modal__overlay" onClick={onClose}>
      <div className="car-modal" onClick={(e) => e.stopPropagation()}>
        <h2>Wyslij raport emailem</h2>
        <p style={{ color: "var(--text-secondary)" }}>
          Raport: <strong>{report.numer_raportu}</strong>
        </p>

        <form onSubmit={handleSend} className="auth-form">
          <MessageBox type="error" message={error} />

          <Input
            label="Adres email"
            type="email"
            name="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="adres@email.pl"
          />
          <div className="email-modal__hint">
            Domyslnie wysylamy na Twoj email konta.
          </div>

          <div className="car-modal__actions" style={{ marginTop: 16 }}>
            <Button
              type="button"
              className="auth-button car-modal__cancel"
              onClick={onClose}
            >
              Anuluj
            </Button>
            <Button
              type="submit"
              disabled={loading || !email.trim()}
              className="auth-button"
            >
              {loading ? <span className="button-loader"></span> : "Wyslij"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default EmailModal;
