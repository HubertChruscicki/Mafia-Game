# Mafia-Game

## Szybko: dev vs prod

- **Dev (lokalnie, domyslnie):** backend startuje z profilem `dev` (z `docker-compose.override.yaml`).
- **Prod:** profil nadpisujesz zmienna `SPRING_PROFILES_ACTIVE=prod`.
- **Lokalnie (dev):** masz porty `8080` (gateway), `8081` (auth), `8082` (game), `3000` (frontend).
- **Docelowo (hostowane osobno):** frontend i backend sa niezalezne; ruch API przez gateway.

## Backend - jak odpalic dev

```powershell
cd backend
docker compose up --build
```

- Gateway: `http://localhost:8080`
- Auth Swagger: `http://localhost:8081/swagger-ui/index.html`
- Game Swagger: `http://localhost:8082/swagger-ui/index.html`

## Backend - jak odpalic prod lokalnie (nadpisanie)

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="prod"
docker compose up --build
```

## Frontend - jak hostowany

- Docker frontendu buduje statyczne pliki React.
- Nginx serwuje je na porcie `80` w kontenerze.
- Na hoście mapowanie jest `3000:80`.

```powershell
cd frontend
docker compose up --build
```

## Frontend dev z hot reload (`npm`)

```powershell
cd frontend
npm install
npm start
```

- `npm start` = hot reload i szybki dev.
- Nie odpalaj jednoczesnie Docker fronta na tym samym porcie `3000`.
- Jak trzeba, uruchom npm na innym porcie:

```powershell
cd frontend
$env:PORT="3001"
npm start
```

