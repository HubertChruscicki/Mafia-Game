import './DayVotingPanel.css';

function DayVotingPanel({ players, myVote, onVote, voteCount, hasVoted, currentUserId }) {
  return (
    <div className="day-voting-panel">
      <div className="day-voting-panel__header">
        <span className="day-voting-panel__icon">☀️</span>
        <h2 className="day-voting-panel__title">Day Vote</h2>
        <p className="day-voting-panel__subtitle">
          Vote to eliminate a suspected Mafia member
        </p>
      </div>

      {hasVoted && (
        <p className="day-voting-panel__voted-notice">
          ✓ Your vote has been cast. Waiting for others...
        </p>
      )}

      <div className="day-voting-panel__players">
        {players.map((player) => {
          const playerId = String(player.userId);
          const count = voteCount[playerId] || 0;
          const isMe = playerId === String(currentUserId);
          const alreadyVotedThis = String(myVote) === playerId;

          return (
            <div
              key={player.userId}
              className={`day-voting-panel__player${isMe ? ' day-voting-panel__player--me' : ''}`}
            >
              <span className="day-voting-panel__player-name">
                {player.username || player.gameNick}
                {isMe && ' (You)'}
              </span>
              <div className="day-voting-panel__vote-bar-wrap">
                <div
                  className="day-voting-panel__vote-bar"
                  style={{ width: count > 0 ? `${Math.min(count * 20, 100)}%` : '0%' }}
                />
                <span className="day-voting-panel__vote-count">{count}</span>
              </div>
              <button
                className={`day-voting-panel__vote-btn${alreadyVotedThis ? ' day-voting-panel__vote-btn--active' : ''}`}
                onClick={() => onVote(player.userId)}
                disabled={hasVoted || isMe}
              >
                {alreadyVotedThis ? 'Voted' : 'Vote'}
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default DayVotingPanel;
