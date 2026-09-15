package com.skipadstube.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public final class YouTubeAutomationService extends AccessibilityService {
    private static final String YOUTUBE = "com.google.android.youtube";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AudioManager audio;
    private boolean mutedByUs;
    private boolean adActive;
    private int volumeBeforeAd = -1;
    private final Runnable endCheck = new Runnable() {
        @Override public void run() { restoreIfAdEnded(); }
    };

    @Override public void onServiceConnected() {
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null || !YOUTUBE.contentEquals(event.getPackageName())) {
            finishAd(false); return;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        ScanResult result = new ScanResult();
        scan(root, result);
        if (isAdEvent(event)) result.adDetected = true;
        root.recycle();

        if (result.adDetected) {
            beginAd();
            if (prefs().getBoolean("skip_ads", true) && result.skipNode != null) click(result.skipNode);
            handler.removeCallbacks(endCheck);
            handler.postDelayed(endCheck, 1400);
        } else {
            handler.removeCallbacks(endCheck);
            handler.postDelayed(endCheck, 700);
        }
        if (result.skipNode != null) result.skipNode.recycle();
    }

    private void scan(AccessibilityNodeInfo node, ScanResult result) {
        String id = node.getViewIdResourceName();
        if (DetectionRules.isAdSignal(id, node.getText(), node.getContentDescription())) result.adDetected = true;
        if (result.skipNode == null && node.isVisibleToUser()
            && DetectionRules.isSkip(id, node.getText(), node.getContentDescription())) {
            result.skipNode = AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) { scan(child, result); child.recycle(); }
        }
    }

    private void click(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo target = node;
        while (target != null && !target.isClickable()) target = target.getParent();
        if (target != null) target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        if (target != null && target != node) target.recycle();
    }

    private void mute() {
        if (mutedByUs || audio == null) return;
        volumeBeforeAd = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        mutedByUs = true;
    }

    private void beginAd() {
        if (!adActive) {
            adActive = true;
            if (prefs().getBoolean("soft_chimes", true)) SoftChime.play(true);
        }
        if (prefs().getBoolean("mute_ads", true)) mute();
    }

    private boolean isAdEvent(AccessibilityEvent event) {
        StringBuilder value = new StringBuilder();
        List<CharSequence> parts = event.getText();
        if (parts != null) for (CharSequence part : parts) if (part != null) value.append(part).append(' ');
        return DetectionRules.isAdSignal(null, value, event.getContentDescription());
    }

    private void restoreIfAdEnded() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) { restoreVolume(); return; }
        ScanResult result = new ScanResult();
        scan(root, result);
        root.recycle();
        if (result.adDetected) {
            if (prefs().getBoolean("skip_ads", true) && result.skipNode != null) click(result.skipNode);
            handler.postDelayed(endCheck, 1200);
        } else {
            finishAd(true);
        }
        if (result.skipNode != null) result.skipNode.recycle();
    }

    private void restoreVolume() {
        if (!mutedByUs || audio == null) return;
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, Math.max(0, volumeBeforeAd), 0);
        mutedByUs = false; volumeBeforeAd = -1;
    }

    private void finishAd(boolean notify) {
        boolean wasActive = adActive;
        restoreVolume();
        adActive = false;
        if (notify && wasActive && prefs().getBoolean("soft_chimes", true)) SoftChime.play(false);
    }

    private SharedPreferences prefs() { return getSharedPreferences("skipadstube", MODE_PRIVATE); }
    @Override public void onInterrupt() { finishAd(false); }
    @Override public void onDestroy() { handler.removeCallbacksAndMessages(null); finishAd(false); super.onDestroy(); }

    private static final class ScanResult { boolean adDetected; AccessibilityNodeInfo skipNode; }
}
