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
