// Volume leveler for audio/video elements (iVoox): compressor + limiter through Web Audio, so
// loud ads are squashed and quiet speech is lifted. Runs as its own content script.
(() => {
  const DEFAULTS = { levelVolume: true };
  const attached = new WeakSet();
  let context = null;
  let enabled = DEFAULTS.levelVolume;
  const chains = [];

  // createMediaElementSource() makes a cross-origin element without CORS permission play silence,
  // so only attach when the element is same-origin, a blob/MSE source, or opted in to CORS.
  function safeToAttach(media) {
    const src = media.currentSrc || media.src || '';
    if (!src || src.startsWith('blob:') || media.crossOrigin) return true;
    try { return new URL(src, location.href).origin === location.origin; } catch { return false; }
  }

  function buildChain(ctx, media) {
    const source = ctx.createMediaElementSource(media);
    const compressor = ctx.createDynamicsCompressor();
    compressor.threshold.value = -35;
    compressor.knee.value = 10;
    compressor.ratio.value = 8;
    compressor.attack.value = 0.005;
    compressor.release.value = 0.2;
    const makeup = ctx.createGain();
    makeup.gain.value = 3; // about +10 dB
    const limiter = ctx.createDynamicsCompressor();
    limiter.threshold.value = -6;
    limiter.knee.value = 0;
    limiter.ratio.value = 20;
    limiter.attack.value = 0.001;
    limiter.release.value = 0.1;
    source.connect(compressor); compressor.connect(makeup); makeup.connect(limiter); limiter.connect(ctx.destination);
    return { compressor, makeup, limiter };
  }

  function apply(chain) {
    chain.compressor.threshold.value = enabled ? -35 : 0;
    chain.makeup.gain.value = enabled ? 3 : 1;
    chain.limiter.threshold.value = enabled ? -6 : 0;
  }

  function scan() {
    for (const media of document.querySelectorAll('audio, video')) {
      if (attached.has(media) || !safeToAttach(media)) continue;
      try {
        context = context || new AudioContext();
        const chain = buildChain(context, media);
        attached.add(media);
        chains.push(chain);
        apply(chain);
        media.addEventListener('play', () => { if (context.state === 'suspended') context.resume(); });
      } catch (_) { /* Element already routed elsewhere; leave it playing normally. */ }
    }
  }

  chrome.storage.sync.get(DEFAULTS, settings => { enabled = settings.levelVolume; chains.forEach(apply); });
  chrome.storage.onChanged.addListener(changes => {
    if (changes.levelVolume) { enabled = changes.levelVolume.newValue; chains.forEach(apply); }
  });
  scan();
  new MutationObserver(scan).observe(document.documentElement, { childList: true, subtree: true });
  setInterval(scan, 3000);
})();
