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
    chrome: { storage: {
      sync: { get: (_, callback) => callback({ muteAds: true, skipAds: skip, softChimes: false }) },
      onChanged: { addListener() {} }
    } },
    getComputedStyle: () => ({ display: 'block', visibility: 'visible', opacity: '1' }),
    MutationObserver: class { observe() {} },
    setInterval: callback => { poll = callback; }
  });
  return { media, poll };
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
