import { useEffect, useState, useCallback } from "react";
import { getReports, downloadReportPdf } from "../../api/ReportsApi";
import { getCars } from "../../api/CarsApi";
import { getCurrentUser } from "../../api/AuthApi";
import ReportCard from "../reports/ReportCard";
import ReportFilters from "../reports/ReportFilters";
import EmailModal from "../reports/EmailModal";
import DeleteReportConfirm from "../reports/DeleteReportConfirm";
import MessageBox from "../ui/MessageBox";
import "../reports/Reports.css";

const EMPTY_FILTERS = {
  search: "",
  date_from: "",
  date_to: "",
  warsztat: "",
  cost_min: "",
  cost_max: "",
  samochod_id: "",
};

function ReportsPage() {
  const [reports, setReports] = useState([]);
  const [cars, setCars] = useState([]);
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [filters, setFilters] = useState(EMPTY_FILTERS);

  const [emailingReport, setEmailingReport] = useState(null);
  const [deletingReport, setDeletingReport] = useState(null);

  const loadReports = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const data = await getReports(filters);
      setReports(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    async function loadInitial() {
      try {
        const [carsData, userData] = await Promise.all([
          getCars(),
          getCurrentUser(),
        ]);
        setCars(carsData);
        setUser(userData);
      } catch (err) {
        setError(err.message);
      }
    }
    loadInitial();
  }, []);

  useEffect(() => {
    const timer = setTimeout(loadReports, 300); // debounce
    return () => clearTimeout(timer);
  }, [loadReports]);

  async function handleDownload(report) {
    setError("");
    try {
      await downloadReportPdf(report.id, report.numer_raportu);
    } catch (err) {
      setError(err.message);
    }
  }

  function showSuccess(msg) {
    setSuccess(msg);
    setTimeout(() => setSuccess(""), 4000);
  }

  return (
    <div>
      <h1>Raporty</h1>

      <ReportFilters
        filters={filters}
        onChange={setFilters}
        onClear={() => setFilters(EMPTY_FILTERS)}
        cars={cars}
      />

      <MessageBox type="error" message={error} />
      <MessageBox type="success" message={success} />

      {loading ? (
        <div className="card">Ladowanie raportow...</div>
      ) : reports.length === 0 ? (
        <div className="card">
          Brak raportow. Wygeneruj raport ze strony "Serwisy".
        </div>
      ) : (
        <div className="reports-list">
          {reports.map((r) => (
            <ReportCard
              key={r.id}
              report={r}
              onDownload={handleDownload}
              onSendEmail={setEmailingReport}
              onDelete={setDeletingReport}
            />
          ))}
        </div>
      )}

      {emailingReport && (
        <EmailModal
          report={emailingReport}
          defaultEmail={user?.email}
          onClose={() => setEmailingReport(null)}
          onSent={(msg) => {
            setEmailingReport(null);
            showSuccess(msg);
          }}
        />
      )}

      {deletingReport && (
        <DeleteReportConfirm
          report={deletingReport}
          onClose={() => setDeletingReport(null)}
          onDeleted={() => {
            setDeletingReport(null);
            showSuccess("Raport usuniety");
            loadReports();
          }}
        />
      )}
    </div>
  );
}

export default ReportsPage;
