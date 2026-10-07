package com.skipadstube.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.audiofx.AudioEffect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keeps a compressor/limiter attached to every audio session of the chosen apps so their volume
 * stays level (loud ads are squashed). Sessions come from {@code dumpsys audio} (DUMP permission)
 * and from the standard OPEN_AUDIO_EFFECT_CONTROL_SESSION broadcast some players send.
 */
public final class VolumeLevelingService extends Service {
    static final String PREFS = "skipadstube";
    static final String KEY_ENABLED = "level_enabled";
    static final String KEY_APPS = "level_apps";
    static final String KEY_STRENGTH = "level_strength";
    static final String DEFAULT_APP = "com.ivoox.app";
    private static final String CHANNEL = "leveling";
    private static final long POLL_MS = 2000;
    private static final int MISSES_BEFORE_RELEASE = 5;

    private HandlerThread thread;
    private Handler handler;
    private final Map<Integer, SessionLeveler> active = new HashMap<>();
    private final Map<Integer, Integer> misses = new HashMap<>();
    private final Map<Integer, String> broadcastSessions = new HashMap<>();
    private int appliedStrength = -1;
    private volatile boolean running;

    private final BroadcastReceiver sessionReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            int session = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1);
            String pkg = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME);
            if (session <= 0 || pkg == null) return;
            synchronized (broadcastSessions) {
                if (AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION.equals(intent.getAction())) {
                    broadcastSessions.put(session, pkg);
                } else {
                    broadcastSessions.remove(session);
                }
            }
        }
    };

    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (!running) return;
            try {
                pollOnce();
            } catch (RuntimeException error) {
                RuntimeStatus.leveling = "Error al nivelar: " + error.getClass().getSimpleName()
                    + " — " + error.getMessage();
            }
            handler.postDelayed(this, POLL_MS);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        thread = new HandlerThread("skipadstube-leveling");
        thread.start();
        handler = new Handler(thread.getLooper());
        IntentFilter filter = new IntentFilter(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
        filter.addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        registerReceiver(sessionReceiver, filter, Context.RECEIVER_EXPORTED);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, "Nivelador de volumen",
            NotificationManager.IMPORTANCE_LOW));
        Notification notification = new Notification.Builder(this, CHANNEL)
            .setContentTitle("skipadstube")
            .setContentText("Nivelando el volumen de las apps elegidas")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(true).build();
        startForeground(1, notification);
        if (!running) {
            running = true;
            handler.post(poll);
        }
        return START_STICKY;
    }

    private void pollOnce() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_ENABLED, false)) {
            stopSelf();
            return;
        }
        Set<String> packages = prefs.getStringSet(KEY_APPS, Collections.singleton(DEFAULT_APP));
        int strength = Math.max(LevelingProfile.MIN_STRENGTH,
            Math.min(LevelingProfile.MAX_STRENGTH, prefs.getInt(KEY_STRENGTH, 1)));
        PackageManager pm = getPackageManager();
        Set<Integer> uids = new HashSet<>();
        for (String pkg : packages) {
            try {
                uids.add(pm.getApplicationInfo(pkg, 0).uid);
            } catch (PackageManager.NameNotFoundException ignored) {
                // Not installed on this phone.
            }
        }

        Set<Integer> wanted = new HashSet<>();
        String source;
        if (AudioSessionFinder.hasDumpPermission(this)) {
            List<AudioSessionParser.Player> players = AudioSessionFinder.read();
            if (players != null) {
                for (AudioSessionParser.Player p : players) {
                    if (uids.contains(p.uid) && p.sessionId > 0) wanted.add(p.sessionId);
                }
                source = " (volcado del sistema: " + players.size() + " reproductores)";
            } else {
                source = " (no se pudo leer el volcado del sistema)";
            }
        } else {
            source = " (sin permiso DUMP: solo sesiones que la app anuncie)";
        }
        synchronized (broadcastSessions) {
            for (Map.Entry<Integer, String> e : broadcastSessions.entrySet()) {
                if (packages.contains(e.getValue())) wanted.add(e.getKey());
            }
        }

        LevelingProfile profile = LevelingProfile.forStrength(strength);
        boolean strengthChanged = strength != appliedStrength;
        appliedStrength = strength;
        for (int session : wanted) {
            misses.remove(session);
            SessionLeveler existing = active.get(session);
            try {
                if (existing == null) active.put(session, new SessionLeveler(session, profile));
                else if (strengthChanged) existing.apply(profile);
            } catch (RuntimeException error) {
                RuntimeStatus.leveling = "No se pudo enganchar a la sesión " + session + ": "
                    + error.getClass().getSimpleName() + " — " + error.getMessage();
                return;
            }
        }
        for (Iterator<Map.Entry<Integer, SessionLeveler>> it = active.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, SessionLeveler> e = it.next();
            if (wanted.contains(e.getKey())) continue;
            int count = misses.containsKey(e.getKey()) ? misses.get(e.getKey()) + 1 : 1;
            if (count >= MISSES_BEFORE_RELEASE) {
                try {
                    e.getValue().release();
                } catch (RuntimeException ignored) {
                    // Already released by the system.
                }
                misses.remove(e.getKey());
                it.remove();
            } else {
                misses.put(e.getKey(), count);
            }
        }
        RuntimeStatus.leveling = uids.isEmpty()
            ? "Ninguna de las apps elegidas está instalada"
            : "Nivelador " + LevelingProfile.NAMES[strength] + ": " + active.size()
                + " sesión(es) de audio controladas" + source;
    }

    @Override public void onDestroy() {
        running = false;
        unregisterReceiver(sessionReceiver);
        handler.post(() -> {
            for (SessionLeveler leveler : active.values()) {
                try {
                    leveler.release();
                } catch (RuntimeException ignored) {
                    // Already released by the system.
                }
            }
            active.clear();
            thread.quitSafely();
        });
        RuntimeStatus.leveling = "Nivelador desactivado";
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
