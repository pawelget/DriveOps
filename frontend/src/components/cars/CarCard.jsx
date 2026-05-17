function CarCard({ car, onEdit, onDelete }) {

  const paliwoLabels = {
    benzyna: "Benzyna",
    diesel: "Diesel",
    elektryczny: "Elektryczny",
    hybryda: "Hybryda",
    benzyna_gaz: "Benzyna + LPG",
  };

  return (
    <div className="car-card">
      <div className="car-card__header">
        <div className="car-card__brand">
          {car.marka} {car.model}
        </div>
        <div className="car-card__plate">{car.numer_rejestracyjny}</div>
      </div>

      <div className="car-card__details">
        {car.rok_produkcji && (
          <div className="car-card__detail">
            <span className="car-card__detail-label">Rok</span>
            <span>{car.rok_produkcji}</span>
          </div>
        )}
        {car.paliwo && (
          <div className="car-card__detail">
            <span className="car-card__detail-label">Paliwo</span>
            <span>{paliwoLabels[car.paliwo] || car.paliwo}</span>
          </div>
        )}
        {car.przebieg !== null && car.przebieg !== undefined && (
          <div className="car-card__detail">
            <span className="car-card__detail-label">Przebieg</span>
            <span>{car.przebieg.toLocaleString("pl-PL")} km</span>
          </div>
        )}
        {car.moc_km && (
          <div className="car-card__detail">
            <span className="car-card__detail-label">Moc</span>
            <span>{car.moc_km} KM</span>
          </div>
        )}
        {car.kolor && (
          <div className="car-card__detail">
            <span className="car-card__detail-label">Kolor</span>
            <span>{car.kolor}</span>
          </div>
        )}
      </div>

      <div className="car-card__actions">
        <button
          className="car-card__action"
          onClick={() => onEdit(car)}
        >
          Edytuj
        </button>
        <button
          className="car-card__action car-card__action--danger"
          onClick={() => onDelete(car)}
        >
          Usun
        </button>
      </div>
    </div>
  );
}

export default CarCard;
