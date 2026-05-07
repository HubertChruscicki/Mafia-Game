import { useState } from 'react';
import FormMessage from '../../components/formMessage/FormMessage';
import './ProfileView.css';

function ProfileView() {
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

  const handleSubmit = async (endpoint, data, successText) => {
    setMessage('');
    setError('');
    try {
      const token = localStorage.getItem('token');
      const apiBase = process.env.REACT_APP_API_BASE_URL;
      if (!apiBase) {
        setMessage(successText);
        return;
      }
      const response = await fetch(`${apiBase}/api/users/profile/${endpoint}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(data),
      });

      if (response.ok) {
        setMessage(successText);
        if (endpoint === 'username') setUsernameData({ newUsername: '' });
        if (endpoint === 'email') setEmailData({ newEmail: '' });
        if (endpoint === 'password')
          setPasswordData({ oldPassword: '', newPassword: '' });
      } else {
        const errorText = await response.text();
        setError(errorText || `Failed to update ${endpoint}`);
      }
    } catch (err) {
      setError(`An error occurred while updating ${endpoint}.`);
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
            handleSubmit('username', usernameData, 'Username updated successfully!');
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
            handleSubmit('email', emailData, 'Email updated successfully!');
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
            handleSubmit('password', passwordData, 'Password updated successfully!');
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
