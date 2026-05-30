import GameRoomItem from '../gameRoomItem/GameRoomItem';
import './GameRoomList.css';

function GameRoomList({ rooms, onJoin, onLeave, onEnd, currentUserId }) {
  if (!rooms || rooms.length === 0) {
    return (
      <p className="game-room-list__empty">No rooms found.</p>
    );
  }

  return (
    <div className="game-room-list">
      {rooms.map((room) => (
        <GameRoomItem
          key={room.id || room.roomCode}
          room={room}
          onJoin={onJoin}
          onLeave={onLeave}
          onEnd={onEnd}
          currentUserId={currentUserId}
        />
      ))}
    </div>
  );
}

export default GameRoomList;
