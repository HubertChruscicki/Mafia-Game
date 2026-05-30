import { useState, useEffect } from 'react';
import './GameRoomSettings.css';

function GameRoomSettings({
  mafiaCount: initialMafiaCount = 1,
  discussionTime: initialDiscussionTime = 120,
  onSettingsChange,
  onStartGame,
  currentPlayers = 0,
  minPlayersRequired = 3,
}) {
  const maxMafia = Math.max(1, Math.floor(currentPlayers / 2));
  const [mafiaCount, setMafiaCount] = useState(
    Math.min(Math.max(1, initialMafiaCount), maxMafia)
  );
  const [discussionTime, setDiscussionTime] = useState(initialDiscussionTime);

  // Clamp mafiaCount when player count changes
  useEffect(() => {
    const newMax = Math.max(1, Math.floor(currentPlayers / 2));
    setMafiaCount((prev) => Math.min(prev, newMax));
  }, [currentPlayers]);

  // Notify parent of setting changes
  useEffect(() => {
    if (onSettingsChange) {
      onSettingsChange({ mafiaCount, discussionTimeSeconds: discussionTime });
    }
  }, [mafiaCount, discussionTime, onSettingsChange]);

  const tooFewPlayers = currentPlayers < minPlayersRequired;

  const mafiaOptions = Array.from({ length: maxMafia }, (_, i) => i + 1);

  const timeOptions = [];
  for (let t = 30; t <= 600; t += 30) {
    const mins = Math.floor(t / 60);
    const secs = t % 60;
    let label;
    if (mins === 0) label = `${t} seconds`;
    else if (secs === 0) label = `${mins} min`;
    else label = `${mins}m ${secs}s`;
    timeOptions.push({ value: t, label });
  }

  const formatTime = (t) => {
    const mins = Math.floor(t / 60);
    const secs = t % 60;
    if (mins === 0) return `${t}s`;
    if (secs === 0) return `${mins} min`;
    return `${mins}m ${secs}s`;
  };

  return (
    <div className="game-settings">
      <div className="game-settings__header">
        <h3 className="game-settings__title">Game Settings</h3>
        <p className="game-settings__subtitle">Configure your game before starting</p>
      </div>

      {tooFewPlayers && (
        <p className="game-settings__warning">
          At least {minPlayersRequired} players are required to start. Currently: {currentPlayers}.
        </p>
      )}

      <div className="game-settings__grid">
        <div className="game-settings__card">
          <div className="game-settings__card-header">
            <span className="game-settings__label">Number of Mafia</span>
            <span className="game-settings__value">{mafiaCount}</span>
          </div>
          <p className="game-settings__hint">Recommended: 1 mafia per 3–4 players</p>
          <div className="game-settings__btn-group">
            {mafiaOptions.map((count) => (
              <button
                key={count}
                type="button"
                className={`game-settings__option-btn${mafiaCount === count ? ' game-settings__option-btn--active' : ''}`}
                onClick={() => setMafiaCount(count)}
              >
                {count}
              </button>
            ))}
          </div>
        </div>

        <div className="game-settings__card">
          <div className="game-settings__card-header">
            <span className="game-settings__label">Discussion Time</span>
            <span className="game-settings__value">{formatTime(discussionTime)}</span>
          </div>
          <p className="game-settings__hint">Recommended: 2–3 minutes</p>
          <select
            className="game-settings__select"
            value={discussionTime}
            onChange={(e) => setDiscussionTime(Number(e.target.value))}
          >
            {timeOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="game-settings__summary">
        <span>Mafia: <strong>{mafiaCount}</strong></span>
        <span>Discussion: <strong>{formatTime(discussionTime)}</strong></span>
        <span>Mode: <strong>Classic</strong></span>
      </div>

      <button
        type="button"
        className="game-settings__start-btn"
        onClick={() => onStartGame && onStartGame()}
        disabled={tooFewPlayers}
      >
        Start Game
      </button>
    </div>
  );
}

export default GameRoomSettings;
