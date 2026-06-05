import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import useAuthSession from '../../hooks/useAuthSession';
import useGameRooms from '../../hooks/useGameRooms';
import { apiFetch } from '../../services/authApi';
import SearchGameRoomBar from '../../components/searchGameRoomBar/SearchGameRoomBar';
import GameRoomList from '../../components/gameRoomList/GameRoomList';
import './DashboardView.css';

function DashboardView() {
  const navigate = useNavigate();
  const { user } = useAuthSession();
  const { rooms, loading, error, fetchRoomsByUserId, leaveRoom } = useGameRooms();

  const [activeTab, setActiveTab] = useState('yourRooms');
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState(null);
  const [searchLoading, setSearchLoading] = useState(false);
  const [searchError, setSearchError] = useState(null);

  useEffect(() => {
    if (user?.id) {
      fetchRoomsByUserId(user.id);
    }
  }, [user?.id, fetchRoomsByUserId]);

  const handleSearch = useCallback(async (query) => {
    if (!query.trim()) {
      setSearchResults(null);
      setSearchError(null);
      return;
    }
    setSearchLoading(true);
    setSearchError(null);
    try {
      const response = await apiFetch(`/api/game_rooms/search?name=${encodeURIComponent(query)}`);
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(data.message || 'Search failed');
      }
      setSearchResults(Array.isArray(data) ? data : data.rooms || []);
    } catch (err) {
      setSearchError(err.message || 'Search failed');
      setSearchResults([]);
    } finally {
      setSearchLoading(false);
    }
  }, []);

  const handleJoin = useCallback(async (roomCode) => {
    try {
      const response = await apiFetch(`/api/game_rooms/join/${roomCode}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
      });
      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.message || 'Failed to join room');
      }
      navigate(`/game-room/${roomCode}`);
    } catch (err) {
      alert(`Error: ${err.message}`);
    }
  }, [navigate]);

  const handleLeave = useCallback(async (roomCode) => {
    if (!window.confirm('Are you sure you want to leave this room?')) return;
    try {
      await leaveRoom(roomCode);
      if (user?.id) fetchRoomsByUserId(user.id);
      if (searchResults) {
        setSearchResults((prev) => prev.filter((r) => r.roomCode !== roomCode));
      }
    } catch (err) {
      alert(`Error: ${err.message}`);
    }
  }, [leaveRoom, fetchRoomsByUserId, user?.id, searchResults]);

  const handleEnd = useCallback(async (roomCode) => {
    if (!window.confirm('Are you sure you want to end this room?')) return;
    try {
      const response = await apiFetch(`/api/game_rooms/leave/${roomCode}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
      });
      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.error || data.message || 'Failed to end room');
      }
      if (user?.id) fetchRoomsByUserId(user.id);
      if (searchResults) {
        setSearchResults((prev) => prev.filter((r) => r.roomCode !== roomCode));
      }
    } catch (err) {
      alert(`Error: ${err.message}`);
    }
  }, [fetchRoomsByUserId, user?.id, searchResults]);

  const isYourRooms = activeTab === 'yourRooms';
  const displayRooms = isYourRooms ? rooms : (searchResults || []);
  const isLoading = isYourRooms ? loading : searchLoading;
  const displayError = isYourRooms ? error : searchError;

  return (
    <div className="dashboard">
      <header className="dashboard__header">
        <div className="dashboard__header-actions">
          <button
            className="dashboard__new-game"
            type="button"
            onClick={() => navigate('/create-room')}
          >
            New Game
          </button>
          <button
            className="dashboard__enter-code"
            type="button"
            onClick={() => navigate('/enter-code')}
          >
            Enter by Code
          </button>
        </div>
      </header>

      <div className="dashboard__tabs">
        <button
          className={`dashboard__tab${isYourRooms ? ' dashboard__tab--active' : ''}`}
          type="button"
          onClick={() => setActiveTab('yourRooms')}
        >
          Your Games
        </button>
        <button
          className={`dashboard__tab${!isYourRooms ? ' dashboard__tab--active' : ''}`}
          type="button"
          onClick={() => setActiveTab('search')}
        >
          Search
        </button>
      </div>

      {!isYourRooms && (
        <div className="dashboard__search-wrapper">
          <SearchGameRoomBar
            onSearch={handleSearch}
            value={searchQuery}
            onChange={setSearchQuery}
          />
        </div>
      )}

      <section className="dashboard__section">
        <h2 className="dashboard__section-title">
          {isYourRooms ? 'Your Games' : 'Search Results'}
        </h2>

        {isLoading && (
          <p className="dashboard__loading">Loading...</p>
        )}

        {!isLoading && displayError && (
          <p className="dashboard__error">{displayError}</p>
        )}

        {!isLoading && !displayError && displayRooms.length === 0 && (
          <div className="dashboard__empty">
            <p className="dashboard__empty-message">
              {isYourRooms
                ? 'You are not currently participating in any games.'
                : searchResults === null
                ? 'Enter a name above and press Search.'
                : 'No rooms match your search.'}
            </p>
            {isYourRooms && (
              <p className="dashboard__empty-subtext">
                Create a new game or search for existing ones to join!
              </p>
            )}
          </div>
        )}

        {!isLoading && !displayError && displayRooms.length > 0 && (
          <GameRoomList
            rooms={displayRooms}
            onJoin={handleJoin}
            onLeave={handleLeave}
            onEnd={handleEnd}
            currentUserId={user?.id}
          />
        )}
      </section>
    </div>
  );
}

export default DashboardView;
