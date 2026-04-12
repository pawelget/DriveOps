import { useEffect, useState } from "react";
import { getCurrentUser } from "../../api/AuthApi";
import MessageBox from "../ui/MessageBox.jsx";

function ProfilePage() {
  const [user, setUser] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchUser() {
      try {
        const data = await getCurrentUser();
        setUser(data);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }

    fetchUser();
  }, []);

  if (loading) {
    return <div className="card">Ladowanie profilu...</div>;
  }

  if (error) {
    return (
      <div className="card">
        <MessageBox type="error" message={error} />
      </div>
    );
  }

  return (
    <div className="card">
      <h2>Profil uzytkownika</h2>
      <p><strong>ID:</strong> {user.id}</p>
      <p><strong>Imie:</strong> {user.imie}</p>
      <p><strong>Nazwisko:</strong> {user.nazwisko}</p>
      <p><strong>Email:</strong> {user.email}</p>
      <p><strong>Telefon:</strong> {user.telefon || "brak"}</p>
      <p><strong>Aktywny:</strong> {user.czy_aktywny ? "tak" : "nie"}</p>
    </div>
  );
}

export default ProfilePage;