import ProfileForm from "../settings/ProfileForm";
import PasswordForm from "../settings/PasswordForm";
import DangerZone from "../settings/DangerZone";

function SettingsPage() {
  return (
    <div>
      <h1>Ustawienia</h1>
      <ProfileForm />
      <PasswordForm />
      <DangerZone />
    </div>
  );
}

export default SettingsPage;
