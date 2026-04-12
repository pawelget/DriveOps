function MessageBox({ type = "info", message }) {
  if (!message) return null;

  return <div className={`message ${type}`}>{message}</div>;
}

export default MessageBox;