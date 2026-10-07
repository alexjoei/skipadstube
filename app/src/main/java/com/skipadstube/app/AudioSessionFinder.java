package com.skipadstube.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/** Reads the system's playback list via {@code dumpsys audio}; needs the DUMP permission (see README). */
final class AudioSessionFinder {
    private static final int MAX_CHARS = 4_000_000;

    static boolean hasDumpPermission(Context context) {
        return context.checkSelfPermission(Manifest.permission.DUMP) == PackageManager.PERMISSION_GRANTED;
    }

    /** Returns null if the dump could not be read. Blocking; call off the main thread. */
    static List<AudioSessionParser.Player> read() {
        Process process = null;
        try {
            process = new ProcessBuilder("dumpsys", "audio").redirectErrorStream(true).start();
            StringBuilder text = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null && text.length() < MAX_CHARS) {
                    if (line.contains("AudioPlaybackConfiguration")) text.append(line).append('\n');
                }
            }
            return AudioSessionParser.parse(text.toString());
        } catch (IOException | RuntimeException error) {
            return null;
        } finally {
            if (process != null) process.destroy();
        }
    }

    private AudioSessionFinder() {}
}
