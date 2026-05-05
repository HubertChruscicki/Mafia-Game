import { render, screen } from '@testing-library/react';
import App from './App';

test('renders application start title', () => {
  render(<App />);
  const titleElement = screen.getByRole('heading', { name: /eMafia/i });
  expect(titleElement).toBeInTheDocument();
});
