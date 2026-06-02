import { useCallback, useEffect, useState } from 'react';
import { apiFetch } from '../../services/authApi';
import './UserManagementTable.css';

function UserManagementTable() {
  const [users, setUsers] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const fetchUsers = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      const response = await apiFetch('/api/admin/users');
      if (!response.ok) {
        const payload = await response.json().catch(() => ({}));
        throw new Error(payload.message || 'Failed to fetch users');
      }
      const data = await response.json();
      setUsers(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Failed to fetch users');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleDeleteUser = async (userId) => {
    if (!window.confirm('Are you sure you want to delete this user?')) return;
    setError('');
    try {
      const response = await apiFetch(`/api/admin/users/${userId}`, { method: 'DELETE' });
      if (!response.ok) {
        const payload = await response.json().catch(() => ({}));
        throw new Error(payload.message || 'Failed to delete user');
      }
      setUsers((prev) => prev.filter((u) => u.id !== userId));
    } catch (err) {
      setError(err.message || 'Failed to delete user');
    }
  };

  const handleToggleAdmin = async (userId, makeAdmin) => {
    setError('');
    try {
      const response = await apiFetch(`/api/admin/users/${userId}/admin`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ admin: makeAdmin }),
      });
      if (!response.ok) {
        const payload = await response.json().catch(() => ({}));
        throw new Error(payload.message || 'Failed to update admin flag');
      }
      const updated = await response.json();
      setUsers((prev) => prev.map((u) => (u.id === userId ? updated : u)));
    } catch (err) {
      setError(err.message || 'Failed to update admin flag');
    }
  };

  if (isLoading && users.length === 0) {
    return <p className="user-mgmt__loading">Loading users...</p>;
  }

  return (
    <div className="user-mgmt">
      {error && <p className="user-mgmt__error">{error}</p>}
      <button
        type="button"
        className="user-mgmt__refresh"
        onClick={fetchUsers}
        disabled={isLoading}
      >
        {isLoading ? 'Refreshing...' : 'Refresh Users'}
      </button>
      <div className="user-mgmt__table-wrap">
        <table className="user-mgmt__table">
          <thead>
            <tr>
              <th>Username</th>
              <th>Email</th>
              <th>Admin</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td>{u.username}</td>
                <td>{u.email}</td>
                <td>{u.admin ? 'Yes' : 'No'}</td>
                <td>{u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '—'}</td>
                <td className="user-mgmt__actions">
                  <button
                    type="button"
                    className="user-mgmt__btn user-mgmt__btn--toggle"
                    onClick={() => handleToggleAdmin(u.id, !u.admin)}
                  >
                    {u.admin ? 'Revoke Admin' : 'Make Admin'}
                  </button>
                  <button
                    type="button"
                    className="user-mgmt__btn user-mgmt__btn--delete"
                    onClick={() => handleDeleteUser(u.id)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default UserManagementTable;
