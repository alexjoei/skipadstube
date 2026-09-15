const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
function run(skip = true, covered = false) {
  let listener, poll, requests = 0;
  const button = { textContent: 'Skip', getAttribute: () => null, getClientRects: () => [{}],
    getBoundingClientRect: () => ({ left: 10, top: 20, width: 80, height: 40 }), contains: el => el === button };
  const root = { classList: { contains: () => true }, querySelectorAll: () => [button] };
  vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../content.js'), 'utf8'), {
    document: { visibilityState: 'visible', documentElement: {}, querySelector: s => s === '#movie_player' ? root : { muted: false }, elementFromPoint: () => covered ? {} : button },
    chrome: { runtime: { onMessage: { addListener: fn => { listener = fn; } }, sendMessage: async () => { requests++; return { sent: true }; } },
      storage: { sync: { get: (_, fn) => fn({ skipAds: skip, muteAds: true, softChimes: false }) }, onChanged: { addListener() {} } } },
    getComputedStyle: () => ({ display: 'block', visibility: 'visible', opacity: '1' }),
    MutationObserver: class { observe() {} }, setInterval: fn => { poll = fn; }
  });
  return { requests: () => requests, poll, message(type) { let result; listener({ type }, {}, value => { result = value; }); return result; } };
}
test('browser input is the default without any stored mode preference', async () => {
  const app = run();
  assert.equal(app.requests(), 1);
  await new Promise(setImmediate);
  app.poll();
  assert.equal(app.message('skipadstube-status').clickAttempts, 1);
});
test('skip setting prevents input requests and coordinates', () => {
  const app = run(false);
  assert.equal(app.requests(), 0);
  assert.equal(app.message('skipadstube-point'), null);
});
test('coordinates are checked against overlays before browser input', () => {
  assert.equal(run(true, true).message('skipadstube-point'), null);
  const point = run().message('skipadstube-point');
  assert.equal(point.x, 50);
  assert.equal(point.y, 40);
});
