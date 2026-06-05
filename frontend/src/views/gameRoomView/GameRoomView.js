import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { QRCodeSVG } from 'qrcode.react';
import { apiFetch } from '../../services/authApi';
import useAuthSession from '../../hooks/useAuthSession';
import useGameRoomLobby from '../../hooks/useGameRoomLobby';
import GameRoomSettings from '../../components/gameRoomSettings/GameRoomSettings';
import './GameRoomView.css';

function isMember(players, userId) {
  if (!userId) return false;
  return (players || []).some((p) => String(p.userId) === String(userId));
}

function GameRoomView() {
  const { roomCode } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthSession();

  const [room, setRoom] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notification, setNotification] = useState('');
  const [copied, setCopied] = useState(false);
  const [leaving, setLeaving] = useState(false);
  const [joining, setJoining] = useState(false);
  const [starting, setStarting] = useState(false);
  const [gameSettings, setGameSettings] = useState({ mafiaCount: 1, discussionTimeSeconds: 120 });

  const joinUrl = `${window.location.origin}/join/${roomCode}`;

  const applyRoomData = useCallback((data) => {
    setRoom(data);
    if (data.mafiaCount != null || data.discussionTimeSeconds != null) {
      setGameSettings((prev) => ({
        mafiaCount: data.mafiaCount ?? prev.mafiaCount,
        discussionTimeSeconds: data.discussionTimeSeconds ?? prev.discussionTimeSeconds,
      }));
    }
  }, []);

  const fetchRoom = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await apiFetch(`/api/game_rooms/${roomCode}`);
      if (response.status === 404) throw new Error('Room not found');
      if (!response.ok) throw new Error('Failed to load room');
      const data = await response.json();

      if (data.status === 'GAME_IN_PROGRESS' && isMember(data.players, user?.id)) {
        navigate(`/game/${roomCode}`, { replace: true });
        return;
      }

      applyRoomData(data);
    } catch (err) {
      setError(err.message || 'Failed to load room');
    } finally {
      setLoading(false);
    }
  }, [roomCode, navigate, user?.id, applyRoomData]);

  useEffect(() => {
    fetchRoom();
  }, [fetchRoom]);

  useGameRoomLobby(roomCode, {
    currentUserId: user?.id,
    onRoomUpdate: (data) => {
      setRoom((prev) => {
        const merged = prev ? { ...prev, ...data } : data;
        if (data.mafiaCount != null || data.discussionTimeSeconds != null) {
          setGameSettings((prevSettings) => ({
            mafiaCount: merged.mafiaCount ?? prevSettings.mafiaCount,
            discussionTimeSeconds: merged.discussionTimeSeconds ?? prevSettings.discussionTimeSeconds,
          }));
        }
        return merged;
      });
    },
    onRoomDeleted: () => {
      setNotification('This room was deleted by the host.');
      setTimeout(() => navigate('/dashboard'), 2000);
    },
  });

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(joinUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopied(false);
    }
  };

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
        throw new Error(payload.message || 'Failed to join room');
      }
      await fetchRoom();
    } catch (err) {
      setError(err.message || 'Failed to join room');
    } finally {
      setJoining(false);
    }
  };

  const handleLeave = async () => {
    if (!window.confirm('Are you sure you want to leave this room?')) return;
    setLeaving(true);
    setError('');
    try {
      const response = await apiFetch(`/api/game_rooms/leave/${roomCode}`, { method: 'POST' });
      if (!response.ok) {
        const payload = await response.json().catch(() => ({}));
        throw new Error(payload.error || payload.message || 'Failed to leave room');
      }
      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Failed to leave room');
      setLeaving(false);
    }
  };

  const handleStartGame = async (settings) => {
    const mafiaCount = settings?.mafiaCount ?? gameSettings.mafiaCount;
    const discussionTimeSeconds =
      settings?.discussionTimeSeconds ?? gameSettings.discussionTimeSeconds;
    setStarting(true);
    setError('');
    try {
      const response = await apiFetch('/api/games/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          roomCode,
          mafiaCount,
          discussionTimeSeconds,
        }),
      });
      if (!response.ok) {
        const payload = await response.json().catch(() => ({}));
        throw new Error(payload.error || payload.message || 'Failed to start game');
      }
      navigate(`/game/${roomCode}`);
    } catch (err) {
      setError(err.message || 'Failed to start game');
      setStarting(false);
    }
  };

  if (loading) {
    return (
      <div className="game-room">
        <p className="game-room__info">Loading room...</p>
      </div>
    );
  }

  if (notification) {
    return (
      <div className="game-room">
        <p className="game-room__notification">{notification}</p>
        <p className="game-room__info">Redirecting to dashboard...</p>
      </div>
    );
  }

  if (error && !room) {
    return (
      <div className="game-room">
        <p className="game-room__error">{error}</p>
        <button type="button" className="game-room__secondary-btn" onClick={() => navigate('/dashboard')}>
          Back to Dashboard
        </button>
      </div>
    );
  }

  if (!room) {
    return (
      <div className="game-room">
        <p className="game-room__error">Room not found.</p>
        <button type="button" className="game-room__secondary-btn" onClick={() => navigate('/dashboard')}>
          Back to Dashboard
        </button>
      </div>
    );
  }

  const players = Array.isArray(room.players) ? room.players : [];
  const status = room.status || 'UNKNOWN';
  const isHost =
    user != null &&
    room.hostId != null &&
    String(user.id) === String(room.hostId);
  const isRoomMember = isMember(players, user?.id);

  return (
    <div className="game-room">
      <div className="game-room__header-row">
        <h2 className="game-room__title">
          Room: {room.name} ({roomCode})
        </h2>
        <button
          type="button"
          className="game-room__secondary-btn"
          onClick={() => navigate('/dashboard')}
        >
          Back to Dashboard
        </button>
      </div>

      <div className="game-room__share">
        <div className="game-room__qr">
          <QRCodeSVG value={joinUrl} size={128} bgColor="#ffffff" fgColor="#1a1a1a" />
          <p className="game-room__qr-label">Scan to join</p>
        </div>
        <div className="game-room__share-links">
          <button type="button" className="game-room__copy-btn" onClick={handleCopyLink}>
            {copied ? 'Link copied!' : 'Copy join link'}
          </button>
          <span className="game-room__link">{joinUrl}</span>
        </div>
      </div>

      <div className="game-room__meta">
        <div>
          <strong>Status:</strong> {status.replace(/_/g, ' ')}
        </div>
        <div>
          <strong>Host:</strong> {room.hostUsername || 'Unknown'}
        </div>
        <div>
          <strong>Players:</strong> {room.currentPlayers ?? players.length}/{room.maxPlayers}
        </div>
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
                {String(player.userId) === String(room.hostId) && (
                  <span className="game-room__host-badge">Host</span>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>

      {error && <p className="game-room__error">{error}</p>}

      {!isRoomMember && status === 'OPEN' && (
        <div className="game-room__join-section">
          <button
            type="button"
            className="game-room__primary-btn"
            onClick={handleJoin}
            disabled={joining}
          >
            {joining ? 'Joining...' : 'Join Room'}
          </button>
        </div>
      )}

      {isHost && isRoomMember && (
        <div className="game-room__settings-section">
          <GameRoomSettings
            roomCode={roomCode}
            mafiaCount={gameSettings.mafiaCount}
            discussionTime={gameSettings.discussionTimeSeconds}
            onSettingsChange={setGameSettings}
            onStartGame={handleStartGame}
            currentPlayers={players.length}
            minPlayersRequired={3}
          />
          {starting && <p className="game-room__info">Starting game...</p>}
        </div>
      )}

      {isRoomMember && !isHost && status === 'OPEN' && (
        <p className="game-room__info">Waiting for the host to start the game...</p>
      )}

      {isRoomMember && (
        <div className="game-room__btn-row">
          <button
            type="button"
            className="game-room__danger-btn"
            onClick={handleLeave}
            disabled={leaving}
          >
            {leaving ? 'Leaving...' : isHost ? 'Delete Room' : 'Leave Room'}
          </button>
        </div>
      )}
    </div>
  );
}

export default GameRoomView;
