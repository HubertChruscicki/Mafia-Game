import { useCallback, useEffect, useState } from 'react';
import { logoutUser } from '../services/authApi';
import {
  clearSession,
  getStoredUser,
  getToken,
  isTokenPresent,
  setSession,
  setStoredUser,
} from '../services/authStorage';

function useAuthSession() {
  const [authenticated, setAuthenticated] = useState(isTokenPresent());
  const [user, setUser] = useState(getStoredUser());

  useEffect(() => {
    const synchronizeState = () => {
      setAuthenticated(isTokenPresent());
      setUser(getStoredUser());
    };

    window.addEventListener('storage', synchronizeState);
    return () => {
      window.removeEventListener('storage', synchronizeState);
    };
  }, []);

  const storeSession = useCallback(({ token, refreshToken, expiresIn = 3600 }) => {
    setSession({
      token,
      refreshToken: refreshToken || 'dev-refresh-token',
      expiresInSeconds: expiresIn,
    });
    setAuthenticated(true);
  }, []);

  const storeUser = useCallback((nextUser) => {
    setStoredUser(nextUser);
    setUser(nextUser);
  }, []);

  const removeSession = useCallback(async () => {
    try {
      await logoutUser();
    } catch (error) {
      clearSession();
    }
    setAuthenticated(false);
    setUser(null);
  }, []);

  return {
    authenticated,
    token: getToken(),
    user,
    storeSession,
    storeUser,
    removeSession,
  };
}

export default useAuthSession;
