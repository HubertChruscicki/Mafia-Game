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
