import DayVotingPanel from '../../day/DayVotingPanel/DayVotingPanel';
import NightVotingPanel from '../../night/NightVotingPanel/NightVotingPanel';
import SpectatorView from '../../common/SpectatorView/SpectatorView';
import EliminationDialog from '../../common/EliminationDialog/EliminationDialog';
import VotingTimer from '../../common/VotingTimer/VotingTimer';
import './VotingPhaseContainer.css';

function VotingPhaseContainer({
  gameId,
  roomCode,
  currentUser,
  players,
  gamePhase,
  votingSession,
  timerSeconds,
  discussionTimeSeconds,
  onVoteCast,
  eliminationResult,
  onClearElimination,
}) {
  const currentPlayer = players?.find(
    (p) => String(p.userId) === String(currentUser?.id)
  );
  const isAlive = currentPlayer ? currentPlayer.isAlive !== false : true;
  const alivePlayers = (players || []).filter((p) => p.isAlive !== false);

  const voteCount = {};
  if (votingSession?.currentResults) {
    Object.entries(votingSession.currentResults).forEach(([k, v]) => {
      voteCount[String(k)] = v;
    });
  }

  const hasVoted = votingSession?.hasVoted === true;

  const isDayVote = gamePhase === 'DAY_VOTE';
  const isNightVote = gamePhase === 'NIGHT_VOTE';
  const isDay = isDayVote || gamePhase === 'DAY_RESULT';
  const isNight = isNightVote || gamePhase === 'NIGHT_RESULT';
  const showTimer = (isDayVote || isNightVote) && timerSeconds != null;

  return (
    <div className="voting-phase-container">
      {eliminationResult && (
        <EliminationDialog
          eliminated={eliminationResult}
          isTie={eliminationResult?.isTie || false}
          phase={gamePhase}
          onClose={onClearElimination}
        />
      )}

      {showTimer && (
        <div className="voting-phase-container__timer">
          <VotingTimer
            remainingSeconds={timerSeconds}
            totalSeconds={discussionTimeSeconds || 60}
          />
        </div>
      )}

      {!isAlive ? (
        <SpectatorView
          players={players}
          myUsername={currentUser?.username}
          phase={gamePhase}
          timerSeconds={showTimer ? timerSeconds : null}
        />
      ) : isDayVote ? (
        <DayVotingPanel
          players={alivePlayers}
          myVote={votingSession?.myVote || null}
          onVote={onVoteCast}
          voteCount={voteCount}
          hasVoted={hasVoted}
          currentUserId={currentUser?.id}
        />
      ) : isNightVote ? (
        <NightVotingPanel
          players={alivePlayers}
          myVote={votingSession?.myVote || null}
          onVote={onVoteCast}
          voteCount={voteCount}
          hasVoted={hasVoted}
          currentUserId={currentUser?.id}
        />
      ) : isDay || isNight ? (
        <div className="voting-phase-container__waiting">
          <p>{gamePhase === 'DAY_RESULT' ? 'Day results...' : 'Night results...'}</p>
        </div>
      ) : (
        <div className="voting-phase-container__waiting">
          <p>Waiting for the next phase...</p>
        </div>
      )}
    </div>
  );
}

export default VotingPhaseContainer;
