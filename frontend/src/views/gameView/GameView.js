import { useEffect, useRef, useState, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { apiFetch } from '../../services/authApi';
import useAuthSession from '../../hooks/useAuthSession';
import VotingPhaseContainer from '../../components/game/voting/VotingPhaseContainer/VotingPhaseContainer';
import GameResultPanel from '../../components/game/GameResultPanel/GameResultPanel';
import RolePanel from '../../components/game/RolePanel/RolePanel';
import './GameView.css';

const VOTING_PHASES = new Set(['NIGHT_VOTE', 'DAY_VOTE']);

function isVotingPhase(phase) {
  return VOTING_PHASES.has(phase);
}

function normalizeElimination(data) {
  if (!data) return null;
  return {
    ...data,
    eliminatedUsername: data.eliminatedUsername || data.eliminated || null,
  };
}

function mapVotingSessionDto(data, prev = null) {
  if (!data) return null;
  const voteMap = data.voteMap || data.currentResults || {};
  const normalizedResults = {};
  Object.entries(voteMap).forEach(([key, value]) => {
    normalizedResults[String(key)] = value;
  });

  return {
    sessionId: data.sessionId,
    phase: data.phase,
    dayNumber: data.dayNumber,
    currentResults: normalizedResults,
    remainingTimeSeconds: data.remainingSeconds ?? data.remainingTimeSeconds,
    totalVoters: data.totalVoters,
    votesReceived: data.votesReceived,
    hasVoted: prev?.hasVoted === true,
    myVote: prev?.myVote ? String(prev.myVote) : null,
  };
}

function GameView() {
  const { roomCode } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthSession();

  const [gameId, setGameId] = useState(null);
  const [gamePhase, setGamePhase] = useState(null);
  const [discussionTimeSeconds, setDiscussionTimeSeconds] = useState(120);
  const [players, setPlayers] = useState([]);
  const [votingSession, setVotingSession] = useState(null);
  const [timerSeconds, setTimerSeconds] = useState(null);
  const [gameOverData, setGameOverData] = useState(null);
  const [eliminationResult, setEliminationResult] = useState(null);
  const [voteError, setVoteError] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const stompClient = useRef(null);
  const gameIdRef = useRef(null);
  const gamePhaseRef = useRef(null);
  const syncGameStateRef = useRef(null);

  const fetchCurrentVotingSession = useCallback(async (gId) => {
    if (!gId) return null;
    try {
      const res = await apiFetch(`/api/games/${gId}/voting/current`);
      if (res.status === 204 || !res.ok) {
        setVotingSession(null);
        if (isVotingPhase(gamePhaseRef.current)) {
          const gameRes = await apiFetch(`/api/games/${gId}`);
          if (gameRes.ok) {
            const gameData = await gameRes.json();
            const newPhase = gameData.phase;
            if (newPhase && newPhase !== gamePhaseRef.current) {
              setGamePhase(newPhase);
              gamePhaseRef.current = newPhase;
              setTimerSeconds(null);
              if (isVotingPhase(newPhase)) {
                return fetchCurrentVotingSession(gId);
              }
            }
          }
        } else {
          setTimerSeconds(null);
        }
        return null;
      }
      const data = await res.json();
      setVotingSession((prev) => mapVotingSessionDto(data, prev));
      if (data.remainingSeconds != null) {
        setTimerSeconds(data.remainingSeconds);
      }
      return data;
    } catch (err) {
      console.error('Failed to fetch voting session:', err);
      return null;
    }
  }, []);

  const fetchActiveGame = async () => {
    try {
      const res = await apiFetch(`/api/games/rooms/${roomCode}/active-game`);
      if (!res.ok) {
        if (res.status === 404) {
          navigate(`/game-room/${roomCode}`, { replace: true });
          return;
        }
        throw new Error('Failed to load game');
      }
      const data = await res.json();
      setGameId(data.gameId);
      gameIdRef.current = data.gameId;
      setPlayers(data.players || []);
      const phase = data.currentPhase || data.phase || null;
      setGamePhase(phase);
      gamePhaseRef.current = phase;

      if (phase === 'GAME_OVER') {
        setGameOverData({ winner: data.winnerTeam || 'UNKNOWN', players: data.players || [] });
      }
      if (isVotingPhase(phase)) {
        await fetchCurrentVotingSession(data.gameId);
      }
    } catch (err) {
      setError(err.message || 'Failed to load game');
    } finally {
      setLoading(false);
    }
  };

  const fetchRoomSettings = async () => {
    try {
      const res = await apiFetch(`/api/game_rooms/${roomCode}`);
      if (res.ok) {
        const data = await res.json();
        if (data.discussionTimeSeconds) {
          setDiscussionTimeSeconds(data.discussionTimeSeconds);
        }
      }
    } catch (err) {
      // non-fatal
    }
  };

  const handlePhaseChange = useCallback((phase) => {
    setGamePhase(phase);
    gamePhaseRef.current = phase;
    setVotingSession(null);
    setVoteError('');
    if (!isVotingPhase(phase)) {
      setTimerSeconds(null);
    }
    if (gameIdRef.current && isVotingPhase(phase)) {
      fetchCurrentVotingSession(gameIdRef.current);
    }
  }, [fetchCurrentVotingSession]);

  const syncGameState = useCallback(async () => {
    const gId = gameIdRef.current;
    if (!gId) return;
    try {
      const res = await apiFetch(`/api/games/${gId}`);
      if (!res.ok) return;
      const data = await res.json();
      const phase = data.phase;
      if (phase && phase !== gamePhaseRef.current) {
        handlePhaseChange(phase);
      } else if (!isVotingPhase(phase)) {
        setVotingSession(null);
        setTimerSeconds(null);
      } else {
        await fetchCurrentVotingSession(gId);
      }
    } catch (err) {
      console.error('Failed to sync game state:', err);
    }
  }, [handlePhaseChange, fetchCurrentVotingSession]);

  syncGameStateRef.current = syncGameState;

  const connectWebSocket = () => {
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        client.subscribe(`/topic/game/${roomCode}/phase/change`, (msg) => {
          const data = JSON.parse(msg.body);
          handlePhaseChange(data.phase || data.currentPhase);
        });

        client.subscribe(`/topic/game/${roomCode}/phase/result`, (msg) => {
          const data = JSON.parse(msg.body);
          setEliminationResult(normalizeElimination(data));
          if (data.eliminatedUserId) {
            setPlayers((prev) =>
              prev.map((p) =>
                String(p.userId) === String(data.eliminatedUserId)
                  ? { ...p, isAlive: false }
                  : p
              )
            );
          }
        });

        client.subscribe(`/topic/game/${roomCode}/voting`, (msg) => {
          const data = JSON.parse(msg.body);
          setVotingSession((prev) => mapVotingSessionDto(data, prev));
          if (data.remainingSeconds != null) {
            setTimerSeconds(data.remainingSeconds);
          }
        });

        client.subscribe(`/topic/game/${roomCode}/voting/timer`, (msg) => {
          const data = JSON.parse(msg.body);
          if (isVotingPhase(data.phase)) {
            setTimerSeconds(data.remainingSeconds);
          }
        });

        client.subscribe(`/topic/game/${roomCode}/voting/result`, (msg) => {
          const data = JSON.parse(msg.body);
          setEliminationResult(normalizeElimination(data));
          if (data.eliminatedUserId) {
            setPlayers((prev) =>
              prev.map((p) =>
                String(p.userId) === String(data.eliminatedUserId)
                  ? { ...p, isAlive: false }
                  : p
              )
            );
          }
          setVotingSession(null);
          setTimerSeconds(null);
        });

        client.subscribe(`/topic/game/${roomCode}/voting/expired`, () => {
          setVotingSession(null);
          setTimerSeconds(null);
          syncGameStateRef.current?.();
        });

        client.subscribe(`/topic/game/${roomCode}/gameOver`, (msg) => {
          const data = JSON.parse(msg.body);
          setGamePhase('GAME_OVER');
          gamePhaseRef.current = 'GAME_OVER';
          setGameOverData(data);
          setVotingSession(null);
          setTimerSeconds(null);
        });
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
      },
      onWebSocketError: (event) => {
        console.warn('[Game WS] connection error', event);
      },
    });

    client.activate();
    stompClient.current = client;
  };

  useEffect(() => {
    fetchActiveGame();
    fetchRoomSettings();
    connectWebSocket();

    return () => {
      if (stompClient.current) {
        stompClient.current.deactivate();
        stompClient.current = null;
      }
    };
  }, [roomCode]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    gameIdRef.current = gameId;
  }, [gameId]);

  useEffect(() => {
    gamePhaseRef.current = gamePhase;
  }, [gamePhase]);

  // HTTP fallback: keep voting session in sync when WS is down or slow
  useEffect(() => {
    if (!gameId || !isVotingPhase(gamePhase)) return undefined;
    fetchCurrentVotingSession(gameId);
    const pollId = setInterval(() => {
      fetchCurrentVotingSession(gameId);
    }, 3000);
    return () => clearInterval(pollId);
  }, [gameId, gamePhase, fetchCurrentVotingSession]);

  // When timer hits zero, poll until phase advances (timeout safety net)
  useEffect(() => {
    if (!gameId || !isVotingPhase(gamePhase) || timerSeconds == null) return undefined;
    if (timerSeconds > 0) return undefined;
    syncGameState();
    const pollId = setInterval(syncGameState, 2000);
    return () => clearInterval(pollId);
  }, [gameId, gamePhase, timerSeconds, syncGameState]);

  const handleVoteCast = async (targetUserId) => {
    setVoteError('');
    let session = votingSession;
    if (!session?.sessionId && gameId) {
      const fresh = await fetchCurrentVotingSession(gameId);
      if (fresh) {
        session = mapVotingSessionDto(fresh, votingSession);
      }
    }
    if (!session?.sessionId) {
      setVoteError('Voting session not ready yet. Please wait a moment.');
      return;
    }
    if (session.hasVoted) return;

    try {
      const res = await apiFetch(`/api/games/${gameId}/voting/vote`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          votingSessionId: session.sessionId,
          targetUserId: String(targetUserId),
        }),
      });
      const data = await res.json().catch(() => ({}));
      if (res.ok && data.success !== false) {
        setVotingSession((prev) =>
          prev
            ? { ...prev, hasVoted: true, myVote: String(targetUserId) }
            : { ...session, hasVoted: true, myVote: String(targetUserId) }
        );
      } else {
        setVoteError(data.message || 'Failed to cast vote');
      }
    } catch (err) {
      console.error('Failed to cast vote:', err);
      setVoteError(err.message || 'Failed to cast vote');
    }
  };

  const handleClearElimination = () => setEliminationResult(null);

  if (gameOverData) {
    return (
      <div className="game-view">
        <GameResultPanel
          winner={gameOverData.winner}
          players={gameOverData.players || players}
          onPlayAgain={() => navigate('/dashboard')}
        />
      </div>
    );
  }

  if (loading) {
    return (
      <div className="game-view game-view--loading">
        <div className="game-view__spinner" />
        <p>Loading game...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="game-view game-view--error">
        <h2>Error</h2>
        <p>{error}</p>
        <button
          className="game-view__back-btn"
          onClick={() => navigate(`/game-room/${roomCode}`)}
        >
          Back to Room
        </button>
      </div>
    );
  }

  return (
    <div className="game-view">
      <RolePanel roomCode={roomCode} />
      {voteError && <p className="game-view__vote-error">{voteError}</p>}
      <VotingPhaseContainer
        gameId={gameId}
        roomCode={roomCode}
        currentUser={user}
        players={players}
        gamePhase={gamePhase}
        votingSession={votingSession}
        timerSeconds={isVotingPhase(gamePhase) ? timerSeconds : null}
        discussionTimeSeconds={discussionTimeSeconds}
        onVoteCast={handleVoteCast}
        eliminationResult={eliminationResult}
        onClearElimination={handleClearElimination}
      />
    </div>
  );
}

export default GameView;
