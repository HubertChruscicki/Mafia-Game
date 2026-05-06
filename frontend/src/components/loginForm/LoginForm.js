import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import FormInput from '../formInput/FormInput';
import FormMessage from '../formMessage/FormMessage';
import './LoginForm.css';

function LoginForm() {
  const [formData, setFormData] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const location = useLocation();
  const successMessage = location.state?.message || '';

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((previous) => ({ ...previous, [name]: value }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();

    if (!formData.email.trim() || !formData.password.trim()) {
      setError('Uzupelnij email i haslo.');
      return;
    }

    localStorage.setItem('token', 'dev-auth-token');
    navigate('/dashboard');
  };

  return (
    <section className="auth-form">
      <h1 className="auth-form__title">Logowanie</h1>
      <FormMessage type="success" message={successMessage} />
      <FormMessage type="error" message={error} />
      <form onSubmit={handleSubmit}>
        <FormInput
          label="Email"
          type="email"
          name="email"
          placeholder="twoj@email.com"
          value={formData.email}
          onChange={handleChange}
          required
        />
        <FormInput
          label="Haslo"
          type="password"
          name="password"
          placeholder="Wpisz haslo"
          value={formData.password}
          onChange={handleChange}
          required
        />
        <button className="auth-form__button" type="submit">
          Zaloguj
        </button>
      </form>
      <p className="auth-form__switch">
        Nie masz konta?{' '}
        <Link to="/register" className="auth-form__link">
          Zarejestruj sie
        </Link>
      </p>
    </section>
  );
}

export default LoginForm;
