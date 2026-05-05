import { Link } from 'react-router-dom';
import './StartView.css';

function StartView() {
  return (
    <main className="start-view">
      <section className="start-view__card">
        <h1 className="start-view__title">eMafia</h1>
        <p className="start-view__subtitle">
          Platforma do prowadzenia lokalnej rozgrywki Mafia bez mistrza gry.
        </p>
        <p className="start-view__meta">
          Szkielet aplikacji gotowy. Kolejne widoki zostana dodane w nastepnych commitach.
        </p>
        <div className="start-view__actions">
          <Link className="start-view__button start-view__button--primary" to="/login">
            Zaloguj sie
          </Link>
          <Link className="start-view__button start-view__button--secondary" to="/register">
            Rejestracja
          </Link>
        </div>
      </section>
    </main>
  );
}

export default StartView;
