import { useNavigate } from 'react-router-dom';
import './GameRoomItem.css';

function isUserInRoom(room, userId) {
  if (!userId) return false;
  if (room.participantIds) {
    return room.participantIds.map(String).includes(String(userId));
  }
  return (room.players || []).some((p) => String(p.userId) === String(userId));
}

function GameRoomItem({ room, onJoin, onLeave, onEnd, currentUserId }) {
  const navigate = useNavigate();

  const isHost = String(room.hostId) === String(currentUserId);
  const isParticipant = isUserInRoom(room, currentUserId);

  const status = room.status ? room.status.toUpperCase() : '';
  const inProgress = status === 'GAME_IN_PROGRESS';

  const statusClass =
    status === 'OPEN'
      ? 'game-room-item__status--open'
      : inProgress
      ? 'game-room-item__status--in-progress'
      : 'game-room-item__status--closed';

  const statusLabel =
    status === 'OPEN'
      ? 'Open'
      : inProgress
      ? 'In Progress'
      : status === 'CLOSED'
      ? 'Closed'
      : status.replace(/_/g, ' ');

  const navigateToRoom = () => {
    if (inProgress) {
      navigate(`/game/${room.roomCode}`);
    } else {
      navigate(`/game-room/${room.roomCode}`);
    }
  };

  const handleCardClick = () => {
    if (isParticipant) {
      navigateToRoom();
    }
  };

  const handleCardKeyDown = (e) => {
    if (!isParticipant) return;
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      navigateToRoom();
    }
  };

  const handleEnter = (e) => {
    e.stopPropagation();
    navigateToRoom();
  };

  const handleJoin = (e) => {
    e.stopPropagation();
    if (onJoin) onJoin(room.roomCode);
  };

  const handleLeave = (e) => {
    e.stopPropagation();
    if (onLeave) onLeave(room.roomCode);
  };

  const handleEnd = (e) => {
    e.stopPropagation();
    if (onEnd) onEnd(room.roomCode);
  };

  return (
    <div
      className={`game-room-item${isParticipant ? ' game-room-item--clickable' : ''}`}
      onClick={handleCardClick}
      onKeyDown={handleCardKeyDown}
      role={isParticipant ? 'button' : undefined}
      tabIndex={isParticipant ? 0 : undefined}
    >
      <div className="game-room-item__info">
        <div className="game-room-item__title-row">
          <h3 className="game-room-item__name">{room.name}</h3>
          <span className={`game-room-item__status ${statusClass}`}>{statusLabel}</span>
        </div>
        <p className="game-room-item__meta">
          Host: <strong>{room.hostUsername || '—'}</strong>
        </p>
        <p className="game-room-item__meta">
          Players: {room.currentPlayers}/{room.maxPlayers}
        </p>
        {room.roomCode && (
          <p className="game-room-item__code">Code: {room.roomCode}</p>
        )}
      </div>

      <div className="game-room-item__actions">
        {isParticipant ? (
          <>
            <button
              type="button"
              className="game-room-item__btn game-room-item__btn--enter"
              onClick={handleEnter}
            >
              {inProgress ? 'Resume Game' : 'Enter Room'}
            </button>
            {isHost ? (
              <button
                type="button"
                className="game-room-item__btn game-room-item__btn--end"
                onClick={handleEnd}
              >
                {inProgress ? 'End Game' : 'Delete Room'}
              </button>
            ) : (
              <button
                type="button"
                className="game-room-item__btn game-room-item__btn--leave"
                onClick={handleLeave}
              >
                Leave
              </button>
            )}
          </>
        ) : (
          status === 'OPEN' && (
            <button
              type="button"
              className="game-room-item__btn game-room-item__btn--join"
              onClick={handleJoin}
            >
              Join
            </button>
          )
        )}
      </div>
    </div>
  );
}

export default GameRoomItem;
