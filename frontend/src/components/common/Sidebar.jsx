import { useState } from "react";
import "./Sidebar.css";
import { removeToken } from "../../utils/token.jsx";

// ── Icons ──────────────────────────────────────────────
const Icon = ({ d, size = 18 }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none"
    stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d={d} />
  </svg>
);

const icons = {
  dashboard: <><rect x="3" y="3" width="7" height="7" stroke="currentColor" strokeWidth="2" fill="none" rx="1"/><rect x="14" y="3" width="7" height="7" stroke="currentColor" strokeWidth="2" fill="none" rx="1"/><rect x="14" y="14" width="7" height="7" stroke="currentColor" strokeWidth="2" fill="none" rx="1"/><rect x="3" y="14" width="7" height="7" stroke="currentColor" strokeWidth="2" fill="none" rx="1"/></>,
  drives:    <Icon d="M3 17a1 1 0 0 1 1-1h16a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-2zM3 7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7z" />,
  routes:    <Icon d="M3 12h18M3 6l9-3 9 3M3 18l9 3 9-3" />,
  drivers:   <Icon d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />,
  reports:   <Icon d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8zM14 2v6h6M16 13H8M16 17H8M10 9H8" />,
  alerts:    <Icon d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0" />,
  settings:  <Icon d="M12 20a8 8 0 1 0 0-16 8 8 0 0 0 0 16zM12 14a2 2 0 1 0 0-4 2 2 0 0 0 0 4z" />,
  logo:      <Icon d="M5 17H3a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11a2 2 0 0 1 2 2v3M9 21h10a2 2 0 0 0 2-2v-7a2 2 0 0 0-2-2H9a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2z" />,
  chevron:   null,
};

const handleLogout = (e) => {
  e.stopPropagation(); // Kluczowe: zapobiega przejściu do profilu przy wylogowywaniu
  removeToken();
  window.location.href = "/logowanie";
};



const ChevronSVG = ({ open }) => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
    stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
    className={`sidebar-chevron${open ? " open" : ""}`}>
    <polyline points="9 18 15 12 9 6" />
  </svg>
);

const ArrowSVG = () => (
  <svg width="12" height="12" viewBox="0 0 24 24" fill="none"
    stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="15 18 9 12 15 6" />
  </svg>
);

// ── Nav config ─────────────────────────────────────────
const NAV = [
  {
    section: "Główne",
    items: [
      { id: "dashboard", label: "Dashboard" },
      { id: "drives",    label: "Pojazdy" },
      { id: "routes",    label: "Trasy",
        children: [
          { id: "routes-active",   label: "Aktywne" },
          { id: "routes-history",  label: "Historia" },
          { id: "routes-planned",  label: "Planowane" },
        ]
      },
      { id: "drivers",   label: "Kierowcy" },
    ]
  },
  {
    section: "Zarządzanie",
    items: [
      { id: "reports",  label: "Raporty", badge: 2 },
      { id: "alerts",   label: "Alerty",  badge: 5 },
      { id: "settings", label: "Ustawienia" },
    ]
  }
];

// ── Component ──────────────────────────────────────────
export default function Sidebar({ active: activeProp, onNavigate, userName = "Użytkownik", userRole = "Admin" }) {
  const [active, setActive]     = useState(activeProp || "dashboard");
  const [expanded, setExpanded] = useState({ routes: true });
  const [collapsed, setCollapsed] = useState(false);

  const navigate = (id) => {
    setActive(id);
    onNavigate?.(id);
  };

  const handleProfileClick = () => {
    onNavigate("profile");
  };

  const toggle = (id) =>
    setExpanded(prev => ({ ...prev, [id]: !prev[id] }));

  // initials from name
  const initials = userName
    .split(" ")
    .map(w => w[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();

  return (
    <nav className={`sidebar${collapsed ? " collapsed" : ""}`}>

      {/* Collapse toggle */}
      <button className="sidebar-toggle" onClick={() => setCollapsed(c => !c)} title={collapsed ? "Rozwiń" : "Zwiń"}>
        <ArrowSVG />
      </button>

      {/* Logo */}
      <a className="sidebar-logo" href="#">
        <div className="sidebar-logo-icon">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <path d="M5 17H3a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11a2 2 0 0 1 2 2v3M9 21h10a2 2 0 0 0 2-2v-7a2 2 0 0 0-2-2H9a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2z"/>
          </svg>
        </div>
        <span className="sidebar-logo-text">DriveOps</span>
      </a>

      {/* Nav sections */}
      <div className="sidebar-nav">
        {NAV.map(({ section, items }) => (
          <div key={section}>
            <div className="sidebar-section-label">{section}</div>

            {items.map(item => {
              const isActive = active === item.id || item.children?.some(c => c.id === active);
              const isOpen   = expanded[item.id];
              const IconEl   = icons[item.id];

              return (
                <div className="sidebar-item-wrap" key={item.id}>
                  {/* Tooltip for collapsed */}
                  <div className="sidebar-tooltip">{item.label}</div>

                  <div
                    className={`sidebar-item${isActive && !item.children ? " active" : ""}${isActive && item.children ? " active" : ""}`}
                    onClick={() => item.children ? toggle(item.id) : navigate(item.id)}
                  >
                    <span className="sidebar-item-icon">
                      {IconEl && (
                        <svg width="17" height="17" viewBox="0 0 24 24" fill="none"
                          stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                          {IconEl}
                        </svg>
                      )}
                    </span>
                    <span className="sidebar-item-label">{item.label}</span>
                    {item.badge && <span className="sidebar-badge">{item.badge}</span>}
                    {item.children && <ChevronSVG open={isOpen} />}
                  </div>

                  {/* Sub-items */}
                  {item.children && (
                    <div className={`sidebar-subitems${isOpen ? " open" : ""}`}>
                      {item.children.map(child => (
                        <div
                          key={child.id}
                          className={`sidebar-subitem${active === child.id ? " active" : ""}`}
                          onClick={() => navigate(child.id)}
                        >
                          {child.label}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              );
            })}

            <div className="sidebar-divider" />
          </div>
        ))}
      </div>

      {/* User footer */}
      <div className="sidebar-footer">
        <div className="sidebar-user" onClick={handleProfileClick}>
          <div className="sidebar-avatar">{initials}</div>
          <div className="sidebar-user-info">
            <div className="sidebar-user-name">{userName}</div>
            <button className="logout-btn" onClick={handleLogout}>
              Wyloguj się
            </button>
        </div>
    </div>
</div>
    </nav>
  );
}