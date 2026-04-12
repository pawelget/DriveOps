import { Link, useNavigate } from "react-router-dom";
import { getToken, removeToken } from "../../utils/token";

function Navbar() {
  const navigate = useNavigate();
  const isLoggedIn = !!getToken();

  function handleLogout() {
    removeToken();
    navigate("/logowanie");
  }

  return (
    <nav className="navbar">
      <div>
        <Link to="/">DriveOps</Link>
      </div>

      <div className="nav-links">
        {!isLoggedIn && <Link to="/rejestracja">Rejestracja</Link>}
        {!isLoggedIn && <Link to="/logowanie">Logowanie</Link>}
        {isLoggedIn && <Link to="/profil">Profil</Link>}
        {isLoggedIn && <button onClick={handleLogout}>Wyloguj</button>}
      </div>
    </nav>
  );
}

export default Navbar;