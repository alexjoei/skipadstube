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
  // Best-effort label for ad_stats rows: reuses whatever on-screen ad text is already
  // visible, same as the Android app. Not confirmed to ever contain the advertiser name.
  const AD_LABEL_SELECTORS = ['.ytp-ad-text', '.ytp-ad-simple-ad-badge', '.ytp-ad-preview-text'];
  // Common player-chrome labels visible during an ad that are not the ad itself. Filters
  // candidates for the best-effort advertiserGuess field; necessarily incomplete.
  const AD_NOISE_LABELS = new Set(['mas', 'more', 'configuracion', 'settings', 'pantalla completa',
    'full screen', 'fullscreen', 'subtitulos', 'subtitles', 'captions', 'reproducir', 'play',
    'pausa', 'pause', 'silenciar', 'mute', 'activar sonido', 'unmute', 'siguiente', 'next',
    'anterior', 'previous', 'cerrar', 'close', 'suscribirse', 'subscribe', 'youtube', 'compartir', 'share']);
  // A gap this short or shorter between one ad ending and the next starting is treated as the
  // same ad break (a "pod") rather than a separate, unrelated ad later.
  const POD_GAP_MS = 3000;

  let settings = { ...DEFAULTS };
  let adActive = false;
  let videoMutedBeforeAd = false;
  let audioContext;
  let clickAttempts = 0;
  let lastClickAt = 0;
  let browserPending = false;
  let browserResult = '';
  let adStartAt = 0;
  let adSkipped = false;
  let adLabel = '';
  let advertiserGuess = '';
  let skippable = false;
  let skipAvailableAt = -1;
  let lastAdEndAt = -1;
  let podPosition = 0;
  // Accumulates while the video is actually playing (not paused) and no ad is showing; frozen
  // into contentMsSnapshot the moment the next ad starts, then reset to zero.
  let contentMs = 0;
  let lastContentTickAt = -1;
  let contentMsSnapshot = 0;
  let status = { ad: false, buttonFound: false, skipEnabled: true, clickAttempts: 0 };
  chrome.runtime.onMessage.addListener((message, sender, respond) => {
    if (message.type === 'skipadstube-status') respond({ ...status, version: '1.0.0', browserResult });
    if (message.type === 'skipadstube-point') {
      const button = settings.skipAds && document.visibilityState === 'visible' && findSkipButton(player(), isAdPlaying());
      if (!button) { respond(null); return; }
      const rect = button.getBoundingClientRect();
      const x = rect.left + rect.width / 2, y = rect.top + rect.height / 2;
      respond(button.contains(document.elementFromPoint(x, y)) ? { x, y } : null);
    }
  });

  chrome.storage.sync.get(DEFAULTS, values => { settings = values; tick(); });
  chrome.storage.onChanged.addListener((changes, area) => {
    if (area !== 'sync') return;
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

  function findAdLabel() {
    const root = player();
    if (!root || typeof root.querySelector !== 'function') return '';
    for (const selector of AD_LABEL_SELECTORS) {
      const element = root.querySelector(selector);
      const text = element && element.textContent && element.textContent.trim();
      if (text) return text;
    }
    return '';
  }

  function normalizeForNoiseCheck(value) {
    return (value || '').toString().normalize('NFD').replace(/[̀-ͯ]/g, '')
      .toLowerCase().replace(/\s+/g, ' ').trim();
  }

  function isAdNoise(value) {
    const normalized = normalizeForNoiseCheck(value);
    if (normalized.length < 2) return true;
    if (/^\d+(:\d{2})?$/.test(normalized)) return true;
    if (/^(anuncio|publicidad|ad)\s*[·•:–-]\s*\d+/.test(normalized)) return true;
    if (/^(skip|omitir|saltar)\b/.test(normalized)) return true;
    return AD_NOISE_LABELS.has(normalized);
  }

  // Best-effort: scans every visible element in the player, not just the known ad-signal
  // selectors, looking for any text that isn't player chrome or the countdown. Only kept by the
  // caller once an ad is confirmed playing, so an ordinary video's title/controls never leak in.
  // Noisy by nature; not confirmed to ever surface the advertiser's name.
  function findAdvertiserGuess() {
    const root = player();
    if (!root || typeof root.querySelectorAll !== 'function') return '';
    for (const element of root.querySelectorAll('*')) {
      const candidates = [element.textContent, element.getAttribute && element.getAttribute('aria-label')];
      for (const candidate of candidates) {
        const text = (candidate || '').toString().trim();
        if (text && text.length < 80 && !isAdNoise(text)) return text;
      }
    }
    return '';
  }

  function parseDeclaredSeconds(label) {
    if (!label) return null;
    const minutes = label.match(/(\d+):(\d{2})/);
    if (minutes) return Number(minutes[1]) * 60 + Number(minutes[2]);
    const seconds = label.match(/\b(\d{1,3})\b/);
    return seconds ? Number(seconds[1]) : null;
  }

  function beginAd(media) {
    if (!adActive) {
      adActive = true;
      adStartAt = Date.now();
      adSkipped = false;
      adLabel = '';
      advertiserGuess = '';
      skippable = false;
      skipAvailableAt = -1;
      podPosition = (lastAdEndAt >= 0 && adStartAt - lastAdEndAt <= POD_GAP_MS) ? podPosition + 1 : 1;
      contentMsSnapshot = contentMs;
      contentMs = 0;
      lastContentTickAt = -1;
      videoMutedBeforeAd = media ? media.muted : false;
      if (settings.softChimes) chime(true);
    }
    if (media && settings.muteAds) media.muted = true;
    if (!adLabel) adLabel = findAdLabel();
    if (!advertiserGuess) advertiserGuess = findAdvertiserGuess();
  }

  function endAd(media) {
    if (!adActive) return;
    if (media && settings.muteAds) media.muted = videoMutedBeforeAd;
    adActive = false;
    if (settings.softChimes) chime(false);
    const end = Date.now();
    const row = { start: adStartAt, end, durationMs: end - adStartAt,
      declaredSeconds: parseDeclaredSeconds(adLabel),
      timeToSkipMs: skipAvailableAt >= 0 ? skipAvailableAt - adStartAt : null,
      skippable, skipped: adSkipped, podPosition, contentMsBeforeAd: contentMsSnapshot,
      label: adLabel, advertiserGuess };
    lastAdEndAt = end;
    chrome.runtime.sendMessage({ type: 'skipadstube-ad-stat', row }).catch(() => {});
  }

  function tick() {
    const media = video();
    // An available ad-specific skip control is itself an ad signal. Do not
    // require a separate player class before attempting to click it.
    const adDetected = isAdPlaying();
    const skipButton = findSkipButton(player(), adDetected);
    if (adDetected || skipButton) beginAd(media); else endAd(media);
    if (adActive && skipButton) {
      skippable = true;
      if (skipAvailableAt < 0) skipAvailableAt = Date.now();
    }
    // Content-watched time: only counts while actually playing (not paused), so pauses and
    // backgrounded tabs never inflate "how much did I watch before this ad" below.
    if (!adActive) {
      if (media && !media.paused) {
        const now = Date.now();
        if (lastContentTickAt >= 0) contentMs += now - lastContentTickAt;
        lastContentTickAt = now;
      } else {
        lastContentTickAt = -1;
      }
    }
    // Bound retries when a click leaves the control visible. Attempts are not
    // counted as successful skips: only the user/player can confirm that.
    if (settings.skipAds && skipButton && Date.now() - lastClickAt >= 1000) {
      lastClickAt = Date.now();
        if (!browserPending) {
          browserPending = true;
          chrome.runtime.sendMessage({ type: 'skipadstube-browser-click' }).then(result => {
            if (result?.sent) { clickAttempts++; adSkipped = true; }
            browserResult = result?.sent ? 'Clic de navegador enviado' : (result?.error || 'Sin respuesta');
          }).catch(error => { browserResult = error.message; }).finally(() => { browserPending = false; });
        }
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
