import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getCars } from "../../api/CarsApi";
import { getServices } from "../../api/ServicesApi";
import { getAlerts } from "../../api/AlertsApi";
import { getReports } from "../../api/ReportsApi";
import { getCurrentUser } from "../../api/AuthApi";
import "./Dashboard.css";

// Ikonki SVG
const Icon = ({ d }) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
    strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d={d} />
  </svg>
);

const ICONS = {
  car: "M5 17H3a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2h-2M7 17a2 2 0 1 0 4 0 2 2 0 0 0-4 0zM13 17a2 2 0 1 0 4 0 2 2 0 0 0-4 0z",
  wrench: "M14.7 6.3a4 4 0 0 0-5.6 5.6L3 18l3 3 6.1-6.1a4 4 0 0 0 5.6-5.6l-2.5 2.5-2.8-.7-.7-2.8 2.7-2.3z",
  alert: "M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0zM12 9v4M12 17h.01",
  report: "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8zM14 2v6h6M16 13H8M16 17H8",
  money: "M12 1v22M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6",
};

function StatCard({ icon, value, label }) {
  return (
    <div className="stat-card">
      <div className="stat-card__icon">
        <Icon d={ICONS[icon]} />
      </div>
      <div className="stat-card__body">
        <span className="stat-card__value">{value}</span>
        <span className="stat-card__label">{label}</span>
      </div>
    </div>
  );
}

function HomePage() {
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [userName, setUserName] = useState("");
  const [cars, setCars] = useState([]);
  const [services, setServices] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [reportsCount, setReportsCount] = useState(0);

  useEffect(() => {
    async function loadAll() {
      try {
        const [user, carsData, servicesData, alertsData, reportsData] =
          await Promise.all([
            getCurrentUser(),
            getCars(),
            getServices(),
            getAlerts(),
            getReports(),
          ]);

        setUserName(user.imie || "");
        setCars(carsData);
        setServices(servicesData);
        setAlerts(alertsData.alerts || []);
        setReportsCount(reportsData.length);
      } catch (err) {
        console.error("Blad ladowania dashboardu:", err);
      } finally {
        setLoading(false);
      }
    }
    loadAll();
  }, []);

  if (loading) {
    return (
      <div className="dashboard">
        <div className="dashboard__loading">Ladowanie pulpitu...</div>
      </div>
    );
  }

  // Statystyki liczone na froncie
  const aktywneSerwisy = services.filter(
    (s) => s.status === "w_toku"
  ).length;

  const today = new Date();
  const monthStart = new Date(today.getFullYear(), today.getMonth(), 1);
  const kosztMiesiac = services
    .filter((s) => s.data_serwisu && new Date(s.data_serwisu) >= monthStart)
    .reduce((sum, s) => sum + (s.koszt_calkowity || 0), 0);

  // Ostatnie 5 serwisow
  const ostatnieSerwisy = [...services]
    .sort((a, b) => {
      const da = a.data_serwisu || "";
      const db = b.data_serwisu || "";
      return db.localeCompare(da);
    })
    .slice(0, 5);

  // Top 5 alertow (juz posortowane po priorytecie przez backend)
  const topAlerty = alerts.slice(0, 5);

  return (
    <div className="dashboard">
      <h1>Witaj{userName ? `, ${userName}` : ""}!</h1>
      <p className="dashboard__subtitle">
        Oto przeglad Twojej floty pojazdow
      </p>

      {/* Kafle */}
      <div className="dashboard__stats">
        <StatCard icon="car" value={cars.length} label="Pojazdy" />
        <StatCard icon="wrench" value={aktywneSerwisy} label="Aktywne serwisy" />
        <StatCard icon="alert" value={alerts.length} label="Alerty" />
        <StatCard icon="report" value={reportsCount} label="Raporty" />
        <StatCard
          icon="money"
          value={`${kosztMiesiac.toFixed(0)} zl`}
          label="Koszty w tym miesiacu"
        />
      </div>

      {/* Dwie kolumny */}
      <div className="dashboard__columns">

        {/* Ostatnie serwisy */}
        <div className="dashboard__panel">
          <div className="dashboard__panel-header">
            <h2 className="dashboard__panel-title">Ostatnie serwisy</h2>
            <button
              className="dashboard__panel-link"
              onClick={() => navigate("/serwisy")}
            >
              Zobacz wszystkie
            </button>
          </div>

          {ostatnieSerwisy.length === 0 ? (
            <div className="dashboard__empty">Brak serwisow</div>
          ) : (
            ostatnieSerwisy.map((s) => (
              <div className="dashboard__row" key={s.id}>
                <div>
                  <div className="dashboard__row-main">
                    {s.samochod
                      ? `${s.samochod.marka} ${s.samochod.model}`
                      : "Pojazd"}
                  </div>
                  <div className="dashboard__row-sub">
                    {s.nazwa_warsztatu || "—"} · {s.data_serwisu || "—"}
                  </div>
                </div>
                <div className="dashboard__row-right">
                  <span className="dashboard__row-cost">
                    {(s.koszt_calkowity || 0).toFixed(2)} zl
                  </span>
                </div>
              </div>
            ))
          )}
        </div>

        {/* Alerty */}
        <div className="dashboard__panel">
          <div className="dashboard__panel-header">
            <h2 className="dashboard__panel-title">Najwazniejsze alerty</h2>
            <button
              className="dashboard__panel-link"
              onClick={() => navigate("/alerty")}
            >
              Zobacz wszystkie
            </button>
          </div>

          {topAlerty.length === 0 ? (
            <div className="dashboard__empty">Brak alertow. Wszystko gra!</div>
          ) : (
            topAlerty.map((a) => (
              <div className="dashboard__row" key={a.id}>
                <div>
                  <div className="dashboard__row-main">{a.tytul}</div>
                  <div className="dashboard__row-sub">{a.samochod_info}</div>
                </div>
                <div className="dashboard__row-right">
                  <span className={`dashboard__badge dashboard__badge--${a.priorytet}`}>
                    {a.priorytet}
                  </span>
                </div>
              </div>
            ))
          )}
        </div>

      </div>
    </div>
  );
}

export default HomePage;
