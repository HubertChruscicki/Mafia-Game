import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import './GameRoomView.css';

function GameRoomView() {
  const { roomCode } = useParams();
  const navigate = useNavigate();
  const [room, setRoom] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);

  const joinUrl = `${window.location.origin}/join/${roomCode}`;

  const fetchRoom = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const apiBase = process.env.REACT_APP_API_BASE_URL;
      if (!apiBase) {
        setRoom({
          name: `Room ${roomCode}`,
          hostUsername: 'You',
          currentPlayers: 1,
          maxPlayers: 8,
          status: 'OPEN',
          players: [{ userId: '1', username: 'You', isHost: true }],
        });
        setLoading(false);
        return;
      }
      const token = localStorage.getItem('token');
      const response = await fetch(`${apiBase}/api/game_rooms/${roomCode}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!response.ok) throw new Error('Failed to load room');
      const data = await response.json();
      setRoom(data);
    } catch (err) {
      setError(err.message || 'Failed to load room');
    } finally {
      setLoading(false);
    }
  }, [roomCode]);

  useEffect(() => {
    fetchRoom();
  }, [fetchRoom]);

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(joinUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopied(false);
    }
  };

  const handleLeave = async () => {
    if (!window.confirm('Are you sure you want to leave this room?')) return;
    try {
      const apiBase = process.env.REACT_APP_API_BASE_URL;
      if (apiBase) {
        const token = localStorage.getItem('token');
        await fetch(`${apiBase}/api/game_rooms/leave/${roomCode}`, {
          method: 'POST',
          headers: { Authorization: `Bearer ${token}` },
        });
      }
      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Failed to leave room');
    }
  };

  if (loading) {
    return (
      <div className="game-room">
        <p className="game-room__info">Loading room...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="game-room">
        <p className="game-room__error">{error}</p>
        <button className="game-room__secondary-btn" onClick={() => navigate('/dashboard')}>
          Back to Dashboard
        </button>
      </div>
    );
  }

  if (!room) {
    return (
      <div className="game-room">
        <p className="game-room__error">Room not found.</p>
        <button className="game-room__secondary-btn" onClick={() => navigate('/dashboard')}>
          Back to Dashboard
        </button>
      </div>
    );
  }

  const players = Array.isArray(room.players) ? room.players : [];
  const status = room.status || 'UNKNOWN';

  return (
    <div className="game-room">
      <div className="game-room__header-row">
        <h2 className="game-room__title">
          Room: {room.name} ({roomCode})
        </h2>
        <button className="game-room__secondary-btn" onClick={() => navigate('/dashboard')}>
          Back to Dashboard
        </button>
      </div>

      <div className="game-room__share">
        <button className="game-room__copy-btn" onClick={handleCopyLink}>
          {copied ? 'Link copied!' : 'Copy join link'}
        </button>
        <span className="game-room__link">{joinUrl}</span>
      </div>

      <div className="game-room__meta">
        <div><strong>Status:</strong> {status.replace(/_/g, ' ')}</div>
        <div><strong>Host:</strong> {room.hostUsername || 'Unknown'}</div>
        <div><strong>Players:</strong> {room.currentPlayers}/{room.maxPlayers}</div>
      </div>

      <div className="game-room__section">
        <h3>Players</h3>
        {players.length === 0 ? (
          <p className="game-room__info">No players in this room yet.</p>
        ) : (
          <ul className="game-room__player-list">
            {players.map((player, index) => (
              <li key={player.userId || index} className="game-room__player-item">
                <span>{player.username || player.nicknameInRoom || player.userId}</span>
                {player.isHost && <span className="game-room__host-badge">Host</span>}
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="game-room__btn-row">
        <button className="game-room__danger-btn" onClick={handleLeave}>
          Leave Room
        </button>
      </div>
    </div>
  );
}

export default GameRoomView;
