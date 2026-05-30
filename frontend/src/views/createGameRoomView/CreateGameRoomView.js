import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiFetch } from '../../services/authApi';
import FormMessage from '../../components/formMessage/FormMessage';
import './CreateGameRoomView.css';

function CreateGameRoomView() {
  const [roomName, setRoomName] = useState('');
  const [maxPlayers, setMaxPlayers] = useState(5);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await apiFetch('/api/game_rooms/create', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: roomName.trim(), maxPlayers: Number(maxPlayers) }),
      });

      if (!response.ok) {
        const payload = await response.json().catch(() => ({}));
        throw new Error(payload.message || 'Failed to create room.');
      }

      const data = await response.json();
      navigate(`/game-room/${data.roomCode}`);
    } catch (err) {
      setError(err.message || 'Failed to create room.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="create-room">
      <h2 className="create-room__title">Create a Game Room</h2>
      <form className="create-room__form" onSubmit={handleSubmit}>
        <FormMessage type="error" message={error} />
        <div className="create-room__field">
          <input
            type="text"
            placeholder="Room name"
            value={roomName}
            onChange={(event) => setRoomName(event.target.value)}
            required
            minLength={3}
            maxLength={100}
          />
        </div>
        <div className="create-room__field">
          <label htmlFor="maxPlayers">Max Players (2-20):</label>
          <input
            type="number"
            id="maxPlayers"
            value={maxPlayers}
            onChange={(event) => setMaxPlayers(event.target.value)}
            required
            min={2}
            max={20}
          />
        </div>
        <button className="create-room__submit" type="submit" disabled={loading}>
          {loading ? 'Creating...' : 'Create Room'}
        </button>
      </form>
    </div>
  );
}

export default CreateGameRoomView;
