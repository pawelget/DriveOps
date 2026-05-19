function ReportCard({ report, onDownload, onSendEmail, onDelete }) {
  return (
    <div className="report-card">
      <div className="report-card__header">
        <div>
          <span className="report-card__number">{report.numer_raportu}</span>
          <h3 className="report-card__title">
            {report.samochod
              ? `${report.samochod.marka} ${report.samochod.model} (${report.samochod.numer_rejestracyjny})`
              : "Pojazd nieznany"}
          </h3>
          <div className="report-card__meta">
            {report.wpis_serwisowy?.rodzaj_serwisu_nazwa && (
              <span>{report.wpis_serwisowy.rodzaj_serwisu_nazwa}</span>
            )}
            {report.wpis_serwisowy?.nazwa_warsztatu && (
              <span>· {report.wpis_serwisowy.nazwa_warsztatu}</span>
            )}
            {report.wpis_serwisowy?.data_serwisu && (
              <span>· {report.wpis_serwisowy.data_serwisu}</span>
            )}
          </div>
        </div>
        <div className="report-card__date">
          {report.wygenerowano_w &&
            new Date(report.wygenerowano_w).toLocaleString("pl-PL")}
        </div>
      </div>

      {report.nazwy_czynnosci && report.nazwy_czynnosci.length > 0 && (
        <div className="report-card__tasks">
          Czynnosci: {report.nazwy_czynnosci.join(", ")}
        </div>
      )}

      {report.wpis_serwisowy && (
        <div className="report-card__cost">
          {report.wpis_serwisowy.koszt_calkowity.toFixed(2)} zl
        </div>
      )}

      <div className="report-card__actions">
        <button
          className="primary"
          onClick={() => onDownload(report)}
        >
          Pobierz PDF
        </button>
        <button
          className="secondary"
          onClick={() => onSendEmail(report)}
        >
          Wyslij email
        </button>
        <button
          className="danger"
          onClick={() => onDelete(report)}
        >
          Usun
        </button>
      </div>
    </div>
  );
}

export default ReportCard;
