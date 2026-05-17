function Button({
  children,
  className = "",
  type = "button",
  disabled = false,
  onClick,
}) {
  return (
    <button
      className={className}
      type={type}
      disabled={disabled}
      onClick={onClick}
    >
      {children}
    </button>
  );
}

export default Button;