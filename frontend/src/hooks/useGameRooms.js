import { useCallback, useState } from 'react';
import { apiFetch } from '../services/authApi';

function useGameRooms() {
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchRoomsByUserId = useCallback(async (userId) => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiFetch('/api/game_rooms/info', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(data.message || 'Failed to fetch user rooms');
      }
      setRooms(data.rooms || []);
    } catch (err) {
      console.error('Error fetching user rooms:', err);
      setError(err.message || 'Failed to fetch user rooms');
      setRooms([]);
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchRoomsByCode = useCallback(async (roomCode) => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiFetch('/api/game_rooms/info', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ roomCode }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(data.message || 'Failed to fetch room by code');
      }
      setRooms(data.rooms || []);
    } catch (err) {
      console.error('Error fetching room by code:', err);
      setError(err.message || 'Failed to fetch room by code');
      setRooms([]);
    } finally {
      setLoading(false);
    }
  }, []);

  const searchRooms = useCallback(async (name) => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiFetch(`/api/game_rooms/search?name=${encodeURIComponent(name)}`);
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(data.message || 'Failed to search rooms');
      }
      setRooms(Array.isArray(data) ? data : data.rooms || []);
    } catch (err) {
      console.error('Error searching rooms:', err);
      setError(err.message || 'Failed to search rooms');
      setRooms([]);
    } finally {
      setLoading(false);
    }
  }, []);

  const leaveRoom = useCallback(async (roomCode) => {
    try {
      const response = await apiFetch(`/api/game_rooms/leave/${roomCode}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
      });
      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.message || 'Failed to leave room');
      }
    } catch (err) {
      console.error('Error leaving room:', err);
      throw err;
    }
  }, []);

  const refreshRooms = useCallback(async (userId) => {
    if (userId) {
      await fetchRoomsByUserId(userId);
    }
  }, [fetchRoomsByUserId]);

  return {
    rooms,
    loading,
    error,
    fetchRoomsByUserId,
    fetchRoomsByCode,
    searchRooms,
    leaveRoom,
    refreshRooms,
  };
}

export default useGameRooms;
