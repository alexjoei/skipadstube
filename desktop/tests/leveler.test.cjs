const { test } = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const path = require('node:path');

function run({ medias, enabled = true }) {
  const created = [];
  const param = () => ({ value: 0 });
  class FakeContext {
    constructor() { this.state = 'running'; this.destination = {}; }
    createMediaElementSource() { created.push('source'); return { connect() {} }; }
    createDynamicsCompressor() { return { threshold: param(), knee: param(), ratio: param(), attack: param(), release: param(), connect() {} }; }
    createGain() { return { gain: param(), connect() {} }; }
    resume() {}
  }
  let onChanged;
  const context = vm.createContext({
    URL, AudioContext: FakeContext, location: { href: 'https://www.ivoox.com/x', origin: 'https://www.ivoox.com' },
    document: { querySelectorAll: () => medias, documentElement: {} },
    MutationObserver: class { observe() {} },
    setInterval() {},
    chrome: { storage: { sync: { get: (_, cb) => cb({ levelVolume: enabled }) }, onChanged: { addListener: fn => { onChanged = fn; } } } }
  });
  vm.runInContext(fs.readFileSync(path.join(__dirname, '../leveler.js'), 'utf8'), context);
  return { created, onChanged: () => onChanged };
}
const media = (currentSrc, crossOrigin = null) => ({ currentSrc, crossOrigin, addEventListener() {} });

test('attaches to same-origin and blob media', () => {
  const { created } = run({ medias: [media('https://www.ivoox.com/a.mp3'), media('blob:https://www.ivoox.com/1')] });
  assert.equal(created.length, 2);
});

test('skips cross-origin media without CORS so playback is never silenced', () => {
  const { created } = run({ medias: [media('https://cdn.example.com/a.mp3')] });
  assert.equal(created.length, 0);
});

test('attaches to cross-origin media that opted in to CORS', () => {
  const { created } = run({ medias: [media('https://cdn.example.com/a.mp3', 'anonymous')] });
  assert.equal(created.length, 1);
});
