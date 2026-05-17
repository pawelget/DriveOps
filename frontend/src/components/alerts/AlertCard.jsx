function AlertCard({ alert }) {
  const priorytetLabels = {
    critical: "Krytyczny",
    warning: "Ostrzezenie",
    info: "Info",
  };

  const typLabels = {
    przeglad: "Przeglad",
    serwis: "Serwis",
  };

  return (
    <div className={`alert-card alert-card--${alert.priorytet}`}>
      <div className="alert-card__header">
        <span className={`alert-badge alert-badge--${alert.priorytet}`}>
          {priorytetLabels[alert.priorytet]}
        </span>
        <span className="alert-type">{typLabels[alert.typ] || alert.typ}</span>
        <span className="alert-date">{alert.data}</span>
      </div>
      <h3 className="alert-card__title">{alert.tytul}</h3>
      <p className="alert-card__desc">{alert.opis}</p>
      <div className="alert-card__footer">
        <span className="alert-car">{alert.samochod_info}</span>
      </div>
    </div>
  );
}

export default AlertCard;
