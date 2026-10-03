package com.skipadstube.app;

/**
 * Turns the per-poll ad signal into one CSV row per finished ad: start/end time,
 * duration, whether our own skip click fired, and the first non-empty ad label
 * text/description seen (if any). Keeps no clock or file access so the state
 * machine stays unit-testable without Android.
 */
final class AdStatsRecorder {
    interface Clock { long now(); }
    interface Sink { void append(String row); }

    static final String HEADER = "start,end,duration_ms,skipped,ad_label";

    private final Clock clock;
    private final Sink sink;
    private long startMillis = -1;
    private boolean skipped;
    private String label = "";

    AdStatsRecorder(Clock clock, Sink sink) {
        this.clock = clock;
        this.sink = sink;
    }

    void update(boolean adDetected, String adLabel, boolean skipClicked) {
        if (adDetected) {
            if (startMillis < 0) {
                startMillis = clock.now();
                skipped = false;
                label = "";
            }
            if (label.isEmpty() && adLabel != null && !adLabel.isEmpty()) label = adLabel;
            if (skipClicked) skipped = true;
            return;
        }
        if (startMillis < 0) return;
        sink.append(row(startMillis, clock.now(), skipped, label));
        startMillis = -1;
    }

    static String row(long startMillis, long endMillis, boolean skipped, String label) {
        return iso(startMillis) + "," + iso(endMillis) + "," + (endMillis - startMillis)
            + "," + skipped + "," + escape(label);
    }

    private static String iso(long epochMillis) {
        java.text.SimpleDateFormat format =
            new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.ROOT);
        return format.format(new java.util.Date(epochMillis));
    }

    private static String escape(String value) {
        if (value == null || value.isEmpty()) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
