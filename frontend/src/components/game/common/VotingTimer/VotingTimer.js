import { useEffect, useState } from 'react';
import './VotingTimer.css';

function VotingTimer({ remainingSeconds, totalSeconds }) {
  const [timeLeft, setTimeLeft] = useState(remainingSeconds || 0);

  useEffect(() => {
    if (remainingSeconds != null) {
      setTimeLeft(remainingSeconds);
    }
  }, [remainingSeconds]);

  useEffect(() => {
    if (timeLeft <= 0) return;
    const id = setInterval(() => {
      setTimeLeft((prev) => (prev <= 1 ? 0 : prev - 1));
    }, 1000);
    return () => clearInterval(id);
  }, [timeLeft]);

  const total = totalSeconds || 60;
  const pct = total > 0 ? Math.max(0, Math.min(100, (timeLeft / total) * 100)) : 0;
  const minutes = Math.floor(timeLeft / 60);
  const seconds = timeLeft % 60;

  const colorClass =
    timeLeft > 30
      ? 'voting-timer--green'
      : timeLeft > 10
      ? 'voting-timer--yellow'
      : 'voting-timer--red';

  return (
    <div className={`voting-timer ${colorClass}`}>
      <div className="voting-timer__bar-track">
        <div className="voting-timer__bar-fill" style={{ width: `${pct}%` }} />
      </div>
      <span className="voting-timer__text">
        {String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')} remaining
      </span>
    </div>
  );
}

export default VotingTimer;
