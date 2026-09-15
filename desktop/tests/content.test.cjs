const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, '../content.js'), 'utf8');
function button({ hidden = false, disabled = false, label = '', parent = null } = {}) {
  return {
    hidden, disabled, textContent: label, parentElement: parent, clicks: 0,
    getAttribute() { return null; },
    getClientRects() { return [{}]; },
    click() { this.clicks++; }
  };
}
function run({ buttons = [], fallback = [], ad = false, skip = true } = {}) {
  let poll;
  let onMessage;
  const media = { muted: false };
  const root = {
    classList: { contains: () => ad },
    querySelectorAll: selector => selector === '.ytp-skip-ad-button' ? buttons :
      selector === 'button, [role="button"]' ? fallback : []
  };
  vm.runInNewContext(source, {
    document: {
      documentElement: {},
      querySelector: selector => selector === '#movie_player' ? root : media
    },
    chrome: { runtime: { onMessage: { addListener(callback) { onMessage = callback; } } }, storage: {
      sync: { get: (_, callback) => callback({ muteAds: true, skipAds: skip, softChimes: false }) },
      onChanged: { addListener() {} }
    } },
    getComputedStyle: () => ({ display: 'block', visibility: 'visible', opacity: '1' }),
    MutationObserver: class { observe() {} },
    setInterval: callback => { poll = callback; }
  });
  return { media, poll, status() {
    let result;
    onMessage({ type: 'skipadstube-status' }, {}, value => { result = value; });
    return result;
  } };
}

test('clicks an available skip control without an ad-state class', () => {
  const target = button();
  const { media } = run({ buttons: [target] });
  assert.ok(target.clicks > 0);
  assert.equal(media.muted, true);
});
test('ignores a hidden first match and clicks the visible duplicate', () => {
  const hidden = button({ hidden: true });
  const target = button();
  run({ buttons: [hidden, target], ad: true });
  assert.equal(hidden.clicks, 0);
  assert.ok(target.clicks > 0);
});
test('rejects disabled controls and controls in hidden containers', () => {
  const disabled = button({ disabled: true });
  const hidden = button({ parent: button({ hidden: true }) });
  const { media } = run({ buttons: [disabled, hidden] });
  assert.equal(disabled.clicks + hidden.clicks, 0);
  assert.equal(media.muted, false);
});
test('recognizes Spanish ad-specific labels within the player', () => {
  const target = button({ label: 'Omitir anuncios' });
  run({ fallback: [target] });
  assert.ok(target.clicks > 0);
});
test('does not click unrelated player controls', () => {
  const target = button({ label: 'Skip chapter' });
  run({ fallback: [target], ad: true });
  assert.equal(target.clicks, 0);
});
test('honors the skip setting', () => {
  const target = button();
  run({ buttons: [target], skip: false });
  assert.equal(target.clicks, 0);
});
test('restores sound when the skip control disappears and no ad remains', () => {
  const buttons = [button()];
  const { media, poll } = run({ buttons });
  assert.equal(media.muted, true);
  buttons.length = 0;
  poll();
  assert.equal(media.muted, false);
});

test('recognizes short skip labels only during a detected ad', () => {
  for (const label of ['Skip', 'Omitir', 'Saltar']) {
    const target = button({ label });
    run({ fallback: [target] });
    assert.equal(target.clicks, 0);
    run({ fallback: [target], ad: true });
    assert.equal(target.clicks, 1);
  }
});
test('reports attempted clicks without claiming a successful skip and limits retries', () => {
  const target = button();
  const { poll, status } = run({ buttons: [target] });
  poll();
  poll();
  assert.equal(target.clicks, 1);
  assert.equal(status().buttonFound, true);
  assert.equal(status().clickAttempts, 1);
  assert.equal(status().ad, true);
});
test('status distinguishes missing buttons and disabled skipping', () => {
  assert.equal(run({ ad: true }).status().buttonFound, false);
  const state = run({ buttons: [button()], skip: false }).status();
  assert.equal(state.buttonFound, true);
  assert.equal(state.skipEnabled, false);
  assert.equal(state.clickAttempts, 0);
});
