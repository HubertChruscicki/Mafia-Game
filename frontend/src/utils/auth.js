export function isAuthenticated() {
  return Boolean(localStorage.getItem('token'));
}
