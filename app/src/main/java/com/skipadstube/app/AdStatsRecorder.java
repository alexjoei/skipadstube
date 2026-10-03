package com.skipadstube.app;

/**
 * Turns the per-poll ad signal into one CSV row per finished ad: start/end time,
 * observed duration, the ad's own declared length when parseable, how long the
 * skip control took to appear (if it ever did), whether the ad was skippable at
 * all, whether our own click fired, this ad's position within a back-to-back run
 * (pod) of ads, and two best-effort text columns. Keeps no clock or file access so
 * the state machine stays unit-testable without Android.
 */
final class AdStatsRecorder {
    interface Clock { long now(); }
    interface Sink { void append(String row); }

    static final String HEADER =
        "start,end,duration_ms,declared_seconds,time_to_skip_ms,skippable,skipped,pod_position,ad_label,advertiser_guess";
    // A gap this short or shorter between one ad ending and the next starting is treated
    // as the same ad break (a "pod") rather than a separate, unrelated ad later.
    private static final long POD_GAP_MS = 3000;

    private final Clock clock;
    private final Sink sink;
    private long startMillis = -1;
    private long skipAvailableAtMillis = -1;
    private boolean skipped;
    private boolean skippable;
    private String label = "";
    private String advertiserGuess = "";
    private long lastAdEndMillis = -1;
    private int podPosition;

    AdStatsRecorder(Clock clock, Sink sink) {
        this.clock = clock;
        this.sink = sink;
    }

    void update(boolean adDetected, String adLabel, boolean skipClicked, boolean skipAvailable, String guess) {
        if (adDetected) {
            long now = clock.now();
            if (startMillis < 0) {
                startMillis = now;
                skipAvailableAtMillis = -1;
                skipped = false;
                skippable = false;
                label = "";
                advertiserGuess = "";
                podPosition = (lastAdEndMillis >= 0 && now - lastAdEndMillis <= POD_GAP_MS) ? podPosition + 1 : 1;
            }
            if (label.isEmpty() && adLabel != null && !adLabel.isEmpty()) label = adLabel;
            if (advertiserGuess.isEmpty() && guess != null && !guess.isEmpty()) advertiserGuess = guess;
            if (skipAvailable) {
                skippable = true;
                if (skipAvailableAtMillis < 0) skipAvailableAtMillis = now;
            }
            if (skipClicked) skipped = true;
            return;
        }
        if (startMillis < 0) return;
        long endMillis = clock.now();
        long timeToSkipMs = skipAvailableAtMillis >= 0 ? skipAvailableAtMillis - startMillis : -1;
        sink.append(row(startMillis, endMillis, timeToSkipMs, skippable, skipped, podPosition, label, advertiserGuess));
        lastAdEndMillis = endMillis;
        startMillis = -1;
    }

    static String row(long startMillis, long endMillis, long timeToSkipMs, boolean skippable,
            boolean skipped, int podPosition, String label, String advertiserGuess) {
        Integer declaredSeconds = parseDeclaredSeconds(label);
        return iso(startMillis) + "," + iso(endMillis) + "," + (endMillis - startMillis) + ","
            + (declaredSeconds == null ? "" : declaredSeconds) + ","
            + (timeToSkipMs < 0 ? "" : timeToSkipMs) + ","
            + skippable + "," + skipped + "," + podPosition + ","
            + escape(label) + "," + escape(advertiserGuess);
    }

    /**
     * Best-effort: reuses the already-captured ad label (e.g. "Anuncio - 15" or "0:15") to guess
     * the ad's full declared length in seconds. Not confirmed against a real ad; returns null when
     * no number is found.
     */
    static Integer parseDeclaredSeconds(String label) {
        if (label == null || label.isEmpty()) return null;
        java.util.regex.Matcher minutes = java.util.regex.Pattern.compile("(\\d+):(\\d{2})").matcher(label);
        if (minutes.find()) return Integer.parseInt(minutes.group(1)) * 60 + Integer.parseInt(minutes.group(2));
        java.util.regex.Matcher seconds = java.util.regex.Pattern.compile("\\b(\\d{1,3})\\b").matcher(label);
        if (seconds.find()) return Integer.parseInt(seconds.group(1));
        return null;
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
