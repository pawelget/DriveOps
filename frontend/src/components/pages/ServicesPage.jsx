import { useEffect, useState } from "react";
import { getServices } from "../../api/ServicesApi";
import { generateReport } from "../../api/ReportsApi";
import ServiceCard from "../services/ServiceCard";
import ServiceForm from "../services/ServiceForm";
import MessageBox from "../ui/MessageBox";
import "../services/Services.css";

function ServicesPage() {
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [generating, setGenerating] = useState(null);

  async function loadServices() {
    setLoading(true);
    setError("");
    try {
      const data = await getServices();
      setServices(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadServices();
  }, []);

  function handleSaved() {
    setShowForm(false);
    setSuccess("Serwis dodany");
    setTimeout(() => setSuccess(""), 3000);
    loadServices();
  }

  async function handleGenerateReport(service) {
    setError("");
    setGenerating(service.id);
    try {
      const res = await generateReport(service.id);
      setSuccess(`Raport ${res.raport.numer_raportu} wygenerowany`);
      setTimeout(() => setSuccess(""), 4000);
      loadServices();
    } catch (err) {
      setError(err.message);
    } finally {
      setGenerating(null);
    }
  }

  return (
    <div>
      <div className="services-page__header">
        <h1>Serwisy</h1>
        <button
          className="auth-button"
          onClick={() => setShowForm(true)}
          style={{ maxWidth: 180, padding: "10px 18px" }}
        >
          + Dodaj serwis
        </button>
      </div>

      <MessageBox type="error" message={error} />
      <MessageBox type="success" message={success} />

      {loading ? (
        <div className="card">Ladowanie serwisow...</div>
      ) : services.length === 0 ? (
        <div className="card">
          Brak wpisow serwisowych. Dodaj pierwszy aby moc wygenerowac raport.
        </div>
      ) : (
        <div className="services-list">
          {services.map((s) => (
            <ServiceCard
              key={s.id}
              service={
                generating === s.id
                  ? { ...s, ma_raport: true } // optymistyczny update
                  : s
              }
              onGenerateReport={handleGenerateReport}
            />
          ))}
        </div>
      )}

      {showForm && (
        <ServiceForm
          onClose={() => setShowForm(false)}
          onSaved={handleSaved}
        />
      )}
    </div>
  );
}

export default ServicesPage;
