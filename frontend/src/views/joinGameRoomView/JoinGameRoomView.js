import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { apiFetch } from '../../services/authApi';
import './JoinGameRoomView.css';

function JoinGameRoomView() {
  const { roomCode } = useParams();
  const navigate = useNavigate();
  const [room, setRoom] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [joining, setJoining] = useState(false);

  useEffect(() => {
    const fetchRoom = async () => {
      setLoading(true);
      setError('');
      try {
        const response = await apiFetch(`/api/game_rooms/${roomCode}`);
        if (response.status === 404) throw new Error('Room not found');
        if (!response.ok) {
          const payload = await response.json().catch(() => ({}));
          throw new Error(payload.message || 'Failed to load room');
        }
        const data = await response.json();
        setRoom(data);
      } catch (err) {
        setError(err.message || 'Nie udało się załadować pokoju');
      } finally {
        setLoading(false);
      }
    };

    if (roomCode) fetchRoom();
  }, [roomCode]);

  const handleJoin = async () => {
    setJoining(true);
    setError('');
    try {
      const response = await apiFetch(`/api/game_rooms/join/${roomCode}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
      });
      if (!response.ok) {
        const payload = await response.json().catch(() => ({}));
        throw new Error(payload.message || 'Nie udało się dołączyć do pokoju');
      }
      navigate(`/game-room/${roomCode}`);
    } catch (err) {
      setError(err.message || 'Nie udało się dołączyć do pokoju');
    } finally {
      setJoining(false);
    }
  };

  return (
    <div className="join-room">
      <h2 className="join-room__title">Join Game Room</h2>

      {loading && <p className="join-room__info">Loading room...</p>}
      {error && <p className="join-room__error">{error}</p>}

      {!loading && room && (
        <>
          <div className="join-room__details">
            <p><strong>Room:</strong> {room.name} ({roomCode})</p>
            <p><strong>Host:</strong> {room.hostUsername || 'Unknown'}</p>
            <p><strong>Players:</strong> {room.currentPlayers}/{room.maxPlayers}</p>
            <p><strong>Status:</strong> {String(room.status || 'UNKNOWN').replace(/_/g, ' ')}</p>
          </div>

          <div className="join-room__actions">
            <button
              className="join-room__primary-btn"
              onClick={handleJoin}
              disabled={joining}
            >
              {joining ? 'Joining...' : 'Join Room'}
            </button>
            <button
              className="join-room__secondary-btn"
              onClick={() => navigate('/dashboard')}
            >
              Back to Dashboard
            </button>
          </div>
        </>
      )}

      {!loading && !room && !error && (
        <div className="join-room__actions">
          <button
            className="join-room__secondary-btn"
            onClick={() => navigate('/dashboard')}
          >
            Back to Dashboard
          </button>
        </div>
      )}
    </div>
  );
}

export default JoinGameRoomView;
