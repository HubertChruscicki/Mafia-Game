import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import useAuthSession from '../../hooks/useAuthSession';
import { fetchCurrentUser, loginUser } from '../../services/authApi';
import { getEmailError, getPasswordError } from '../../utils/validators';
import FormInput from '../formInput/FormInput';
import FormMessage from '../formMessage/FormMessage';
import './LoginForm.css';

function LoginForm() {
  const [formData, setFormData] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { storeSession, storeUser } = useAuthSession();
  const successMessage = location.state?.message || '';

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((previous) => ({ ...previous, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const emailError = getEmailError(formData.email);
    if (emailError) {
      setError(emailError);
      return;
    }

    const passwordError = getPasswordError(formData.password);
    if (passwordError) {
      setError(passwordError);
      return;
    }

    setLoading(true);
    setError('');

    try {
      const session = await loginUser(formData);
      storeSession(session);
      try {
        const currentUser = await fetchCurrentUser();
        if (currentUser) {
          storeUser(currentUser);
        }
      } catch (userError) {
        // ignore: header will just fall back to anonymous label
      }
      navigate('/dashboard');
    } catch (submitError) {
      setError(submitError.message || 'Logowanie nie powiodlo sie.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="auth-form">
      <h1 className="auth-form__title">Logowanie</h1>
      <div className="auth-form__inner">
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
            disabled={loading}
            required
          />
          <FormInput
            label="Haslo"
            type="password"
            name="password"
            placeholder="Wpisz haslo"
            value={formData.password}
            onChange={handleChange}
            disabled={loading}
            required
          />
          <button className="auth-form__button" type="submit" disabled={loading}>
            {loading ? 'Logowanie...' : 'Zaloguj'}
          </button>
        </form>
        <p className="auth-form__switch">
          Nie masz konta?{' '}
          <Link to="/register" className="auth-form__link">
            Zarejestruj sie
          </Link>
        </p>
      </div>
    </section>
  );
}

export default LoginForm;
