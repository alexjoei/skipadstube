package com.skipadstube.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class LevelingProfileTest {
    @Test public void strongerLevelsCompressHarder() {
        LevelingProfile soft = LevelingProfile.forStrength(0);
        LevelingProfile strong = LevelingProfile.forStrength(2);
        assertTrue(strong.compressorRatio > soft.compressorRatio);
        assertTrue(strong.compressorThresholdDb < soft.compressorThresholdDb);
        assertTrue(strong.makeupGainDb > soft.makeupGainDb);
    }
    @Test public void outOfRangeStrengthIsClamped() {
        assertEquals(LevelingProfile.forStrength(0).compressorRatio, LevelingProfile.forStrength(-5).compressorRatio, 0f);
        assertEquals(LevelingProfile.forStrength(2).compressorRatio, LevelingProfile.forStrength(9).compressorRatio, 0f);
    }
    @Test public void limiterAlwaysBelowFullScale() {
        for (int s = LevelingProfile.MIN_STRENGTH; s <= LevelingProfile.MAX_STRENGTH; s++) {
            assertTrue(LevelingProfile.forStrength(s).limiterThresholdDb < 0f);
        }
    }
}
