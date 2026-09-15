const DEFAULTS = { muteAds: true, skipAds: true, softChimes: true };
chrome.storage.sync.get(DEFAULTS, settings => {
  for (const key of Object.keys(DEFAULTS)) {
    const input = document.getElementById(key);
    input.checked = settings[key];
    input.addEventListener('change', () => chrome.storage.sync.set({ [key]: input.checked }));
  }
});
