import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
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
  const navigate = useNavigate();

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((previous) => ({ ...previous, [name]: value }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();

    if (!formData.username.trim() || !formData.email.trim() || !formData.password.trim()) {
      setError('Uzupelnij wszystkie pola formularza.');
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      setError('Hasla nie sa takie same.');
      return;
    }

    navigate('/login', {
      state: { message: 'Konto utworzone. Zaloguj sie, aby kontynuowac.' },
    });
  };

  return (
    <section className="register-form">
      <h1 className="register-form__title">Rejestracja</h1>
      <FormMessage type="error" message={error} />
      <form onSubmit={handleSubmit}>
        <FormInput
          label="Nazwa uzytkownika"
          name="username"
          placeholder="Podaj nick"
          value={formData.username}
          onChange={handleChange}
          required
        />
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
        <FormInput
          label="Powtorz haslo"
          type="password"
          name="confirmPassword"
          placeholder="Wpisz haslo ponownie"
          value={formData.confirmPassword}
          onChange={handleChange}
          required
        />
        <button className="register-form__button" type="submit">
          Utworz konto
        </button>
      </form>
      <p className="register-form__switch">
        Masz juz konto?{' '}
        <Link to="/login" className="register-form__link">
          Zaloguj sie
        </Link>
      </p>
    </section>
  );
}

export default RegisterForm;
