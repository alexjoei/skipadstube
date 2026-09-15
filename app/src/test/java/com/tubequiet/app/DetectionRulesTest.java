package com.tubequiet.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class DetectionRulesTest {
    @Test public void recognizesSpanishSkipTextWithAccent() {
        assertTrue(DetectionRules.isSkip(null, "Omitir anuncio", null));
    }
    @Test public void recognizesEnglishResourceIdWithoutText() {
        assertTrue(DetectionRules.isSkip("com.google.android.youtube:id/skip_ad_button", null, null));
    }
    @Test public void ordinaryVideoControlsAreNotAds() {
        assertFalse(DetectionRules.isAdSignal("com.google.android.youtube:id/play_pause_button", "Pausa", null));
    }
}
