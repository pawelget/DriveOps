import { Routes, Route, useNavigate, useLocation, Navigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { getToken } from "./utils/token.jsx";

import Layout from "./components/common/Layout";
import HomePage from "./components/pages/HomePage.jsx";
import LoginPage from "./components/pages/LoginPage.jsx";
import RegisterPage from "./components/pages/RegisterPage.jsx";
import ProfilePage from "./components/pages/ProfilePage.jsx";

function App() {
  const navigate = useNavigate();
  const location = useLocation();
  
  // Używamy stanu, aby React wiedział, kiedy przeliczyć isAuthenticated
  const [token, setToken] = useState(getToken());

  // Nasłuchujemy na zmiany w location - za każdym razem sprawdzamy token
  useEffect(() => {
    setToken(getToken());
  }, [location]);

  const isAuthenticated = !!token;
  const isAuthPage = ["/logowanie", "/rejestracja"].includes(location.pathname);

  const handleNavigate = (id) => {
    switch (id) {
      case "dashboard": navigate("/"); break;
      case "drivers": navigate("/kierowcy"); break;
      case "settings": navigate("/ustawienia"); break;
      case "profile": navigate("/profil"); break;
      default: console.log(id);
    }
  };

  if (!isAuthenticated && !isAuthPage) {
    return <Navigate to="/logowanie" replace />;
  }

  if (isAuthenticated && isAuthPage) {
    return <Navigate to="/" replace />;
  }

  if (isAuthPage) {
    return (
      <div className="auth-wrapper">
        <Routes>
          <Route path="/logowanie" element={<LoginPage />} />
          <Route path="/rejestracja" element={<RegisterPage />} />
        </Routes>
      </div>
    );
  }

  return (
    <Layout onNavigate={handleNavigate}>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/profil" element={<ProfilePage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  );
}

export default App;