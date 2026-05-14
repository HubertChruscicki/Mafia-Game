import { isTokenPresent } from '../services/authStorage';

export function isAuthenticated() {
  return isTokenPresent();
}
