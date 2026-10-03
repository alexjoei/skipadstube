const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function run({ skipAds = true } = {}) {
  let poll;
  const messages = [];
  let adPresent = true;
  const adTextEl = { textContent: 'Anuncio - Ejemplo' };
  const button = { textContent: 'Skip', getAttribute: () => null, getClientRects: () => [{}],
    getBoundingClientRect: () => ({ left: 10, top: 20, width: 80, height: 40 }), contains: el => el === button };
  const root = {
    classList: { contains: () => adPresent },
    querySelectorAll: () => (adPresent ? [button] : []),
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

test('a finished ad is reported once with duration, skip flag and label', async () => {
  const app = run();
  await new Promise(setImmediate);
  app.messages.length = 0;
  app.setAdPresent(false);
  app.poll();
  await new Promise(setImmediate);
  const stats = app.messages.filter(m => m.type === 'skipadstube-ad-stat');
  assert.equal(stats.length, 1);
  assert.equal(stats[0].row.skipped, true);
  assert.equal(stats[0].row.label, 'Anuncio - Ejemplo');
  assert.ok(stats[0].row.durationMs >= 0);
});

test('an ad that is never clicked is reported with skipped false', async () => {
  const app = run({ skipAds: false });
  await new Promise(setImmediate);
  app.messages.length = 0;
  app.setAdPresent(false);
  app.poll();
  await new Promise(setImmediate);
  const stats = app.messages.filter(m => m.type === 'skipadstube-ad-stat');
  assert.equal(stats.length, 1);
  assert.equal(stats[0].row.skipped, false);
});

test('an ad that never ends never reports a row', async () => {
  const app = run();
  await new Promise(setImmediate);
  app.messages.length = 0;
  app.poll();
  await new Promise(setImmediate);
  assert.equal(app.messages.filter(m => m.type === 'skipadstube-ad-stat').length, 0);
});
