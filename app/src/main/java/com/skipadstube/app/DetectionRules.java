package com.skipadstube.app;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

final class DetectionRules {
    // IDs are preferred because they do not depend on the device language.
    private static final Set<String> SKIP_ID_SUFFIXES = new HashSet<>(Arrays.asList(
        "skip_ad_button", "skip_button", "skip_ad_button_text", "ad_skip_button"
    ));
    private static final Set<String> AD_ID_SUFFIXES = new HashSet<>(Arrays.asList(
        "ad_badge", "ad_badge_text", "ad_progress", "ad_progress_text", "ad_countdown", "ad_duration",
        "ad_remaining_time"
    ));
    private static final String[] SKIP_TEXT = {
        "saltar anuncio", "omitir anuncio", "omitir anuncios", "skip ad", "skip ads",
        "uberspringen", "annonce ignorer", "ignorer l annonce", "salta annuncio"
    };
    // Common player-chrome labels that are visible during an ad but are not the ad itself.
    // Filters candidates for the best-effort advertiser_guess column; necessarily incomplete.
    private static final Set<String> PLAYER_CHROME_LABELS = new HashSet<>(Arrays.asList(
        "mas", "more", "configuracion", "settings", "pantalla completa", "full screen", "fullscreen",
        "subtitulos", "subtitles", "captions", "reproducir", "play", "pausa", "pause",
        "silenciar", "mute", "activar sonido", "unmute", "siguiente", "next", "anterior", "previous",
        "cerrar", "close", "suscribirse", "subscribe", "youtube", "compartir", "share"
    ));

    /** True when the text/description is too generic to be a useful advertiser_guess candidate. */
    static boolean isAdNoise(CharSequence value) {
        if (value == null) return true;
        String normalized = normalize(value.toString());
        if (normalized.length() < 2) return true;
        if (normalized.matches("\\d+(?::\\d{2})?")) return true;
        if (isSkipLabel(value) || isAdLabel(value)) return true;
        return PLAYER_CHROME_LABELS.contains(normalized);
    }

    static boolean isSkip(String id, CharSequence text, CharSequence description) {
        return hasIdSuffix(id, SKIP_ID_SUFFIXES) || isSkipLabel(text) || isSkipLabel(description);
    }

    static boolean isAdSignal(String id, CharSequence text, CharSequence description) {
        return hasIdSuffix(id, AD_ID_SUFFIXES) || isSkip(id, text, description)
            || isAdLabel(text) || isAdLabel(description);
    }

    private static boolean isSkipLabel(CharSequence value) {
        if (value == null) return false;
        return Arrays.asList(SKIP_TEXT).contains(normalize(value.toString()));
    }

    private static boolean isAdLabel(CharSequence value) {
        if (value == null) return false;
        String label = normalize(value.toString());
        return label.matches("(?:anuncio|publicidad|ad)\\s*[·•:–-]\\s*\\d+(?::\\d{2})?(?:\\s*(?:s|seg|seconds))?")
            || label.matches("(?:anuncio|ad)\\s+\\d+\\s+(?:de|of)\\s+\\d+(?:\\s*[·•:–-].*)?");
    }

    private static boolean hasIdSuffix(String id, Set<String> suffixes) {
        if (id == null) return false;
        for (String suffix : suffixes) if (id.endsWith(":id/" + suffix) || id.endsWith(":" + suffix)) return true;
        return false;
    }

    private static String normalize(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

}
