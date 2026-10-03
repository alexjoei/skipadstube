const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function run({ skipAds = true, brandText = null } = {}) {
  let poll;
  const messages = [];
  let adPresent = true;
  const adTextEl = { textContent: 'Anuncio - Ejemplo' };
  const button = { textContent: 'Skip', getAttribute: () => null, getClientRects: () => [{}],
    getBoundingClientRect: () => ({ left: 10, top: 20, width: 80, height: 40 }), contains: el => el === button };
  const brandEl = { textContent: brandText, getAttribute: () => null };
  const root = {
    classList: { contains: () => adPresent },
    querySelectorAll: selector => {
      if (!adPresent) return [];
      if (selector === '*') return brandText ? [button, brandEl] : [button];
      return [button];
    },
    querySelector: selector => (adPresent && selector === '.ytp-ad-text' ? adTextEl : null)
  };
  const media = { muted: false };
  vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../content.js'), 'utf8'), {
    document: { visibilityState: 'visible', documentElement: {},
      querySelector: s => s === '#movie_player' ? root : media, elementFromPoint: () => button },
    chrome: {
      runtime: { onMessage: { addListener() {} },
        sendMessage: async message => { messages.push(message); return { sent: true }; } },
      storage: { sync: { get: (_, fn) => fn({ skipAds, muteAds: true, softChimes: false }) }, onChanged: { addListener() {} } }
    },
    getComputedStyle: () => ({ display: 'block', visibility: 'visible', opacity: '1' }),
    MutationObserver: class { observe() {} }, setInterval: fn => { poll = fn; }
  });
  return { messages, setAdPresent: value => { adPresent = value; }, poll };
}

function lastStat(app) {
  const stats = app.messages.filter(m => m.type === 'skipadstube-ad-stat');
  return stats.length ? stats[stats.length - 1].row : null;
}

test('a finished ad is reported once with duration, skip flag, label and declared seconds', async () => {
  const app = run();
  await new Promise(setImmediate);
  app.messages.length = 0;
  app.setAdPresent(false);
  app.poll();
  await new Promise(setImmediate);
  const row = lastStat(app);
  assert.ok(row);
  assert.equal(row.skipped, true);
  assert.equal(row.skippable, true);
  assert.equal(row.label, 'Anuncio - Ejemplo');
  assert.ok(row.durationMs >= 0);
  assert.equal(row.declaredSeconds, null);
  assert.ok(row.timeToSkipMs >= 0);
  assert.equal(row.podPosition, 1);
});

test('an ad that is never clicked is reported as skippable but not skipped', async () => {
  const app = run({ skipAds: false });
  await new Promise(setImmediate);
  app.messages.length = 0;
  app.setAdPresent(false);
  app.poll();
  await new Promise(setImmediate);
  const row = lastStat(app);
  assert.equal(row.skipped, false);
  assert.equal(row.skippable, true);
});

test('an ad that never ends never reports a row', async () => {
  const app = run();
  await new Promise(setImmediate);
  app.messages.length = 0;
  app.poll();
  await new Promise(setImmediate);
  assert.equal(app.messages.filter(m => m.type === 'skipadstube-ad-stat').length, 0);
});

test('a non-generic text in the player is captured as advertiserGuess', async () => {
  const app = run({ brandText: 'Visita ejemplo.com' });
  await new Promise(setImmediate);
  app.messages.length = 0;
  app.setAdPresent(false);
  app.poll();
  await new Promise(setImmediate);
  assert.equal(lastStat(app).advertiserGuess, 'Visita ejemplo.com');
});

test('a second ad right after the first shares the pod and increments position', async () => {
  const app = run();
  await new Promise(setImmediate);
  app.messages.length = 0;
  app.setAdPresent(false);
  app.poll();
  await new Promise(setImmediate);
  assert.equal(lastStat(app).podPosition, 1);
  app.setAdPresent(true);
  app.poll();
  await new Promise(setImmediate);
  app.setAdPresent(false);
  app.poll();
  await new Promise(setImmediate);
  assert.equal(lastStat(app).podPosition, 2);
});
