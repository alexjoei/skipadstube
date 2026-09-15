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
  let clickAttempts = 0;
  let lastClickAt = 0;
  let status = { ad: false, buttonFound: false, skipEnabled: true, clickAttempts: 0 };
  chrome.runtime.onMessage.addListener((message, sender, respond) => {
    if (message.type === 'skipadstube-status') respond({ ...status, version: '0.1.2' });
  });

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
    if (!element || element.disabled || element.getAttribute('aria-disabled') === 'true' ||
        element.getClientRects().length === 0) return false;
    for (let node = element; node; node = node.parentElement) {
      const style = getComputedStyle(node);
      if (node.hidden || node.getAttribute('aria-hidden') === 'true' ||
          style.display === 'none' || style.visibility === 'hidden' ||
          style.visibility === 'collapse' || style.opacity === '0') return false;
    }
    return true;
  }

  function findSkipButton(root, adDetected) {
    if (!root) return null;
    for (const selector of SKIP_SELECTORS) {
      for (const button of root.querySelectorAll(selector)) {
        if (visible(button)) return button;
      }
    }
    // Fallback for experiments where YouTube changes class names but keeps an accessible label.
    for (const button of root.querySelectorAll('button, [role="button"]')) {
      const labels = [button.textContent, button.getAttribute('aria-label')]
        .map(value => (value || '').toLowerCase().replace(/\s+/g, ' ').trim());
      const matches = labels.some(label => /\b(?:skip ads?|omitir anuncios?|saltar anuncios?)\b/.test(label) ||
        (adDetected && /^(?:skip|omitir|saltar)$/.test(label)));
      if (visible(button) && matches) {
        return button;
      }
    }
    return null;
  }

  function beginAd(media) {
    if (!adActive) {
      adActive = true;
      videoMutedBeforeAd = media ? media.muted : false;
      if (settings.softChimes) chime(true);
    }
    if (media && settings.muteAds) media.muted = true;
  }

  function endAd(media) {
    if (!adActive) return;
    if (media && settings.muteAds) media.muted = videoMutedBeforeAd;
    adActive = false;
    if (settings.softChimes) chime(false);
  }

  function tick() {
    const media = video();
    // An available ad-specific skip control is itself an ad signal. Do not
    // require a separate player class before attempting to click it.
    const adDetected = isAdPlaying();
    const skipButton = findSkipButton(player(), adDetected);
    if (adDetected || skipButton) beginAd(media); else endAd(media);
    // Bound retries when a click leaves the control visible. Attempts are not
    // counted as successful skips: only the user/player can confirm that.
    if (settings.skipAds && skipButton && Date.now() - lastClickAt >= 1000) {
      lastClickAt = Date.now();
      clickAttempts++;
      skipButton.click();
    }
    status = { ad: Boolean(adDetected || skipButton), buttonFound: Boolean(skipButton),
      skipEnabled: settings.skipAds, clickAttempts };
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
  tick();
})();
