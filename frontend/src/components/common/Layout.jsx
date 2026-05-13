import Sidebar from "./Sidebar"; // upewnij się, że ścieżka jest poprawna

function Layout({ children, onNavigate }) {
  return (
    <div className="dashboard-layout">
      {/* Sidebar jest teraz stałym elementem układu po lewej stronie */}
      <Sidebar 
        onNavigate={onNavigate} 
        userName="Jan Kowalski" 
        userRole="Admin" 
      />
      
      {/* Główna treść strony (HomePage, Profil itp.) wyświetli się tutaj */}
      <main className="dashboard-content">
        {children}
      </main>
    </div>
  );
}

export default Layout;