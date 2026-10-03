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
      `${status.buttonFound ? 'Botón encontrado' : 'Botón no encontrado'} · Intentos de clic: ${status.clickAttempts}` +
      (status.browserResult ? ` · ${status.browserResult}` : '');
  } catch (_) {
    output.textContent = 'Sin conexión. Abre YouTube y recarga la pestaña. Comprueba el acceso de la extensión al sitio.';
  }
}
updateStatus();
setInterval(updateStatus, 1000);

document.getElementById('downloadStats').addEventListener('click', async () => {
  const { adStats = [] } = await chrome.storage.local.get({ adStats: [] });
  const header = 'start,end,duration_ms,declared_seconds,time_to_skip_ms,skippable,skipped,pod_position,ad_label,advertiser_guess';
  const csvText = value => value ? `"${String(value).replace(/"/g, '""')}"` : '';
  const rows = adStats.map(row => [
    new Date(row.start).toISOString(),
    new Date(row.end).toISOString(),
    row.durationMs,
    row.declaredSeconds ?? '',
    row.timeToSkipMs ?? '',
    Boolean(row.skippable),
    Boolean(row.skipped),
    row.podPosition ?? '',
    csvText(row.label),
    csvText(row.advertiserGuess)
  ].join(','));
  const blob = new Blob([[header, ...rows].join('\n')], { type: 'text/csv' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'skipadstube-ad-stats.csv';
  link.click();
  URL.revokeObjectURL(url);
});
