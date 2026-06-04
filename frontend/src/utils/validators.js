export function getEmailError(email) {
  if (!email.trim()) {
    return 'Email jest wymagany.';
  }

  const pattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!pattern.test(email)) {
    return 'Podaj poprawny adres email.';
  }

  return '';
}

export function getPasswordError(password) {
  if (!password.trim()) {
    return 'Haslo jest wymagane.';
  }

  if (password.length < 8) {
    return 'Haslo musi miec co najmniej 8 znakow.';
  }

  return '';
}

export function getUsernameError(username) {
  if (!username.trim()) {
    return 'Nazwa uzytkownika jest wymagana.';
  }

  if (username.trim().length < 3) {
    return 'Nazwa uzytkownika musi miec minimum 3 znaki.';
  }

  return '';
}
