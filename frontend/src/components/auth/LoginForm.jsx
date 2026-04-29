import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { createPortal } from "react-dom";
import Input from "../ui/Input";
import Button from "../ui/Button";
import MessageBox from "../ui/MessageBox";
import { loginUser } from "../../api/AuthApi";
import { saveToken } from "../../utils/token";

function LoginForm() {
  const navigate = useNavigate();
  const smokeCanvasRef = useRef(null);
  const smokeAnimRef = useRef(null);

  const [form, setForm] = useState({ email: "", haslo: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [loginSuccess, setLoginSuccess] = useState(false);
  const [carDriving, setCarDriving] = useState(false);
  const [hideForm, setHideForm] = useState(false);

  useEffect(() => {
    const canvas = smokeCanvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");

    const puffs = Array.from({ length: 8 }, (_, i) => ({
      x: 250,
      y: 270,
      size: 12 + Math.random() * 14,
      phase: (i / 8) * Math.PI * 2,
      speed: 0.15 + Math.random() * 0.25,
      wobble: (Math.random() - 0.5) * 0.4,
    }));

    let t = 0;

    function drawPuff(x, y, size, opacity) {
      if (opacity <= 0) return;
      ctx.save();
      ctx.globalAlpha = opacity;

      ctx.fillStyle = "rgba(100,100,100,0.3)";
      ctx.beginPath();
      ctx.arc(x + 2, y + 2, size * 0.9, 0, Math.PI * 2);
      ctx.fill();

      ctx.fillStyle = "rgba(220,220,220,0.85)";
      ctx.beginPath();
      ctx.arc(x, y, size, 0, Math.PI * 2);
      ctx.arc(x - size * 0.6, y + size * 0.3, size * 0.65, 0, Math.PI * 2);
      ctx.arc(x + size * 0.6, y + size * 0.2, size * 0.7, 0, Math.PI * 2);
      ctx.arc(x + size * 0.1, y - size * 0.5, size * 0.6, 0, Math.PI * 2);
      ctx.fill();

      ctx.strokeStyle = "rgba(80,80,80,0.4)";
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.arc(x, y, size, 0, Math.PI * 2);
      ctx.stroke();
      ctx.beginPath();
      ctx.arc(x - size * 0.6, y + size * 0.3, size * 0.65, 0, Math.PI * 2);
      ctx.stroke();
      ctx.beginPath();
      ctx.arc(x + size * 0.6, y + size * 0.2, size * 0.7, 0, Math.PI * 2);
      ctx.stroke();
      ctx.beginPath();
      ctx.arc(x + size * 0.1, y - size * 0.5, size * 0.6, 0, Math.PI * 2);
      ctx.stroke();

      ctx.restore();
    }

    function animate() {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      t += 0.016;

      puffs.forEach((p) => {
        const progress = (t * p.speed + p.phase / (Math.PI * 2)) % 1;
        const x = p.x + progress * -220 + Math.sin(progress * 5 + p.wobble) * 10;
        const y = p.y - progress * 80;
        const size = p.size * (0.5 + progress * 2.2);
        const opacity =
          progress < 0.15
            ? progress / 0.15
            : progress > 0.7
            ? (1 - progress) / 0.3
            : 1;

        drawPuff(x, y, size, opacity * 0.75);
      });

      smokeAnimRef.current = requestAnimationFrame(animate);
    }

    animate();

    return () => cancelAnimationFrame(smokeAnimRef.current);
  }, []);

  function handleChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const data = await loginUser(form);
      saveToken(data.token);
      setLoginSuccess(true);

      setTimeout(() => setHideForm(true), 400);
      setTimeout(() => setCarDriving(true), 900);
      setTimeout(() => navigate("/profil"), 2800);
    } catch (err) {
      setError(err.message);
      setLoading(false);
      setLoginSuccess(false);
      setCarDriving(false);
      setHideForm(false);
    }
  }

  // canvas renderowany przez portal bezpośrednio w body
  const smokePortal = createPortal(
    <canvas
      ref={smokeCanvasRef}
      width={500}
      height={300}
      className={`smoke-canvas ${carDriving ? "driving" : ""}`}
    />,
    document.body
  );

  return (
    <div className="login-animation-wrapper">
      {smokePortal}

      <img
        src="/car.png"
        alt="Samochód"
        className={`login-car ${carDriving ? "drive" : ""}`}
      />

      <form
        onSubmit={handleSubmit}
        className={`card login-card ${hideForm ? "hide-login-card" : ""}`}
      >
        <h2>Logowanie</h2>
        <MessageBox type="error" message={error} />
        <Input
          label="Email"
          type="email"
          name="email"
          value={form.email}
          onChange={handleChange}
          placeholder="Podaj email"
        />
        <Input
          label="Haslo"
          type="password"
          name="haslo"
          value={form.haslo}
          onChange={handleChange}
          placeholder="Podaj haslo"
        />
        <Button
          type="submit"
          disabled={loading || loginSuccess}
          className={`login-submit-button ${loading ? "loading-button" : ""} ${
            loginSuccess ? "success-button" : ""
          }`}
        >
          {loginSuccess ? (
            <span className="button-check">✓</span>
          ) : loading ? (
            <span className="button-loader"></span>
          ) : (
            "Zaloguj sie"
          )}
        </Button>
      </form>
    </div>
  );
}

export default LoginForm;