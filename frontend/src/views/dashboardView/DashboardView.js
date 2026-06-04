import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './DashboardView.css';

function DashboardView() {
  const [searchTerm, setSearchTerm] = useState('');
  const navigate = useNavigate();

  const handleSearch = (event) => {
    event.preventDefault();
  };

  return (
    <div className="dashboard">
      <header className="dashboard__header">
        <button
          className="dashboard__new-game"
          type="button"
          onClick={() => navigate('/create-room')}
        >
          New Game
        </button>
        <form className="dashboard__search" onSubmit={handleSearch}>
          <input
            className="dashboard__search-input"
            type="text"
            placeholder="Search games..."
            value={searchTerm}
            onChange={(event) => setSearchTerm(event.target.value)}
          />
        </form>
      </header>

      <section>
        <h2 className="dashboard__section-title">Your Games</h2>
        <div className="dashboard__empty">
          <p className="dashboard__empty-message">
            You are not currently participating in any games.
          </p>
          <p className="dashboard__empty-subtext">
            Create a new game or search for existing ones to join!
          </p>
        </div>
      </section>
    </div>
  );
}

export default DashboardView;
