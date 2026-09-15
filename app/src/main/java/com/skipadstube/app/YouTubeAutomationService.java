package com.skipadstube.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public final class YouTubeAutomationService extends AccessibilityService {
    private static final String YOUTUBE = "com.google.android.youtube";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AdAudioController controller;
    private SharedPreferences settings;
    private long lastClick;
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            inspect();
            handler.postDelayed(this, 500);
        }
    };
    private final SharedPreferences.OnSharedPreferenceChangeListener settingsChanged =
        (prefs, key) -> inspect();

    @Override public void onServiceConnected() {
        RuntimeStatus.connected = true;
        final AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        controller = new AdAudioController(new AdAudioController.Output() {
            public int volume() { return audio.getStreamVolume(AudioManager.STREAM_MUSIC); }
            public boolean muted() { return audio.isStreamMute(AudioManager.STREAM_MUSIC); }
            public void mute(boolean value) {
                try {
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        value ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE, 0);
                    if (audio.isStreamMute(AudioManager.STREAM_MUSIC) != value
                            && !(value && audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0)) {
                        RuntimeStatus.audioError = "Android no ha aplicado el estado de silencio solicitado";
                    }
                } catch (SecurityException error) {
                    RuntimeStatus.audioError = "Android ha bloqueado el silencio multimedia";
                }
            }
            public void volume(int value) {
                try {
                    VolumeAdjustment.set(new VolumeAdjustment.Output() {
                        public int current() { return audio.getStreamVolume(AudioManager.STREAM_MUSIC); }
                        public int maximum() { return audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC); }
                        public void absolute(int target) {
                            audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0);
                        }
                        public void step(boolean up) {
                            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                                up ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, 0);
                        }
                    }, value);
                    RuntimeStatus.audioError = audio.getStreamVolume(AudioManager.STREAM_MUSIC) == value
                        ? "" : "Android no ha aplicado el volumen solicitado";
                } catch (SecurityException error) {
                    RuntimeStatus.audioError = "Android ha bloqueado el cambio de volumen";
                }
            }
            public void chime(boolean start) { SoftChime.play(start); }
        });
        settings = getSharedPreferences("skipadstube", MODE_PRIVATE);
        settings.registerOnSharedPreferenceChangeListener(settingsChanged);
        handler.removeCallbacks(poll);
        handler.post(poll);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        // The fixed poll avoids rescanning the whole tree for every animation event.
    }

    private void inspect() {
        if (controller == null) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) { RuntimeStatus.scan = "No se puede leer la ventana activa"; finish(); return; }
        ScanResult result = new ScanResult();
        try {
            if (root.getPackageName() == null || !YOUTUBE.contentEquals(root.getPackageName())) {
                RuntimeStatus.scan = "Abre YouTube para comprobar los anuncios";
                finish(); return;
            }
            java.util.List<AccessibilityNodeInfo> players = root.findAccessibilityNodeInfosByViewId(
                YOUTUBE + ":id/watch_player");
            try {
                for (AccessibilityNodeInfo player : players) {
                    if (player.refresh()) scan(player, result);
                }
            } finally {
                for (AccessibilityNodeInfo player : players) player.recycle();
            }
            long now = SystemClock.elapsedRealtime();
            // Never extend muting into content after the visible ad signal disappears.
            boolean ad = result.adDetected;
            controller.update(ad, settings.getBoolean("mute_ads", true),
                settings.getBoolean("soft_chimes", true));
            RuntimeStatus.scan = ad ? "Anuncio detectado" : "YouTube visible; sin señal de anuncio";
            if (ad) {
                AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                RuntimeStatus.lastAd = "Último anuncio: " + new java.text.SimpleDateFormat("HH:mm:ss",
                    java.util.Locale.getDefault()).format(new java.util.Date())
                    + "; volumen multimedia " + audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                    + "; canal silenciado: " + (audio.isStreamMute(AudioManager.STREAM_MUSIC) ? "sí" : "no")
                    + (settings.getBoolean("mute_ads", true) ? "; silencio activado" : "; silencio desactivado");
            }
            if (result.skipNode != null && settings.getBoolean("skip_ads", true)
                    && now - lastClick >= 1000) {
                click(result.skipNode);
                lastClick = now;
            }
        } finally {
            root.recycle();
            if (result.skipNode != null) result.skipNode.recycle();
        }
    }

    private void scan(AccessibilityNodeInfo node, ScanResult result) {
        if (++result.visited > 512 || (result.adDetected && result.skipNode != null)) return;
        if (node.isVisibleToUser()) {
            String id = node.getViewIdResourceName();
            if (DetectionRules.isAdSignal(id, node.getText(), node.getContentDescription())) result.adDetected = true;
            if (result.skipNode == null && node.isEnabled()
                    && DetectionRules.isSkip(id, node.getText(), node.getContentDescription())) {
                result.skipNode = AccessibilityNodeInfo.obtain(node);
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                try { scan(child, result); } finally { child.recycle(); }
            }
        }
    }

    private void click(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo target = AccessibilityNodeInfo.obtain(node);
        while (target != null) {
            if (target.isClickable() && target.isEnabled()) {
                target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                target.recycle();
                return;
            }
            AccessibilityNodeInfo parent = target.getParent();
            target.recycle();
            target = parent;
        }
    }

    private void finish() {
        if (controller != null) controller.finish(false);
    }
    @Override public void onInterrupt() { finish(); }
    @Override public void onDestroy() {
        RuntimeStatus.connected = false;
        handler.removeCallbacksAndMessages(null);
        if (settings != null) settings.unregisterOnSharedPreferenceChangeListener(settingsChanged);
        finish();
        super.onDestroy();
    }
    private static final class ScanResult { boolean adDetected; AccessibilityNodeInfo skipNode; int visited; }
}
