function ServiceCard({ service, onGenerateReport }) {
  const statusLabels = {
    w_toku: "W toku",
    zakonczony: "Zakonczony",
    anulowany: "Anulowany",
  };

  return (
    <div className="service-card">
      <div className="service-card__header">
        <div>
          <h3 className="service-card__title">
            {service.samochod
              ? `${service.samochod.marka} ${service.samochod.model} (${service.samochod.numer_rejestracyjny})`
              : "Pojazd nieznany"}
          </h3>
          <div className="service-card__meta">
            {service.rodzaj_serwisu && (
              <span className="service-card__meta-item">
                <strong>{service.rodzaj_serwisu.nazwa}</strong>
              </span>
            )}
            {service.nazwa_warsztatu && (
              <span className="service-card__meta-item">
                Warsztat: {service.nazwa_warsztatu}
              </span>
            )}
            <span
              className={`service-card__status service-card__status--${service.status}`}
            >
              {statusLabels[service.status] || service.status}
            </span>
          </div>
        </div>
        <div className="service-card__date">{service.data_serwisu}</div>
      </div>

      {service.opis && (
        <div className="service-card__details">{service.opis}</div>
      )}

      {service.zadania && service.zadania.length > 0 && (
        <div className="service-card__details">
          <strong>Czynnosci:</strong>
          <ul className="service-card__sublist">
            {service.zadania.map((z) => (
              <li key={z.id}>
                {z.nazwa_zadania} — {z.koszt_robocizny.toFixed(2)} zl
              </li>
            ))}
          </ul>
        </div>
      )}

      {service.uzyte_czesci && service.uzyte_czesci.length > 0 && (
        <div className="service-card__details">
          <strong>Czesci:</strong>
          <ul className="service-card__sublist">
            {service.uzyte_czesci.map((uc) => (
              <li key={uc.id}>
                {uc.czesc?.nazwa || "-"} × {uc.ilosc} ={" "}
                {uc.suma.toFixed(2)} zl
              </li>
            ))}
          </ul>
        </div>
      )}

      <div className="service-card__cost">
        Razem: {service.koszt_calkowity.toFixed(2)} zl
      </div>

      <div className="service-card__actions">
        {service.ma_raport ? (
          <span style={{ color: "var(--text-muted)", fontSize: 13 }}>
            ✓ Raport wygenerowany (#{service.raport_id})
          </span>
        ) : (
          <button
            className="primary"
            onClick={() => onGenerateReport(service)}
          >
            Wygeneruj raport
          </button>
        )}
      </div>
    </div>
  );
}

export default ServiceCard;
