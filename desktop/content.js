(() => {
  const DEFAULTS = { muteAds: true, skipAds: true, softChimes: true };
  const SKIP_SELECTORS = [
    '.ytp-skip-ad-button',
    '.ytp-ad-skip-button',
    '.ytp-ad-skip-button-modern',
    'button.ytp-ad-skip-button',
    '[id="skip-button"] button',
    'ytd-button-renderer#skip-button button'
  ];

  let settings = { ...DEFAULTS };
  let adActive = false;
  let videoMutedBeforeAd = false;
  let audioContext;

  chrome.storage.sync.get(DEFAULTS, values => { settings = values; tick(); });
  chrome.storage.onChanged.addListener(changes => {
    for (const [key, value] of Object.entries(changes)) settings[key] = value.newValue;
    tick();
  });

  function player() {
    return document.querySelector('#movie_player');
  }

  function video() {
    return document.querySelector('video.html5-main-video');
  }

  function isAdPlaying() {
    const root = player();
    return Boolean(root && (root.classList.contains('ad-showing') || root.classList.contains('ad-interrupting')));
  }

  function visible(element) {
    if (!element || element.disabled) return false;
    const style = getComputedStyle(element);
    return style.display !== 'none' && style.visibility !== 'hidden' && element.getClientRects().length > 0;
  }

  function skipNow() {
    if (!settings.skipAds) return;
    for (const selector of SKIP_SELECTORS) {
      const button = document.querySelector(selector);
      if (visible(button)) { button.click(); return; }
    }
    // Fallback for experiments where YouTube changes class names but keeps an accessible label.
    for (const button of document.querySelectorAll('button')) {
      const label = `${button.textContent || ''} ${button.getAttribute('aria-label') || ''}`.toLowerCase();
      if (visible(button) && /skip ad|skip ads|omitir anuncio|omitir anuncios|saltar anuncio/.test(label)) {
        button.click(); return;
      }
    }
  }

  function beginAd(media) {
    if (!adActive) {
      adActive = true;
      videoMutedBeforeAd = media ? media.muted : false;
      if (settings.softChimes) chime(true);
    }
    if (media && settings.muteAds) media.muted = true;
    skipNow();
  }

  function endAd(media) {
    if (!adActive) return;
    if (media && settings.muteAds) media.muted = videoMutedBeforeAd;
    adActive = false;
    if (settings.softChimes) chime(false);
  }

  function tick() {
    const media = video();
    if (isAdPlaying()) beginAd(media); else endAd(media);
  }

  function chime(start) {
    try {
      audioContext ||= new AudioContext();
      const now = audioContext.currentTime;
      const gain = audioContext.createGain();
      gain.gain.setValueAtTime(0.0001, now);
      gain.gain.exponentialRampToValueAtTime(0.055, now + 0.025);
      gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.30);
      gain.connect(audioContext.destination);
      [start ? 523.25 : 659.25, start ? 659.25 : 523.25].forEach((frequency, index) => {
        const oscillator = audioContext.createOscillator();
        oscillator.type = 'sine'; oscillator.frequency.value = frequency;
        oscillator.connect(gain);
        oscillator.start(now + index * 0.13); oscillator.stop(now + 0.18 + index * 0.13);
      });
    } catch (_) { /* The browser may block sound until the user interacts with the page. */ }
  }

  const observer = new MutationObserver(tick);
  observer.observe(document.documentElement, { attributes: true, childList: true, subtree: true });
  setInterval(tick, 300);
})();
