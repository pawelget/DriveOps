function ReportFilters({ filters, onChange, onClear, cars }) {
  function handleChange(field, value) {
    onChange({ ...filters, [field]: value });
  }

  return (
    <div className="reports-filters">
      <div className="reports-filters__row">
        <input
          type="text"
          placeholder="Szukaj (czynnosci, opis, nr raportu)..."
          value={filters.search}
          onChange={(e) => handleChange("search", e.target.value)}
        />
        <input
          type="text"
          placeholder="Warsztat"
          value={filters.warsztat}
          onChange={(e) => handleChange("warsztat", e.target.value)}
        />
        <select
          value={filters.samochod_id}
          onChange={(e) => handleChange("samochod_id", e.target.value)}
        >
          <option value="">Wszystkie pojazdy</option>
          {cars.map((c) => (
            <option key={c.id} value={c.id}>
              {c.marka} {c.model} ({c.numer_rejestracyjny})
            </option>
          ))}
        </select>
      </div>

      <div className="reports-filters__row">
        <input
          type="date"
          placeholder="Data od"
          value={filters.date_from}
          onChange={(e) => handleChange("date_from", e.target.value)}
        />
        <input
          type="date"
          placeholder="Data do"
          value={filters.date_to}
          onChange={(e) => handleChange("date_to", e.target.value)}
        />
        <div style={{ display: "flex", gap: 8 }}>
          <input
            type="number"
            placeholder="Koszt od"
            value={filters.cost_min}
            onChange={(e) => handleChange("cost_min", e.target.value)}
          />
          <input
            type="number"
            placeholder="Koszt do"
            value={filters.cost_max}
            onChange={(e) => handleChange("cost_max", e.target.value)}
          />
        </div>
      </div>

      <div className="reports-filters__actions">
        <button
          type="button"
          className="reports-filters__clear"
          onClick={onClear}
        >
          Wyczysc filtry
        </button>
      </div>
    </div>
  );
}

export default ReportFilters;
