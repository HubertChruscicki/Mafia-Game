# Mafia-Game (eMafia)

Aplikacja webowa do gry w Mafię online: rejestracja i logowanie, lobby pokoi, zaproszenia QR, rozgrywka wieloetapowa (noc/dzień, głosowania, timer, WebSocket), panel admina.

Monorepo: **React** (frontend) + **Spring Boot** (mikroserwisy) + **PostgreSQL**, **RabbitMQ**, **Docker Compose**.

---

## Spis treści

- [Architektura](#architektura)
- [Wymagania](#wymagania)
- [Szybki start (Docker — zalecane)](#szybki-start-docker--zalecane)
- [Tryb developerski](#tryb-developerski)
- [Porty i adresy](#porty-i-adresy)
- [Przepływ gry (skrót)](#przepływ-gry-skrót)
- [Testy](#testy)
- [Struktura repozytorium](#struktura-repozytorium)
- [Zmienne środowiskowe](#zmienne-środowiskowe)
- [Rozwiązywanie problemów](#rozwiązywanie-problemów)
- [Prezentacja / telefony w tej samej sieci](#prezentacja--telefony-w-tej-samej-sieci)

---

## Architektura

```
┌─────────────┐     HTTP/WS      ┌──────────────────────────────────────────┐
│   Browser   │ ───────────────► │  frontend (nginx / React)  :3000         │
│  (React)    │                  │       │                                  │
└─────────────┘                  │       ├── /api/auth, /api/users, /admin  ──► auth-service :8081
                                 │       ├── /api/* (rooms, games, voting)  ──► game-service  :8082
                                 │       └── /ws (SockJS/STOMP)             ──► game-service  :8082
                                 └──────────────────────────────────────────┘
                                              │                    │
                                              ▼                    ▼
                                        PostgreSQL :5432    RabbitMQ :5672
```

| Serwis | Odpowiedzialność |
|--------|------------------|
| **auth-service** | Rejestracja, logowanie, JWT, refresh tokeny, profil użytkownika, panel admina (API) |
| **game-service** | Pokoje, lobby, start gry, fazy, głosowania, orchestrator, WebSocket, RabbitMQ events |
| **gateway-service** | Spring Cloud Gateway (opcjonalny punkt wejścia `:8080`; w Dockerze frontend proxy w nginx) |
| **frontend** | SPA React; w produkcji statyczne pliki + nginx jako reverse proxy |

**Stack:** Java 21, Spring Boot 4, React 19, PostgreSQL 18, RabbitMQ 4, JWT, SockJS/STOMP.

---

## Wymagania

### Do uruchomienia całej aplikacji (Docker)

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Compose v2)
- ~4 GB wolnego RAM (wszystkie kontenery naraz)

### Do developmentu frontendu

- Node.js 20+
- npm

### Do testów backendu lokalnie (bez Dockera)

- JDK 21
- Maven 3.9+ (lub `./mvnw` w każdym module)

### Do testów E2E (Playwright)

- Działająca aplikacja pod `http://localhost:3000`
- Playwright instaluje się przez `npm install` w `frontend/`

---

## Szybki start (Docker — zalecane)

Z katalogu głównego repozytorium:

```powershell
docker compose up --build -d
```

Pierwsze uruchomienie trwa kilka minut (build Javy + React). Kolejne starty są szybsze.

**Sprawdzenie stanu:**

```powershell
docker compose ps
```

Wszystkie serwisy powinny być `healthy` / `running`.

**Aplikacja:**

| Co | URL |
|----|-----|
| Frontend | http://localhost:3000 |
| Auth Swagger | http://localhost:8081/swagger-ui/index.html |
| Game Swagger | http://localhost:8082/swagger-ui/index.html |
| RabbitMQ Management | http://localhost:15672 (guest / guest) |

**Zatrzymanie:**

```powershell
docker compose down
```

**Pełny rebuild (np. po zmianach w Dockerfile):**

```powershell
docker compose down
docker compose up --build -d
```

**Rebuild tylko frontendu:**

```powershell
docker compose build --no-cache frontend
docker compose up -d frontend
```

---

## Tryb developerski

### Wariant A — wszystko w Dockerze (najprostszy)

Jak w [Szybki start](#szybki-start-docker--zalecane). Edytujesz kod, potem rebuild odpowiedniego serwisu.

### Wariant B — frontend z hot reload

1. Uruchom backend + infrastrukturę:

```powershell
docker compose up -d mafia-db mafia-rabbitmq auth-service game-service
```

2. W drugim terminalu — frontend dev server:

```powershell
cd frontend
npm install
npm start
```

Domyślnie: http://localhost:3000. Proxy API jest w `frontend/src/setupProxy.js` (auth `:8081`, game `:8082`, WebSocket `/ws`).

> Nie uruchamiaj jednocześnie kontenera `frontend` i `npm start` na porcie 3000.

Inny port dev:

```powershell
$env:PORT="3001"
npm start
```

### Wariant C — backend bez Dockera (zaawansowany)

Wymaga lokalnego PostgreSQL i RabbitMQ (albo tylko infrastruktury z Compose):

```powershell
docker compose up -d mafia-db mafia-rabbitmq
```

Następnie w `backend/auth-service` i `backend/game-service`:

```powershell
cd backend/auth-service
.\mvnw.cmd spring-boot:run

cd backend/game-service
.\mvnw.cmd spring-boot:run
```

Gateway opcjonalnie: `backend/gateway-service`.

---

## Porty i adresy

| Port | Usługa |
|------|--------|
| 3000 | Frontend (nginx w Dockerze) |
| 8080 | Gateway (Spring Cloud Gateway) |
| 8081 | auth-service |
| 8082 | game-service |
| 5432 | PostgreSQL |
| 5672 | RabbitMQ (AMQP) |
| 15672 | RabbitMQ Management UI |

---

## Przepływ gry (skrót)

1. **Rejestracja / logowanie** → JWT w localStorage.
2. **Dashboard** — lista pokoi, wyszukiwarka, dołączanie.
3. **Utwórz pokój** — host ustawia nazwę i limit graczy; dostaje kod pokoju.
4. **Lobby** (`/game-room/:code`) — lista graczy, QR/link zaproszenia, ustawienia (liczba mafii, czas dyskusji), start gry (host).
5. **Gra** (`/game/:code`) — fazy noc/dzień, głosowania z timerem, WebSocket (wyniki, zmiana fazy, koniec gry).
6. **Rola** — przycisk „Check My Role” (auto-ukrycie po 3 s); inni gracze nie widzą Twojej roli na ekranie.
7. **Admin** (`/admin`) — zarządzanie użytkownikami (konto z flagą admin).

Główne trasy frontendu:

| Trasa | Opis |
|-------|------|
| `/` | Strona startowa |
| `/login`, `/register` | Auth |
| `/dashboard` | Lista pokoi |
| `/create-room` | Nowy pokój |
| `/enter-code` | Wejście kodem |
| `/join/:roomCode` | Dołączenie linkiem / QR |
| `/game-room/:roomCode` | Lobby |
| `/game/:roomCode` | Rozgrywka |
| `/profile` | Profil |
| `/admin` | Panel admina |

---

## Testy

### Backend — testy jednostkowe (Maven + JaCoCo)

Z katalogu modułu (zalecane wrapper Maven):

```powershell
# game-service (~64 testy)
cd backend/game-service
.\mvnw.cmd test

# auth-service
cd backend/auth-service
.\mvnw.cmd test

# gateway-service
cd backend/gateway-service
.\mvnw.cmd test
```

Raport pokrycia (po `.\mvnw.cmd test`):

```
backend/<moduł>/target/site/jacoco/index.html
```

**Docker (gdy brak Maven/JDK lokalnie):**

```powershell
docker run --rm -v "${PWD}/backend/game-service:/app" -w /app maven:3.9-eclipse-temurin-21 mvn test
```

### API — skrypt PowerShell (flow gry)

Wymaga działających `auth-service` i `game-service` (np. przez Docker):

```powershell
./scripts/test-game-flows.ps1
```

Skrypt rejestruje użytkowników, tworzy pokój, startuje grę, symuluje głosowania i sprawdza warunki wygranej.

### Frontend — testy E2E (Playwright)

Wymaga **całej** aplikacji na http://localhost:3000:

```powershell
cd frontend
npm install
npm run test:e2e
```

Tryb z widoczną przeglądarką:

```powershell
npm run test:e2e:headed
```

Inny adres bazowy:

```powershell
$env:E2E_BASE_URL="http://192.168.1.10:3000"
npm run test:e2e
```

### Frontend — testy jednostkowe (Jest / RTL)

```powershell
cd frontend
npm test
```

---

## Struktura repozytorium

```
Mafia-Game/
├── docker-compose.yaml      # Pełny stack (DB, RabbitMQ, backend, frontend)
├── backend/
│   ├── auth-service/        # Autentykacja, użytkownicy, admin API
│   ├── game-service/        # Pokoje, gra, głosowania, WebSocket
│   ├── gateway-service/     # API Gateway (opcjonalny)
│   └── pom.xml              # Parent Maven (JaCoCo plugin)
├── frontend/
│   ├── src/                 # React — widoki, komponenty, hooki
│   ├── e2e/                 # Playwright — testy UI
│   ├── nginx.conf           # Proxy produkcyjne (Docker)
│   └── setupProxy.js        # Proxy dev (npm start)
└── scripts/
    └── test-game-flows.ps1  # Testy API end-to-end
```

---

## Zmienne środowiskowe

Najczęściej używane w `docker-compose.yaml` (można nadpisać w `.env` w katalogu głównym):

| Zmienna | Domyślnie | Opis |
|---------|-----------|------|
| `SPRING_PROFILES_ACTIVE` | `dev` | Profil Spring (`dev` / `prod`) |
| `FRONTEND_URL` | `http://localhost:3000` | URL frontendu (auth / linki) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,...` | CORS dla gateway |
| `JWT_SECRET` | (dev default) | Klucz JWT — **zmień na produkcji** |
| `SPRING_TASK_SCHEDULING_ENABLED` | `true` | Timer głosowań w game-service |

---

## Rozwiązywanie problemów

| Problem | Rozwiązanie |
|---------|-------------|
| Port 3000 zajęty | Zatrzymaj drugi frontend (`docker compose stop frontend` lub zamknij `npm start`) |
| Frontend nie łączy się z API | Sprawdź `docker compose ps` — auth i game muszą być healthy |
| WebSocket nie działa | Upewnij się, że wchodzisz przez nginx (`:3000`), nie bezpośrednio na `:8082` |
| Stary build frontendu | `docker compose build --no-cache frontend && docker compose up -d frontend` |
| Błędy bazy po reinstalacji | `docker compose down -v` (usuwa wolumeny — **kasuje dane**) |
| Maven OOM na Windows | Użyj Dockera do testów lub `$env:MAVEN_OPTS="-Xmx512m"` |
| Logi JVM (`hs_err_pid*.log`) | Ignorowane przez git; można usunąć ręcznie |

Logi kontenerów:

```powershell
docker compose logs -f game-service
docker compose logs -f auth-service
docker compose logs -f frontend
```

---

## Prezentacja / telefony w tej samej sieci

1. Uruchom stack: `docker compose up -d`
2. Sprawdź IP komputera: `ipconfig` (Windows) — np. `192.168.1.50`
3. Na telefonach (ta sama WiFi / hotspot): **http://192.168.1.50:3000**
4. Host też musi otworzyć aplikację przez **IP**, nie `localhost` — inaczej QR w lobby będzie nieważny
5. Zezwól na port 3000 w Zaporze Windows (reguła inbound TCP)

---

## Licencja / autorzy

Projekt studencki — repozytorium: [HubertChruscicki/Mafia-Game](https://github.com/HubertChruscicki/Mafia-Game).
