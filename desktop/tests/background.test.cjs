const { test } = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const path = require('node:path');
function setup({ enabled = true, point = { x: 40, y: 50 }, fail = false, active = true, missingUrl = false } = {}) {
  const calls = [];
  const context = vm.createContext({ URL, chrome: {
    runtime: { onMessage: { addListener() {} } },
    storage: { sync: { get: async () => ({ skipAds: enabled }) } },
    tabs: { get: async () => ({ active, url: missingUrl ? undefined : 'https://www.youtube.com/watch?v=test' }),
      sendMessage: async () => point, onRemoved: { addListener() {} } },
    debugger: {
      attach: async () => { calls.push('attach'); },
      detach: async () => { calls.push('detach'); },
      sendCommand: async (_, method, params) => { calls.push(params.type); if (fail) throw new Error('Input failed'); }
    }
  } });
  vm.runInContext(fs.readFileSync(path.join(__dirname, '../background.js'), 'utf8'), context);
  const send = (url = 'https://www.youtube.com/watch?v=test') => {
    context.sender = { tab: { id: 7 }, frameId: 0, url };
    return vm.runInContext('browserClick(sender)', context);
  };
  return { send, calls };
}
test('browser input presses, releases, and detaches', async () => {
  const { send, calls } = setup();
  assert.equal((await send()).sent, true);
  assert.deepEqual(calls, ['attach', 'mousePressed', 'mouseReleased', 'detach']);
});
test('does not attach when disabled, inactive, or outside YouTube', async () => {
  for (const options of [{ enabled: false }, { active: false }]) {
    const { send, calls } = setup(options);
    await send();
    assert.deepEqual(calls, []);
  }
  const { send, calls } = setup();
  await send('https://example.com');
  assert.deepEqual(calls, []);
});
test('missing button detaches without clicking', async () => {
  const { send, calls } = setup({ point: null });
  assert.ok((await send()).error);
  assert.deepEqual(calls, ['attach', 'detach']);
});
test('input failure still detaches and reports the error', async () => {
  const { send, calls } = setup({ fail: true });
  assert.equal((await send()).error, 'Input failed');
  assert.equal(calls.at(-1), 'detach');
});
test('withheld tab URL reports missing permission rather than inactive tab', async () => {
  const { send, calls } = setup({ missingUrl: true });
  assert.match((await send()).error, /Falta acceso/);
  assert.deepEqual(calls, []);
});
test('manifest grants URL visibility only for YouTube', () => {
  const manifest = JSON.parse(fs.readFileSync(path.join(__dirname, '../manifest.json'), 'utf8'));
  assert.deepEqual(manifest.host_permissions, ['https://www.youtube.com/*']);
  assert.equal(manifest.permissions.includes('tabs'), false);
});
