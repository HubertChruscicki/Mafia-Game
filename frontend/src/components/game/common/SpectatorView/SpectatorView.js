import VotingTimer from '../VotingTimer/VotingTimer';
import './SpectatorView.css';

function SpectatorView({ players, myUsername, phase, timerSeconds, waitingOnly }) {
  const alivePlayers = (players || []).filter((p) => p.isAlive === true);
  const deadPlayers = (players || []).filter((p) => p.isAlive !== true);
  const isNight = phase && phase.includes('NIGHT');

  return (
    <div className={`spectator-view${isNight ? ' spectator-view--night' : ' spectator-view--day'}`}>
      <div className="spectator-view__header">
        <span className="spectator-view__ghost">👻</span>
        <h2 className="spectator-view__title">
          {waitingOnly ? 'Waiting for voting to end' : 'You have been eliminated'}
        </h2>
        <p className="spectator-view__subtitle">Spectator mode — watch the game unfold</p>
      </div>

      {timerSeconds != null && (
        <VotingTimer remainingSeconds={timerSeconds} totalSeconds={60} />
      )}

      <div className="spectator-view__stats">
        <div className="spectator-view__stat">
          <span className="spectator-view__stat-num">{alivePlayers.length}</span>
          <span className="spectator-view__stat-label">Alive</span>
        </div>
        <div className="spectator-view__stat">
          <span className="spectator-view__stat-num">{deadPlayers.length}</span>
          <span className="spectator-view__stat-label">Eliminated</span>
        </div>
      </div>

      <div className="spectator-view__players">
        {(players || []).map((player) => {
          const alive = player.isAlive === true;
          const isMe = player.username === myUsername;
          return (
            <div
              key={player.userId}
              className={`spectator-view__player${!alive ? ' spectator-view__player--dead' : ''}`}
            >
              <span>{alive ? '🟢' : '💀'}</span>
              <span>
                {player.username || player.gameNick}
                {isMe && ' (You)'}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default SpectatorView;
