const TOKEN_KEY = 'token';
const REFRESH_TOKEN_KEY = 'refreshToken';
const EXPIRATION_KEY = 'tokenExpiration';

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function isTokenPresent() {
  return Boolean(getToken());
}

export function setSession({ token, refreshToken, expiresInSeconds = 3600 }) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  localStorage.setItem(
    EXPIRATION_KEY,
    String(Date.now() + expiresInSeconds * 1000)
  );
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(EXPIRATION_KEY);
}
