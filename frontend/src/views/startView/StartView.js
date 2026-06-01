import { Link } from 'react-router-dom';
import './StartView.css';

function StartView() {
  return (
    <main className="start-view">
      <div className="start-view__bg" aria-hidden="true" />
      <section className="start-view__card">
        <div className="start-view__logo">eMafia</div>
        <h1 className="start-view__headline">Zagraj w Mafię bez mistrza gry</h1>
        <p className="start-view__description">
          Prowadź lokalne rozgrywki Mafii — role, głosowania i eliminacje obsługuje platforma.
          Dołącz do pokoju lub stwórz własny i zaproś znajomych.
        </p>
        <div className="start-view__actions">
          <Link className="start-view__button start-view__button--primary" to="/login">
            Zaloguj się
          </Link>
          <Link className="start-view__button start-view__button--secondary" to="/register">
            Zarejestruj się
          </Link>
        </div>
        <p className="start-view__hint">Dołączasz linkiem? <Link className="start-view__link" to="/enter-code">Wpisz kod pokoju</Link></p>
      </section>
    </main>
  );
}

export default StartView;
