import { useState } from 'react';
import FormMessage from '../../components/formMessage/FormMessage';
import useAuthSession from '../../hooks/useAuthSession';
import { apiFetch } from '../../services/authApi';
import './ProfileView.css';

function ProfileView() {
  const { storeUser } = useAuthSession();
  const [usernameData, setUsernameData] = useState({ newUsername: '' });
  const [emailData, setEmailData] = useState({ newEmail: '' });
  const [passwordData, setPasswordData] = useState({
    oldPassword: '',
    newPassword: '',
  });
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const handleInputChange = (setter) => (event) => {
    setter((previous) => ({
      ...previous,
      [event.target.name]: event.target.value,
    }));
    setMessage('');
    setError('');
  };

  const handleSubmit = async (path, data, successText, onSuccess) => {
    setMessage('');
    setError('');
    try {
      const response = await apiFetch(path, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });

      if (response.ok) {
        setMessage(successText);
        onSuccess?.(response);
      } else {
        const payload = await response.json().catch(() => ({}));
        setError(payload.message || `Failed to update profile`);
      }
    } catch (err) {
      setError(err.message || 'An error occurred while updating profile.');
    }
  };

  return (
    <div className="profile">
      <h1 className="profile__title">Your Profile</h1>

      <div className="profile__forms">
        <FormMessage type="success" message={message} />
        <FormMessage type="error" message={error} />

        <form
          className="profile__section"
          onSubmit={(event) => {
            event.preventDefault();
            handleSubmit(
              '/api/users/me/username',
              usernameData,
              'Username updated successfully!',
              async (response) => {
                const updated = await response.json();
                if (updated) storeUser(updated);
                setUsernameData({ newUsername: '' });
              }
            );
          }}
        >
          <h3 className="profile__section-title">Change Username</h3>
          <div className="profile__field">
            <input
              type="text"
              name="newUsername"
              placeholder="New username"
              value={usernameData.newUsername}
              onChange={handleInputChange(setUsernameData)}
              required
            />
          </div>
          <button className="profile__submit" type="submit">
            Update Username
          </button>
        </form>

        <form
          className="profile__section"
          onSubmit={(event) => {
            event.preventDefault();
            handleSubmit(
              '/api/users/me/email',
              emailData,
              'Email updated successfully!',
              async (response) => {
                const updated = await response.json();
                if (updated) storeUser(updated);
                setEmailData({ newEmail: '' });
              }
            );
          }}
        >
          <h3 className="profile__section-title">Change Email</h3>
          <div className="profile__field">
            <input
              type="email"
              name="newEmail"
              placeholder="example@mail.com"
              value={emailData.newEmail}
              onChange={handleInputChange(setEmailData)}
              required
            />
          </div>
          <button className="profile__submit" type="submit">
            Update Email
          </button>
        </form>

        <form
          className="profile__section"
          onSubmit={(event) => {
            event.preventDefault();
            handleSubmit(
              '/api/users/me/password',
              passwordData,
              'Password updated successfully!',
              () => setPasswordData({ oldPassword: '', newPassword: '' })
            );
          }}
        >
          <h3 className="profile__section-title">Change Password</h3>
          <div className="profile__field">
            <input
              type="password"
              name="oldPassword"
              placeholder="Current password"
              value={passwordData.oldPassword}
              onChange={handleInputChange(setPasswordData)}
              required
            />
          </div>
          <div className="profile__field">
            <input
              type="password"
              name="newPassword"
              placeholder="New password"
              value={passwordData.newPassword}
              onChange={handleInputChange(setPasswordData)}
              required
            />
          </div>
          <button className="profile__submit" type="submit">
            Update Password
          </button>
        </form>
      </div>
    </div>
  );
}

export default ProfileView;
