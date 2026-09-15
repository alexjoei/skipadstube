const busy = new Set();
const lastAttempt = new Map();
const youtube = url => {
  try { return new URL(url).origin === 'https://www.youtube.com'; } catch { return false; }
};

async function browserClick(sender) {
  const tabId = sender.tab?.id;
  if (!Number.isInteger(tabId) || sender.frameId !== 0 || !youtube(sender.url)) {
    return { error: 'Solicitud fuera de YouTube' };
  }
  if (busy.has(tabId) || Date.now() - (lastAttempt.get(tabId) || 0) < 2000) return { error: 'Esperando' };
  busy.add(tabId);
  lastAttempt.set(tabId, Date.now());
  const target = { tabId };
  let attached = false;
  try {
    const sync = await chrome.storage.sync.get({ skipAds: true });
    if (!sync.skipAds) return { error: 'Omitir automáticamente desactivado' };
    const tab = await chrome.tabs.get(tabId);
    if (!tab.active) return { error: 'Activa la pestaña de YouTube' };
    if (!tab.url) return { error: 'Falta acceso al sitio YouTube. Revisa los permisos de la extensión.' };
    if (!youtube(tab.url)) return { error: 'La pestaña ya no está en YouTube' };
    await chrome.debugger.attach(target, '1.3');
    attached = true;
    // Recompute after attach; never accept arbitrary coordinates in the request.
    const point = await chrome.tabs.sendMessage(tabId, { type: 'skipadstube-point' }, { frameId: 0 });
    if (!point || !Number.isFinite(point.x) || !Number.isFinite(point.y) || point.x < 0 || point.y < 0) {
      return { error: 'Botón no disponible' };
    }
    const current = await chrome.tabs.get(tabId);
    if (!current.active || current.url !== tab.url) return { error: 'La pestaña cambió' };
    const mouse = { x: point.x, y: point.y, button: 'left', clickCount: 1 };
    await chrome.debugger.sendCommand(target, 'Input.dispatchMouseEvent', { ...mouse, type: 'mousePressed', buttons: 1 });
    await chrome.debugger.sendCommand(target, 'Input.dispatchMouseEvent', { ...mouse, type: 'mouseReleased', buttons: 0 });
    return { sent: true };
  } catch (error) {
    return { error: error.message || String(error) };
  } finally {
    if (attached) {
      try { await chrome.debugger.detach(target); } catch { /* Tab closed or Chrome detached. */ }
    }
    busy.delete(tabId);
  }
}
chrome.runtime.onMessage.addListener((message, sender, respond) => {
  if (message.type !== 'skipadstube-browser-click') return;
  browserClick(sender).then(respond);
  return true;
});
chrome.tabs.onRemoved.addListener(tabId => lastAttempt.delete(tabId));
