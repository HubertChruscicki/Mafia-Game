import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App';

test('renders main layout with start screen', () => {
  render(<App />);
  const titleElement = screen.getByRole('heading', { name: /eMafia/i });
  const navElement = screen.getByRole('navigation', { name: /main navigation/i });
  const footerElement = screen.getByRole('contentinfo');
  expect(titleElement).toBeInTheDocument();
  expect(navElement).toBeInTheDocument();
  expect(footerElement).toBeInTheDocument();
});

test('toggles mobile menu links in header', async () => {
  render(<App />);

  const menuButton = screen.getByRole('button', { name: /open menu/i });
  await userEvent.click(menuButton);
  const mobileNav = screen.getByRole('navigation', { name: /mobile navigation/i });
  expect(mobileNav).toBeInTheDocument();

  await userEvent.click(within(mobileNav).getByRole('link', { name: /dashboard/i }));
  expect(screen.queryByRole('navigation', { name: /mobile navigation/i })).not.toBeInTheDocument();
});
