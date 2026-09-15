package com.skipadstube.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class DetectionRulesTest {
    @Test public void realYouTubeAdProgressIsDetectedButSponsoredRecommendationIsNot() {
        assertTrue(DetectionRules.isAdSignal("com.google.android.youtube:id/ad_progress_text",
            "Sponsored · 1 of 2 · 1:42", "Sponsored · 1 of 2 · 1:42 My Ad Center"));
        assertFalse(DetectionRules.isAdSignal(null, "Sponsored", "Sponsored"));
        assertFalse(DetectionRules.isAdSignal(null, null,
            "Sponsored - #culpanuestra - 20 minutes - Prime Video - play video"));
    }
    @Test public void genericAdvertisingLabelsDoNotProvePlayback() {
        for (String label : new String[] {"Anuncio", "Publicidad", "Ad", "Sponsored", "Patrocinado",
                "Visitar anunciante", "Acerca de este anuncio"}) {
            assertFalse(label, DetectionRules.isAdSignal(null, label, label));
        }
    }
    @Test public void informationalAdControlsDoNotProvePlayback() {
        for (String id : new String[] {"ad_info", "ad_info_view", "player_ad_controls"}) {
            assertFalse(DetectionRules.isAdSignal("com.google.android.youtube:id/" + id, null, null));
        }
    }
    @Test public void videoTitlesDiscussingSkipDoNotMuteOrClick() {
        String title = "Cómo omitir anuncios en YouTube";
        assertFalse(DetectionRules.isAdSignal(null, title, null));
        assertFalse(DetectionRules.isSkip(null, title, null));
    }
    @Test public void spanishAdCountersAndTimers() {
        assertTrue(DetectionRules.isAdSignal(null, "Anuncio 1 de 2", null));
        assertTrue(DetectionRules.isAdSignal(null, "Anuncio · 0:15", null));
        assertTrue(DetectionRules.isAdSignal(null, "Ad • 15 s", null));
    }
    @Test public void ordinaryTitlesContainingAdAreNotAdLabels() {
        assertFalse(DetectionRules.isAdSignal(null, "Mi anuncio favorito", null));
        assertFalse(DetectionRules.isAdSignal(null, "Publicidad en la historia", null));
    }
    @Test public void recognizesSpanishSkipTextWithAccent() {
        assertTrue(DetectionRules.isSkip(null, "Omitir anuncio", null));
    }
    @Test public void recognizesEnglishResourceIdWithoutText() {
        assertTrue(DetectionRules.isSkip("com.google.android.youtube:id/skip_ad_button", null, null));
    }
    @Test public void ordinaryVideoControlsAreNotAds() {
        assertFalse(DetectionRules.isAdSignal("com.google.android.youtube:id/play_pause_button", "Pausa", null));
    }
    @Test public void recognizesAdBadgeResourceIdWithoutText() {
        assertTrue(DetectionRules.isAdSignal("com.google.android.youtube:id/ad_badge", null, null));
    }
    @Test public void similarResourceIdIsNotSkipButton() {
        assertFalse(DetectionRules.isSkip("com.google.android.youtube:id/not_skip_ad_button", null, null));
    }
}
