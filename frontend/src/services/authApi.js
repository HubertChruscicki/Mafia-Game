import {
  clearSession,
  getRefreshToken,
  getToken,
  updateAccessToken,
} from './authStorage';

async function parseResponse(response, fallbackMessage) {
  const payload = await response.json().catch(() => ({}));

  if (!response.ok) {
    throw new Error(payload.message || fallbackMessage);
  }

  return payload;
}

export async function loginUser(credentials) {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(credentials),
  });

  return parseResponse(response, 'Nie udało się zalogować.');
}

export async function registerUser(registrationData) {
  const response = await fetch('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(registrationData),
  });

  return parseResponse(response, 'Nie udało się utworzyć konta.');
}

export async function refreshAccessToken() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    return null;
  }

  const response = await fetch('/api/auth/refresh', {
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
  const refreshToken = getRefreshToken();
  if (refreshToken) {
    try {
      await fetch('/api/auth/logout', {
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
  const response = await apiFetch('/api/users/me');
  if (!response.ok) {
    return null;
  }
  return response.json().catch(() => null);
}
