import { useNavigate } from 'react-router-dom';
import './GameResultPanel.css';

function GameResultPanel({ winner, players, onPlayAgain }) {
  const navigate = useNavigate();
  const isMafiaWin = winner === 'MAFIA';

  const mafiaPlayers = (players || []).filter((p) => p.role === 'MAFIA');
  const citizenPlayers = (players || []).filter((p) => p.role === 'CITIZEN');
  const survivors = (players || []).filter((p) => p.isAlive === true).length;

  return (
    <div
      className={`game-result-panel${isMafiaWin ? ' game-result-panel--mafia' : ' game-result-panel--citizens'}`}
    >
      <div className="game-result-panel__header">
        <span className="game-result-panel__trophy">
          {isMafiaWin ? '🎭' : '🏆'}
        </span>
        <h1 className="game-result-panel__winner">
          {isMafiaWin ? 'Mafia Wins!' : 'Citizens Win!'}
        </h1>
        <p className="game-result-panel__subtitle">
          {isMafiaWin
            ? 'The Mafia has taken over the town!'
            : 'The town has eliminated all Mafia members!'}
        </p>
      </div>

      <div className="game-result-panel__stats">
        <div className="game-result-panel__stat">
          <span className="game-result-panel__stat-num">{(players || []).length}</span>
          <span className="game-result-panel__stat-label">Total Players</span>
        </div>
        <div className="game-result-panel__stat">
          <span className="game-result-panel__stat-num">{survivors}</span>
          <span className="game-result-panel__stat-label">Survivors</span>
        </div>
        <div className="game-result-panel__stat">
          <span className="game-result-panel__stat-num">{mafiaPlayers.length}</span>
          <span className="game-result-panel__stat-label">Mafia</span>
        </div>
      </div>

      <div className="game-result-panel__teams">
        <div className="game-result-panel__team game-result-panel__team--mafia">
          <h3>🎭 Mafia Team</h3>
          {mafiaPlayers.map((p) => (
            <div
              key={p.userId}
              className={`game-result-panel__player${!p.isAlive ? ' game-result-panel__player--dead' : ''}`}
            >
              <span>{p.username}</span>
              <span>{p.isAlive ? '✔ Survived' : '💀 Eliminated'}</span>
            </div>
          ))}
        </div>

        <div className="game-result-panel__team game-result-panel__team--citizens">
          <h3>👥 Citizens Team</h3>
          {citizenPlayers.map((p) => (
            <div
              key={p.userId}
              className={`game-result-panel__player${!p.isAlive ? ' game-result-panel__player--dead' : ''}`}
            >
              <span>{p.username}</span>
              <span>{p.isAlive ? '✔ Survived' : '💀 Eliminated'}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="game-result-panel__actions">
        <button
          className="game-result-panel__btn game-result-panel__btn--primary"
          onClick={onPlayAgain || (() => navigate('/dashboard'))}
        >
          Back to Dashboard
        </button>
      </div>
    </div>
  );
}

export default GameResultPanel;
