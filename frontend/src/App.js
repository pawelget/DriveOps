import { Routes, Route } from "react-router-dom";
import Layout from "./components/common/Layout";
import HomePage from "./components/pages/HomePage.jsx";
import LoginPage from "./components/pages/LoginPage.jsx";
import RegisterPage from "./components/pages/RegisterPage.jsx";
import ProfilePage from "./components/pages/ProfilePage.jsx";

function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/logowanie" element={<LoginPage />} />
        <Route path="/rejestracja" element={<RegisterPage />} />
        <Route path="/profil" element={<ProfilePage />} />
      </Routes>
    </Layout>
  );
}

export default App;