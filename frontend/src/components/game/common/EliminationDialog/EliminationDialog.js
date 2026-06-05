import { useEffect, useState } from 'react';
import './EliminationDialog.css';

function EliminationDialog({ eliminated, isTie, phase, onClose }) {
  const [canClose, setCanClose] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => setCanClose(true), 3000);
    return () => clearTimeout(timer);
  }, []);

  if (!eliminated) return null;

  const isNight = phase === 'NIGHT_VOTE' || phase === 'NIGHT_RESULT';
  const hasElimination =
    eliminated.eliminatedUsername &&
    eliminated.resultType !== 'NO_ELIMINATION' &&
    eliminated.resultType !== 'EXPIRED_NO_VOTES';

  return (
    <div className="elimination-dialog__overlay">
      <div
        className={`elimination-dialog${isNight ? ' elimination-dialog--night' : ' elimination-dialog--day'}`}
      >
        {canClose && (
          <button className="elimination-dialog__close" onClick={onClose} aria-label="Close">
            ✕
          </button>
        )}

        <span className="elimination-dialog__icon">
          {hasElimination ? '💀' : isNight ? '🌙' : '☀️'}
        </span>

        <h2 className="elimination-dialog__title">
          {isTie && !hasElimination
            ? 'Tie Vote — No Elimination'
            : isNight
            ? 'Mafia Strikes'
            : 'Town Voted'}
        </h2>

        {hasElimination ? (
          <div className="elimination-dialog__eliminated">
            <p className="elimination-dialog__label">
              {isNight ? 'Killed by the Mafia:' : 'Voted out:'}
            </p>
            <p className="elimination-dialog__name">{eliminated.eliminatedUsername}</p>
          </div>
        ) : (
          <p className="elimination-dialog__no-elim">
            {isTie ? 'The vote ended in a tie.' : 'No one was eliminated.'}
          </p>
        )}

        {canClose && (
          <button className="elimination-dialog__ok" onClick={onClose}>
            OK
          </button>
        )}
        {!canClose && (
          <p className="elimination-dialog__wait">Closes in a moment...</p>
        )}
      </div>
    </div>
  );
}

export default EliminationDialog;
