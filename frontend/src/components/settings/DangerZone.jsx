import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Input from "../ui/Input";
import Button from "../ui/Button";
import MessageBox from "../ui/MessageBox";
import { deactivateAccount } from "../../api/UserApi";
import { removeToken } from "../../utils/token";

function DangerZone() {
  const navigate = useNavigate();

  const [haslo, setHaslo] = useState("");
  const [confirming, setConfirming] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleDeactivate(e) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await deactivateAccount({ haslo });
      removeToken();
      navigate("/logowanie");
    } catch (err) {
      setError(err.message);
      setLoading(false);
    }
  }

  return (
    <div className="card">
      <h2>Strefa niebezpieczna</h2>
      <p>Dezaktywacja konta wyloguje Cie i uniemozliwi ponowne zalogowanie.</p>

      {!confirming ? (
        <Button
          type="button"
          className="auth-button"
        >
          <span onClick={() => setConfirming(true)}>Dezaktywuj konto</span>
        </Button>
      ) : (
        <form onSubmit={handleDeactivate} className="auth-form">
          <MessageBox type="error" message={error} />
          <Input
            label="Potwierdz haslem"
            type="password"
            name="haslo"
            value={haslo}
            onChange={(e) => setHaslo(e.target.value)}
            placeholder="Twoje haslo"
          />
          <Button
            type="submit"
            disabled={loading}
            className="auth-button"
          >
            {loading ? <span className="button-loader"></span> : "Potwierdz dezaktywacje"}
          </Button>
        </form>
      )}
    </div>
  );
}

export default DangerZone;
