import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { MdEmail, MdLock, MdPerson } from 'react-icons/md';
import { registerUser } from '../../services/authApi';
import {
  getEmailError,
  getPasswordError,
  getUsernameError,
} from '../../utils/validators';
import FormInput from '../formInput/FormInput';
import FormMessage from '../formMessage/FormMessage';
import './RegisterForm.css';

function RegisterForm() {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((previous) => ({ ...previous, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const usernameError = getUsernameError(formData.username);
    if (usernameError) {
      setError(usernameError);
      return;
    }

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

    if (formData.password !== formData.confirmPassword) {
      setError('Hasla nie sa takie same.');
      return;
    }

    setLoading(true);
    setError('');
    try {
      await registerUser({
        username: formData.username,
        email: formData.email,
        password: formData.password,
      });
      navigate('/login', {
        state: { message: 'Konto utworzone. Zaloguj sie, aby kontynuowac.' },
      });
    } catch (submitError) {
      setError(submitError.message || 'Rejestracja nie powiodla sie.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="register-form">
      <h1 className="register-form__title">Rejestracja</h1>
      <div className="register-form__inner">
        <FormMessage type="error" message={error} />
        <form onSubmit={handleSubmit}>
          <FormInput
            label={<MdPerson size={22} />}
            name="username"
            placeholder="Nazwa użytkownika"
            value={formData.username}
            onChange={handleChange}
            disabled={loading}
            required
          />
          <FormInput
            label={<MdEmail size={22} />}
            type="email"
            name="email"
            placeholder="Twój email"
            value={formData.email}
            onChange={handleChange}
            disabled={loading}
            required
          />
          <FormInput
            label={<MdLock size={22} />}
            type="password"
            name="password"
            placeholder="Hasło"
            value={formData.password}
            onChange={handleChange}
            disabled={loading}
            required
          />
          <FormInput
            label={<MdLock size={22} />}
            type="password"
            name="confirmPassword"
            placeholder="Powtórz hasło"
            value={formData.confirmPassword}
            onChange={handleChange}
            disabled={loading}
            required
          />
          <button className="register-form__button" type="submit" disabled={loading}>
            {loading ? 'Tworzenie konta...' : 'Utworz konto'}
          </button>
        </form>
        <p className="register-form__switch">
          Masz juz konto?{' '}
          <Link to="/login" className="register-form__link">
            Zaloguj sie
          </Link>
        </p>
      </div>
    </section>
  );
}

export default RegisterForm;
