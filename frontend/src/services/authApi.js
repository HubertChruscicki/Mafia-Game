import {
  clearSession,
  getRefreshToken,
  getToken,
  updateAccessToken,
} from './authStorage';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;

async function parseResponse(response, fallbackMessage) {
  const payload = await response.json().catch(() => ({}));

  if (!response.ok) {
    throw new Error(payload.message || fallbackMessage);
  }

  return payload;
}

export async function loginUser(credentials) {
  if (!API_BASE_URL) {
    return {
      token: 'dev-auth-token',
      refreshToken: 'dev-refresh-token',
      expiresIn: 3600,
    };
  }

  const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(credentials),
  });

  return parseResponse(response, 'Nie udalo sie zalogowac.');
}

export async function registerUser(registrationData) {
  if (!API_BASE_URL) {
    return { ok: true };
  }

  const response = await fetch(`${API_BASE_URL}/api/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(registrationData),
  });

  return parseResponse(response, 'Nie udalo sie utworzyc konta.');
}

export async function refreshAccessToken() {
  if (!API_BASE_URL) {
    return null;
  }
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    return null;
  }

  const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  });

  if (!response.ok) {
    return null;
  }

  const payload = await response.json().catch(() => null);
  if (!payload || !payload.token) {
    return null;
  }

  updateAccessToken({
    token: payload.token,
    refreshToken: payload.refreshToken,
    expiresInSeconds: payload.expiresIn || 3600,
  });
  return payload;
}

export async function logoutUser() {
  if (!API_BASE_URL) {
    clearSession();
    return;
  }
  const refreshToken = getRefreshToken();
  if (refreshToken) {
    try {
      await fetch(`${API_BASE_URL}/api/auth/logout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      });
    } catch (error) {
      // best-effort: still clear local session below
    }
  }
  clearSession();
}

/**
 * fetch wrapper that attaches the bearer token and transparently retries
 * once after refreshing the access token on a 401 response.
 */
export async function apiFetch(input, init = {}) {
  const token = getToken();
  const headers = new Headers(init.headers || {});
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  const response = await fetch(input, { ...init, headers });
  if (response.status !== 401) {
    return response;
  }

  const refreshed = await refreshAccessToken();
  if (!refreshed) {
    clearSession();
    return response;
  }

  const retryHeaders = new Headers(init.headers || {});
  retryHeaders.set('Authorization', `Bearer ${refreshed.token}`);
  return fetch(input, { ...init, headers: retryHeaders });
}

export async function fetchCurrentUser() {
  if (!API_BASE_URL) {
    return null;
  }
  const response = await apiFetch(`${API_BASE_URL}/api/users/me`);
  if (!response.ok) {
    return null;
  }
  return response.json().catch(() => null);
}
