import { useEffect, useState } from "react";
import Sidebar from "./Sidebar";
import { getCurrentUser } from "../../api/AuthApi";

function Layout({ children, onNavigate }) {
  const [user, setUser] = useState(null);

  useEffect(() => {
    async function loadUser() {
      try {
        const data = await getCurrentUser();
        setUser(data);
      } catch (err) {
        console.error("Nie udalo sie pobrac danych uzytkownika:", err);
      }
    }
    loadUser();
  }, []);

  const userName = user
    ? `${user.imie} ${user.nazwisko}`
    : "Uzytkownik";

  const userRole = user?.czy_aktywny ? "Aktywny" : "Nieaktywny";

  return (
    <div className="dashboard-layout">
      <Sidebar
        onNavigate={onNavigate}
        userName={userName}
        userRole={userRole}
      />

      <main className="dashboard-content">
        {children}
      </main>
    </div>
  );
}

export default Layout;