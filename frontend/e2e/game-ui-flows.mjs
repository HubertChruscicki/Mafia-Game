/**
 * Browser E2E tests for core game UI flows.
 * Run from frontend/: npm run test:e2e
 * Headed: npm run test:e2e:headed
 */
import { chromium } from 'playwright';

const BASE = process.env.E2E_BASE_URL || 'http://localhost:3000';
const headed = process.argv.includes('--headed');
const suffix = Date.now();
const email = `e2e_${suffix}@test.com`;
const password = 'Test1234!';
const username = `e2e_${suffix}`;
const roomName = `E2E Room ${suffix}`;

const results = [];

function record(name, ok, detail = '') {
  results.push({ name, ok, detail });
  const mark = ok ? 'PASS' : 'FAIL';
  console.log(`[${mark}] ${name}${detail ? ` — ${detail}` : ''}`);
}

async function registerAndLogin(page) {
  await page.goto(`${BASE}/register`);
  await page.fill('input[name="username"]', username);
  await page.fill('input[name="email"]', email);
  await page.fill('input[name="password"]', password);
  await page.fill('input[name="confirmPassword"]', password);
  await page.getByRole('button', { name: /utworz konto/i }).click();
  await page.waitForURL(/\/login/, { timeout: 15000 });
  await page.fill('input[name="email"]', email);
  await page.fill('input[name="password"]', password);
  await page.getByRole('button', { name: /zaloguj/i }).click();
  await page.waitForURL(/\/dashboard/, { timeout: 15000 });
}

async function main() {
  const browser = await chromium.launch({ headless: !headed, slowMo: headed ? 80 : 0 });
  const context = await browser.newContext();
  const page = await context.newPage();
  let roomCode = '';

  try {
    await page.goto(`${BASE}/`);
    const enterLink = page.getByRole('link', { name: /wpisz kod pokoju/i });
    const href = await enterLink.getAttribute('href');
    record('StartView links to enter-code', href === '/enter-code', href || 'no href');

    await registerAndLogin(page);
    record('Register and reach dashboard', page.url().includes('/dashboard'));

    await page.getByRole('button', { name: /new game/i }).click();
    await page.waitForURL(/\/create-room/);
    await page.getByPlaceholder(/room name/i).fill(roomName);
    await page.getByRole('button', { name: /create room/i }).click();
    await page.waitForURL(/\/game-room\//, { timeout: 15000 });
    await page.waitForSelector('.game-room__share', { timeout: 10000 });
    roomCode = page.url().split('/game-room/')[1];
    record('Create room navigates to lobby', !!roomCode, roomCode);

    record('Lobby shows QR code', (await page.locator('.game-room__qr svg').count()) > 0);
    record('Lobby has copy join link', await page.getByRole('button', { name: /copy join link/i }).isVisible());

    await page.goto(`${BASE}/dashboard`);
    await page.waitForTimeout(2000);
    const hasEnter = await page.getByRole('button', { name: /enter room/i }).first().isVisible().catch(() => false);
    record('Dashboard shows Enter Room for own room', hasEnter, hasEnter ? 'Enter visible' : 'missing');

    await page.getByRole('button', { name: /^search$/i }).click();
    await page.locator('.search-bar__input').fill(roomName);
    await page.locator('.search-bar__button').click();
    await page.waitForTimeout(2000);
    record(
      'Search returns room with status',
      (await page.getByText(roomName).isVisible()) && (await page.getByText(/^open$/i).first().isVisible().catch(() => false)),
      roomName
    );

    await page.goto(`${BASE}/profile`);
    await page.fill('input[name="newUsername"]', `e2e${suffix}`.slice(0, 20));
    await page.getByRole('button', { name: /update username/i }).click();
    await page.waitForTimeout(2000);
    record('Profile username update', await page.getByText(/username updated successfully/i).isVisible().catch(() => false));

    const guestContext = await browser.newContext();
    const guestPage = await guestContext.newPage();
    await guestPage.goto(`${BASE}/join/${roomCode}`);
    await guestPage.waitForURL(/\/login/, { timeout: 10000 });
    await guestPage.fill('input[name="email"]', email);
    await guestPage.fill('input[name="password"]', password);
    await guestPage.getByRole('button', { name: /zaloguj/i }).click();
    await guestPage.waitForTimeout(3000);
    record('Login preserves invite URL redirect', guestPage.url().includes(`/join/${roomCode}`), guestPage.url());
    await guestContext.close();

    await page.goto(`${BASE}/game-room/${roomCode}`);
    await page.waitForTimeout(1500);
    record('Host sees Start Game in lobby', await page.getByRole('button', { name: /start game/i }).isVisible().catch(() => false));

    await page.goto(`${BASE}/dashboard`);
    await page.waitForTimeout(1000);
    const card = page.locator('.game-room-item--clickable').first();
    if (await card.count()) {
      await card.click();
      await page.waitForTimeout(1000);
      record('Clickable room card navigates', page.url().includes('/game-room/') || page.url().includes('/game/'), page.url());
    } else {
      record('Clickable room card present', false);
    }

    if (roomCode) {
      const joinUser = async (label) => {
        const u = `${label}_${suffix}`;
        await fetch(`${BASE}/api/auth/register`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username: u, email: `${u}@test.com`, password }),
        });
        const loginRes = await fetch(`${BASE}/api/auth/login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email: `${u}@test.com`, password }),
        });
        const { token } = await loginRes.json();
        await fetch(`${BASE}/api/game_rooms/join/${roomCode}`, {
          method: 'POST',
          headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        });
      };
      await joinUser('p2');
      await joinUser('p3');

      await page.goto(`${BASE}/game-room/${roomCode}`);
      await page.waitForTimeout(1000);
      await page.getByRole('button', { name: /start game/i }).click();
      await page.waitForURL(/\/game\//, { timeout: 20000 });
      record('Game starts and navigates to game view', page.url().includes('/game/'), page.url());

      await page.getByRole('button', { name: /check my role/i }).click();
      await page.waitForTimeout(500);
      const roleVisible = await page.getByText(/your role/i).isVisible().catch(() => false);
      record('Check My Role reveals role temporarily', roleVisible);
      await page.waitForTimeout(3500);
      const roleHidden = !(await page.getByText(/your role/i).isVisible().catch(() => true));
      record('Role auto-hides after 3 seconds', roleHidden);
    }

  } catch (err) {
    console.error('E2E fatal error:', err.message);
    record('E2E run', false, err.message);
  } finally {
    await browser.close();
  }

  const failed = results.filter((r) => !r.ok);
  console.log('\n--- Summary ---');
  console.log(`Passed: ${results.length - failed.length}/${results.length}`);
  if (failed.length) {
    console.log('Failed:', failed.map((f) => f.name).join(', '));
    process.exit(1);
  }
}

main();
