function Button({ children, className = "", type = "button", disabled = false }) {
  return (
    <button className={className} type={type} disabled={disabled}>
      {children}
    </button>
  );
}

export default Button;