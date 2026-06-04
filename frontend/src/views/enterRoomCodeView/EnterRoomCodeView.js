import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './EnterRoomCodeView.css';

function EnterRoomCodeView() {
  const [roomCode, setRoomCode] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = (event) => {
    event.preventDefault();
    const trimmed = roomCode.trim().toUpperCase();
    if (!trimmed) {
      setError('Please enter a room code.');
      return;
    }
    if (trimmed.length !== 6) {
      setError('Room code must be 6 characters long.');
      return;
    }
    if (!/^[A-Z0-9]+$/.test(trimmed)) {
      setError('Room code can only contain uppercase letters and numbers.');
      return;
    }
    setError('');
    navigate(`/join/${trimmed}`);
  };

  return (
    <div className="enter-code">
      <form className="enter-code__form" onSubmit={handleSubmit}>
        <h2 className="enter-code__title">Enter Game Code</h2>
        {error && <p className="enter-code__error">{error}</p>}
        <div className="enter-code__field">
          <input
            type="text"
            placeholder="ABCDEF"
            value={roomCode}
            onChange={(event) => setRoomCode(event.target.value)}
            maxLength={6}
            required
            autoFocus
          />
        </div>
        <button className="enter-code__submit" type="submit">
          Join Game
        </button>
      </form>
    </div>
  );
}

export default EnterRoomCodeView;
