import './FormInput.css';

function FormInput({
  label: Icon,
  type = 'text',
  name,
  placeholder,
  value,
  onChange,
  disabled = false,
  required = false,
  autoComplete = 'off',
}) {
  return (
    <div className="form-input">
      {Icon && <label className="form-input__icon" htmlFor={name}>{Icon}</label>}
      <input
        className="form-input__control"
        id={name}
        type={type}
        name={name}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        disabled={disabled}
        required={required}
        autoComplete={autoComplete}
        autoCorrect="off"
        autoCapitalize="none"
        spellCheck="false"
      />
    </div>
  );
}

export default FormInput;
