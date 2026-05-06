import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App';

beforeEach(() => {
  localStorage.clear();
  window.history.pushState({}, '', '/');
});

test('renders start screen on public root route', () => {
  render(<App />);
  const titleElement = screen.getByRole('heading', { name: /eMafia/i });
  expect(titleElement).toBeInTheDocument();
  expect(screen.getByRole('link', { name: /zaloguj sie/i })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: /rejestracja/i })).toBeInTheDocument();
});

test('toggles mobile menu links in header', async () => {
  localStorage.setItem('token', 'test-token');
  window.history.pushState({}, '', '/dashboard');
  render(<App />);

  const menuButton = screen.getByRole('button', { name: /open menu/i });
  await userEvent.click(menuButton);
  const mobileNav = screen.getByRole('navigation', { name: /mobile navigation/i });
  expect(mobileNav).toBeInTheDocument();

  await userEvent.click(within(mobileNav).getByRole('link', { name: /profile/i }));
  expect(screen.queryByRole('navigation', { name: /mobile navigation/i })).not.toBeInTheDocument();
});

test('redirects unauthenticated user from private route to login', () => {
  window.history.pushState({}, '', '/dashboard');
  render(<App />);
  expect(screen.getByRole('heading', { name: /logowanie/i })).toBeInTheDocument();
});

test('redirects authenticated user from login to dashboard', () => {
  localStorage.setItem('token', 'test-token');
  window.history.pushState({}, '', '/login');
  render(<App />);
  expect(screen.getByRole('heading', { name: /dashboard/i })).toBeInTheDocument();
});

test('allows navigating from register to login with success message', async () => {
  window.history.pushState({}, '', '/register');
  render(<App />);

  await userEvent.type(screen.getByLabelText(/nazwa uzytkownika/i), 'player1');
  await userEvent.type(screen.getByLabelText(/^email$/i), 'player@example.com');
  await userEvent.type(screen.getByLabelText(/^haslo$/i), 'Secret123!');
  await userEvent.type(screen.getByLabelText(/powtorz haslo/i), 'Secret123!');
  await userEvent.click(screen.getByRole('button', { name: /utworz konto/i }));

  expect(await screen.findByRole('heading', { name: /logowanie/i })).toBeInTheDocument();
  expect(screen.getByText(/konto utworzone/i)).toBeInTheDocument();
});

test('logs in and redirects to dashboard', async () => {
  window.history.pushState({}, '', '/login');
  render(<App />);

  await userEvent.type(screen.getByLabelText(/^email$/i), 'player@example.com');
  await userEvent.type(screen.getByLabelText(/^haslo$/i), 'Secret123!');
  await userEvent.click(screen.getByRole('button', { name: /zaloguj/i }));

  expect(await screen.findByRole('heading', { name: /dashboard/i })).toBeInTheDocument();
});

test('logs out from header and returns to login', async () => {
  localStorage.setItem('token', 'test-token');
  window.history.pushState({}, '', '/dashboard');
  render(<App />);

  await userEvent.click(screen.getByRole('button', { name: /logout/i }));
  expect(screen.getByRole('heading', { name: /logowanie/i })).toBeInTheDocument();
});

test('shows validation message for invalid login email', async () => {
  window.history.pushState({}, '', '/login');
  render(<App />);

  await userEvent.type(screen.getByLabelText(/^email$/i), 'wrong-email');
  await userEvent.type(screen.getByLabelText(/^haslo$/i), 'Secret123!');
  await userEvent.click(screen.getByRole('button', { name: /zaloguj/i }));

  expect(screen.getByText(/podaj poprawny adres email/i)).toBeInTheDocument();
});
