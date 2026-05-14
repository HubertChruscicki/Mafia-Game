import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
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
        const apiBase = process.env.REACT_APP_API_BASE_URL;
        if (!apiBase) {
          setRoom({
            name: `Room ${roomCode}`,
            hostUsername: 'Host',
            currentPlayers: 3,
            maxPlayers: 8,
            status: 'OPEN',
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
    };

    if (roomCode) fetchRoom();
  }, [roomCode]);

  const handleJoin = async () => {
    setJoining(true);
    setError('');
    try {
      const apiBase = process.env.REACT_APP_API_BASE_URL;
      if (!apiBase) {
        navigate(`/game-room/${roomCode}`);
        return;
      }
      const token = localStorage.getItem('token');
      const response = await fetch(`${apiBase}/api/game_rooms/join/${roomCode}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
      });
      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || 'Failed to join room');
      }
      navigate(`/game-room/${roomCode}`);
    } catch (err) {
      setError(err.message || 'Failed to join room');
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
