import { useMemo, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import './Header.css';
import { isAuthenticated } from '../../utils/auth';

function Header() {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const location = useLocation();
  const authenticated = isAuthenticated();

  const links = useMemo(
    () =>
      authenticated
        ? [
            { to: '/dashboard', label: 'Dashboard' },
            { to: '/create-room', label: 'Create room' },
            { to: '/enter-code', label: 'Enter code' },
            { to: '/profile', label: 'Profile' },
          ]
        : [
            { to: '/login', label: 'Login' },
            { to: '/register', label: 'Register' },
          ],
    [authenticated]
  );

  const toggleMenu = () => {
    setIsMenuOpen((previous) => !previous);
  };

  const closeMenu = () => {
    setIsMenuOpen(false);
  };

  if (location.pathname === '/') {
    return null;
  }

  return (
    <header className="header">
      <div className="header__content">
        <button
          className="header__menu-button"
          type="button"
          aria-label="Open menu"
          aria-expanded={isMenuOpen}
          aria-controls="mobile-navigation"
          onClick={toggleMenu}
        >
          &#9776;
        </button>
        <Link className="header__logo" to="/" onClick={closeMenu}>
          eMafia
        </Link>
        <nav className="header__desktop-nav" aria-label="Main navigation">
          {links.map((link) => (
            <Link key={link.to} className="header__link" to={link.to} onClick={closeMenu}>
              {link.label}
            </Link>
          ))}
        </nav>
      </div>
      {isMenuOpen && (
        <nav id="mobile-navigation" className="header__mobile-nav" aria-label="Mobile navigation">
          {links.map((link) => (
            <Link key={link.to} className="header__mobile-link" to={link.to} onClick={closeMenu}>
              {link.label}
            </Link>
          ))}
        </nav>
      )}
    </header>
  );
}

export default Header;
