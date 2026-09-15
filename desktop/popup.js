const DEFAULTS = { muteAds: true, skipAds: true, softChimes: true };
chrome.storage.sync.get(DEFAULTS, settings => {
  for (const key of Object.keys(DEFAULTS)) {
    const input = document.getElementById(key);
    input.checked = settings[key];
    input.addEventListener('change', () => chrome.storage.sync.set({ [key]: input.checked }));
  }
});

async function updateStatus() {
  const output = document.getElementById('status');
  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    if (!tab) throw new Error('No active tab');
    const status = await chrome.tabs.sendMessage(tab.id, { type: 'skipadstube-status' });
    if (!status) throw new Error('No response');
    output.textContent = `v${status.version} · ${status.skipEnabled ? 'Omitir activado' : 'Omitir desactivado'} · ` +
      `${status.ad ? 'Anuncio detectado' : 'Sin anuncio'} · ` +
      `${status.buttonFound ? 'Botón encontrado' : 'Botón no encontrado'} · Intentos de clic: ${status.clickAttempts}`;
  } catch (_) {
    output.textContent = 'Sin conexión. Abre YouTube y recarga la pestaña. Comprueba el acceso de la extensión al sitio.';
  }
}
updateStatus();
setInterval(updateStatus, 1000);
