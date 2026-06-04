import './FormInput.css';

function FormInput({
  label,
  type = 'text',
  name,
  placeholder,
  value,
  onChange,
  disabled = false,
  required = false,
}) {
  return (
    <label className="form-input">
      <span className="form-input__label">{label}</span>
      <input
        className="form-input__control"
        type={type}
        name={name}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        disabled={disabled}
        required={required}
        autoComplete="off"
      />
    </label>
  );
}

export default FormInput;
