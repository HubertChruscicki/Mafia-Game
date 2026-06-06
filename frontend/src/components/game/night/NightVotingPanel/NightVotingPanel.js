import './NightVotingPanel.css';

function NightVotingPanel({ players, myVote, onVote, voteCount, hasVoted, currentUserId }) {
  return (
    <div className="night-voting-panel">
      <div className="night-voting-panel__header">
        <span className="night-voting-panel__icon">🌙</span>
        <h2 className="night-voting-panel__title">Night Vote</h2>
        <p className="night-voting-panel__subtitle">
          Select a player. Everyone votes — keep your choice discreet.
        </p>
      </div>

      {hasVoted && (
        <p className="night-voting-panel__voted-notice">
          ✓ Your vote has been cast. Waiting for others...
        </p>
      )}

      <div className="night-voting-panel__players">
        {players.map((player) => {
          const playerId = String(player.userId);
          const count = voteCount[playerId] || 0;
          const isMe = playerId === String(currentUserId);
          const alreadyVotedThis = String(myVote) === playerId;

          return (
            <div
              key={player.userId}
              className={`night-voting-panel__player${isMe ? ' night-voting-panel__player--me' : ''}`}
            >
              <span className="night-voting-panel__player-name">
                {player.username || player.gameNick}
                {isMe && ' (You)'}
              </span>
              <div className="night-voting-panel__vote-bar-wrap">
                <div
                  className="night-voting-panel__vote-bar"
                  style={{ width: count > 0 ? `${Math.min(count * 20, 100)}%` : '0%' }}
                />
                <span className="night-voting-panel__vote-count">{count}</span>
              </div>
              <button
                className={`night-voting-panel__vote-btn${alreadyVotedThis ? ' night-voting-panel__vote-btn--active' : ''}`}
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

export default NightVotingPanel;
