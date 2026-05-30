import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { apiFetch } from '../services/authApi';

function isUserInRoomList(players, userId) {
  if (!userId || !Array.isArray(players)) return false;
  return players.some((p) => String(p.userId) === String(userId));
}

/**
 * Keeps lobby in sync and navigates players to the game when it starts.
 * Uses WebSocket + HTTP polling so missed WS events still redirect correctly.
 */
export function useGameRoomLobby(
  roomCode,
  { currentUserId, onRoomUpdate, onRoomDeleted } = {}
) {
  const navigate = useNavigate();
  const navigatedRef = useRef(false);
  const userIdRef = useRef(currentUserId);

  useEffect(() => {
    userIdRef.current = currentUserId;
  }, [currentUserId]);

  const goToGame = () => {
    if (navigatedRef.current) return;
    navigatedRef.current = true;
    navigate(`/game/${roomCode}`);
  };

  const canEnterGame = (roomData, gameData) => {
    const userId = userIdRef.current;
    if (!userId) return false;
    if (roomData && isUserInRoomList(roomData.players, userId)) return true;
    if (gameData && isUserInRoomList(gameData.players, userId)) return true;
    return false;
  };

  const checkGameStarted = async () => {
    try {
      let roomData = null;
      const roomRes = await apiFetch(`/api/game_rooms/${roomCode}`);
      if (roomRes.ok) {
        roomData = await roomRes.json();
        onRoomUpdate?.(roomData);
      }

      const gameRes = await apiFetch(`/api/games/rooms/${roomCode}/active-game`);
      let gameData = null;
      if (gameRes.ok) {
        gameData = await gameRes.json();
      }

      if (!canEnterGame(roomData, gameData)) {
        return false;
      }

      if (roomData?.status === 'GAME_IN_PROGRESS' || gameData) {
        goToGame();
        return true;
      }
    } catch {
      // ignore transient errors
    }
    return false;
  };

  useEffect(() => {
    navigatedRef.current = false;
    if (!roomCode) return undefined;

    let client;
    try {
      client = new Client({
        webSocketFactory: () => new SockJS('/ws'),
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          client.subscribe(`/topic/game/${roomCode}/ready`, () => {
            checkGameStarted();
          });

          client.subscribe(`/topic/game/${roomCode}/updated`, (msg) => {
            try {
              const payload = JSON.parse(msg.body);
              onRoomUpdate?.(payload);
              if (payload.status === 'GAME_IN_PROGRESS') {
                checkGameStarted();
              }
            } catch {
              // ignore
            }
          });

          client.subscribe(`/topic/game/${roomCode}/phase/change`, () => {
            checkGameStarted();
          });

          client.subscribe(`/topic/game/${roomCode}/roomDeleted`, () => {
            onRoomDeleted?.();
          });
        },
        onStompError: (frame) => {
          console.warn('[Lobby WS]', frame.headers?.message || frame);
        },
        onWebSocketError: (event) => {
          console.warn('[Lobby WS] connection error', event);
        },
      });
      client.activate();
    } catch (err) {
      console.warn('[Lobby WS] failed to start', err);
    }

    checkGameStarted();
    const pollId = setInterval(checkGameStarted, 2000);

    return () => {
      clearInterval(pollId);
      try {
        client?.deactivate();
      } catch {
        // ignore
      }
    };
  }, [roomCode]); // eslint-disable-line react-hooks/exhaustive-deps

  return { checkGameStarted };
}

export default useGameRoomLobby;
