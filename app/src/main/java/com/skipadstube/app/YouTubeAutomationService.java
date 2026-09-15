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
    private long lastEvidence = -1;
    private long lastClick;
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            inspect(false);
            handler.postDelayed(this, 500);
        }
    };
    private final SharedPreferences.OnSharedPreferenceChangeListener settingsChanged =
        (prefs, key) -> inspect(false);

    @Override public void onServiceConnected() {
        final AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        controller = new AdAudioController(new AdAudioController.Output() {
            public int volume() { return audio.getStreamVolume(AudioManager.STREAM_MUSIC); }
            public void volume(int value) {
                try { audio.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0); }
                catch (SecurityException ignored) { /* Device policy may block volume changes. */ }
            }
            public void chime(boolean start) { SoftChime.play(start); }
        });
        settings = getSharedPreferences("skipadstube", MODE_PRIVATE);
        settings.registerOnSharedPreferenceChangeListener(settingsChanged);
        handler.removeCallbacks(poll);
        handler.post(poll);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() != null && YOUTUBE.contentEquals(event.getPackageName())) {
            boolean evidence = DetectionRules.isAdSignal(null, null, event.getContentDescription());
            for (CharSequence text : event.getText()) {
                evidence |= DetectionRules.isAdSignal(null, text, null);
            }
            inspect(evidence);
        }
    }

    private void inspect(boolean eventEvidence) {
        if (controller == null) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) { finish(); return; }
        ScanResult result = new ScanResult();
        try {
            if (root.getPackageName() == null || !YOUTUBE.contentEquals(root.getPackageName())) {
                finish(); return;
            }
            scan(root, result);
            long now = SystemClock.elapsedRealtime();
            if (result.adDetected || eventEvidence) lastEvidence = now;
            // Bridge brief tree updates and transitions between consecutive ads.
            boolean ad = lastEvidence >= 0 && now - lastEvidence < 1400;
            controller.update(ad, settings.getBoolean("mute_ads", true),
                settings.getBoolean("soft_chimes", true));
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
        lastEvidence = -1;
        if (controller != null) controller.finish(false);
    }
    @Override public void onInterrupt() { finish(); }
    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (settings != null) settings.unregisterOnSharedPreferenceChangeListener(settingsChanged);
        finish();
        super.onDestroy();
    }
    private static final class ScanResult { boolean adDetected; AccessibilityNodeInfo skipNode; }
}
