import { useState } from 'react';
import { FaSearch } from 'react-icons/fa';
import './SearchGameRoomBar.css';

function SearchGameRoomBar({ onSearch, placeholder, value, onChange }) {
  const [internalValue, setInternalValue] = useState('');

  const controlled = value !== undefined && onChange !== undefined;
  const currentValue = controlled ? value : internalValue;

  const handleChange = (e) => {
    if (controlled) {
      onChange(e.target.value);
    } else {
      setInternalValue(e.target.value);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSearch(currentValue);
  };

  return (
    <form className="search-bar" onSubmit={handleSubmit}>
      <div className="search-bar__input-wrapper">
        <FaSearch className="search-bar__icon" />
        <input
          type="text"
          className="search-bar__input"
          value={currentValue}
          onChange={handleChange}
          placeholder={placeholder || 'Search games by name...'}
        />
      </div>
      <button type="submit" className="search-bar__button">
        Search
      </button>
    </form>
  );
}

export default SearchGameRoomBar;
