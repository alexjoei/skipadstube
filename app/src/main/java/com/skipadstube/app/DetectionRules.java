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
        "ad_badge", "ad_badge_text", "ad_progress", "ad_countdown", "ad_duration",
        "ad_info", "ad_info_view", "ad_remaining_time", "player_ad_controls"
    ));
    private static final String[] SKIP_TEXT = {
        "saltar anuncio", "omitir anuncio", "omitir anuncios", "skip ad", "skip ads",
        "uberspringen", "annonce ignorer", "ignorer l annonce", "salta annuncio"
    };
    private static final String[] AD_TEXT = {
        "ad 1 of", "ad 2 of", "sponsored", "patrocinado",
        "visit advertiser", "visitar anunciante", "more about this ad",
        "acerca de este anuncio", "why this ad", "por que este anuncio"
    };
    private static final Set<String> EXACT_AD_TEXT = new HashSet<>(Arrays.asList(
        "anuncio", "publicidad", "ad"
    ));

    static boolean isSkip(String id, CharSequence text, CharSequence description) {
        return hasIdSuffix(id, SKIP_ID_SUFFIXES) || containsAny(join(text, description), SKIP_TEXT);
    }

    static boolean isAdSignal(String id, CharSequence text, CharSequence description) {
        String normalized = join(text, description);
        return hasIdSuffix(id, AD_ID_SUFFIXES) || isSkip(id, text, description)
            || EXACT_AD_TEXT.contains(normalized) || containsAny(normalized, AD_TEXT);
    }

    private static boolean hasIdSuffix(String id, Set<String> suffixes) {
        if (id == null) return false;
        for (String suffix : suffixes) if (id.endsWith("/id/" + suffix) || id.endsWith(":" + suffix)) return true;
        return false;
    }

    private static String join(CharSequence a, CharSequence b) {
        return normalize((a == null ? "" : a.toString()) + " " + (b == null ? "" : b.toString()));
    }

    private static String normalize(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    private static boolean containsAny(String value, String[] needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }
}
