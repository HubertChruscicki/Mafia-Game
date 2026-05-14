import './FormMessage.css';

function FormMessage({ type = 'error', message }) {
  if (!message) {
    return null;
  }

  return <p className={`form-message form-message--${type}`}>{message}</p>;
}

export default FormMessage;
