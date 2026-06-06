import { useEffect, useRef, useState } from 'react';
import { apiFetch } from '../../../services/authApi';
import './RolePanel.css';

const ROLE_HIDE_MS = 3000;

function RolePanel({ roomCode }) {
  const [loading, setLoading] = useState(false);
  const [isRoleVisible, setIsRoleVisible] = useState(false);
  const [roleData, setRoleData] = useState(null);
  const [error, setError] = useState('');
  const hideTimerRef = useRef(null);

  useEffect(() => {
    return () => {
      if (hideTimerRef.current) {
        clearTimeout(hideTimerRef.current);
      }
    };
  }, []);

  const handleShowRole = async () => {
    if (isRoleVisible) {
      setIsRoleVisible(false);
      if (hideTimerRef.current) {
        clearTimeout(hideTimerRef.current);
      }
      return;
    }

    setLoading(true);
    setError('');
    try {
      const res = await apiFetch(`/api/games/rooms/${roomCode}/me/role`);
      if (!res.ok) {
        throw new Error('Failed to fetch role');
      }
      const data = await res.json();
      setRoleData(data);
      setIsRoleVisible(true);

      hideTimerRef.current = setTimeout(() => {
        setIsRoleVisible(false);
      }, ROLE_HIDE_MS);
    } catch (err) {
      console.error('Failed to fetch role:', err);
      setError('Failed to fetch role');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="role-panel">
      <button
        type="button"
        className="role-panel__check-btn"
        onClick={handleShowRole}
        disabled={loading}
      >
        {loading ? 'Loading...' : isRoleVisible ? '🔒 Hide Role' : '🔓 Check My Role'}
      </button>

      {error && <p className="role-panel__error">{error}</p>}

      {isRoleVisible && roleData && (
        <div className="role-panel__card">
          <div className="role-panel__icon">
            {roleData.role === 'MAFIA' ? '🎭' : '👤'}
          </div>
          <div className="role-panel__info">
            <h3 className="role-panel__title">Your Role</h3>
            <p className="role-panel__name">{roleData.role}</p>
            {roleData.isAlive === false && (
              <p className="role-panel__dead">💀 Eliminated</p>
            )}
          </div>
          <p className="role-panel__hint">Auto-hides in 3 seconds</p>
        </div>
      )}
    </div>
  );
}

export default RolePanel;
