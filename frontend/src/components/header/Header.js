import { useMemo, useState } from 'react';
import './Header.css';

function Header() {
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  const links = useMemo(
    () => [
      { href: '/#dashboard', label: 'Dashboard' },
      { href: '/#rooms', label: 'Rooms' },
      { href: '/#profile', label: 'Profile' },
    ],
    []
  );

  const toggleMenu = () => {
    setIsMenuOpen((previous) => !previous);
  };

  const closeMenu = () => {
    setIsMenuOpen(false);
  };

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
        <span className="header__logo">eMafia</span>
        <nav className="header__desktop-nav" aria-label="Main navigation">
          {links.map((link) => (
            <a key={link.href} className="header__link" href={link.href}>
              {link.label}
            </a>
          ))}
        </nav>
      </div>
      {isMenuOpen && (
        <nav id="mobile-navigation" className="header__mobile-nav" aria-label="Mobile navigation">
          {links.map((link) => (
            <a key={link.href} className="header__mobile-link" href={link.href} onClick={closeMenu}>
              {link.label}
            </a>
          ))}
        </nav>
      )}
    </header>
  );
}

export default Header;
