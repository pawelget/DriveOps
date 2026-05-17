import { useEffect, useState } from "react";
import { getAlerts } from "../../api/AlertsApi";
import AlertCard from "../alerts/AlertCard";
import MessageBox from "../ui/MessageBox";
import "../alerts/Alerts.css";

function AlertsPage() {
  const [alerts, setAlerts] = useState([]);
  const [stats, setStats] = useState({ critical: 0, warning: 0, info: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [filterTyp, setFilterTyp] = useState("");
  const [filterPriorytet, setFilterPriorytet] = useState("");

  async function loadAlerts() {
    setLoading(true);
    setError("");
    try {
      const data = await getAlerts({
        typ: filterTyp,
        priorytet: filterPriorytet,
      });
      setAlerts(data.alerts);
      setStats(data.by_priority);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAlerts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filterTyp, filterPriorytet]);

  return (
    <div>
      <h1>Alerty</h1>

      <div className="alerts-stats">
        <div className="alerts-stat alerts-stat--critical">
          <span className="alerts-stat__count">{stats.critical}</span>
          <span className="alerts-stat__label">Krytyczne</span>
        </div>
        <div className="alerts-stat alerts-stat--warning">
          <span className="alerts-stat__count">{stats.warning}</span>
          <span className="alerts-stat__label">Ostrzezenia</span>
        </div>
        <div className="alerts-stat alerts-stat--info">
          <span className="alerts-stat__count">{stats.info}</span>
          <span className="alerts-stat__label">Info</span>
        </div>
      </div>

      <div className="alerts-filters">
        <select
          value={filterTyp}
          onChange={(e) => setFilterTyp(e.target.value)}
        >
          <option value="">Wszystkie typy</option>
          <option value="przeglad">Przeglady</option>
          <option value="serwis">Serwisy</option>
        </select>

        <select
          value={filterPriorytet}
          onChange={(e) => setFilterPriorytet(e.target.value)}
        >
          <option value="">Wszystkie priorytety</option>
          <option value="critical">Krytyczne</option>
          <option value="warning">Ostrzezenia</option>
          <option value="info">Info</option>
        </select>
      </div>

      <MessageBox type="error" message={error} />

      {loading ? (
        <div className="card">Ladowanie alertow...</div>
      ) : alerts.length === 0 ? (
        <div className="card">Brak alertow. Wszystko gra! 🚗</div>
      ) : (
        <div className="alerts-list">
          {alerts.map((alert) => (
            <AlertCard key={alert.id} alert={alert} />
          ))}
        </div>
      )}
    </div>
  );
}

export default AlertsPage;
