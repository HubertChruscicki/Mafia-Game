import { useEffect, useState } from 'react';
import { clearSession, getToken, isTokenPresent, setSession } from '../services/authStorage';

function useAuthSession() {
  const [authenticated, setAuthenticated] = useState(isTokenPresent());

  useEffect(() => {
    const synchronizeState = () => {
      setAuthenticated(isTokenPresent());
    };

    window.addEventListener('storage', synchronizeState);
    return () => {
      window.removeEventListener('storage', synchronizeState);
    };
  }, []);

  const storeSession = ({ token, refreshToken, expiresIn = 3600 }) => {
    setSession({
      token,
      refreshToken: refreshToken || 'dev-refresh-token',
      expiresInSeconds: expiresIn,
    });
    setAuthenticated(true);
  };

  const removeSession = () => {
    clearSession();
    setAuthenticated(false);
  };

  return {
    authenticated,
    token: getToken(),
    storeSession,
    removeSession,
  };
}

export default useAuthSession;
