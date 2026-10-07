package com.skipadstube.app;

/**
 * Compressor + limiter settings (dB / ms) for the per-app volume leveler. Higher strengths pull
 * quiet speech up and clamp loud ads harder. Values are a starting point to be tuned by ear.
 */
final class LevelingProfile {
    static final int MIN_STRENGTH = 0;
    static final int MAX_STRENGTH = 2;
    static final String[] NAMES = {"Suave", "Medio", "Fuerte"};

    final float compressorThresholdDb;
    final float compressorRatio;
    final float makeupGainDb;
    final float limiterThresholdDb;
    final float limiterRatio;
    final float attackMs = 5f;
    final float releaseMs = 200f;
    final float kneeDb = 6f;

    private LevelingProfile(float threshold, float ratio, float makeup, float limiterThreshold, float limiterRatio) {
        this.compressorThresholdDb = threshold;
        this.compressorRatio = ratio;
        this.makeupGainDb = makeup;
        this.limiterThresholdDb = limiterThreshold;
        this.limiterRatio = limiterRatio;
    }

    static LevelingProfile forStrength(int strength) {
        switch (Math.max(MIN_STRENGTH, Math.min(MAX_STRENGTH, strength))) {
            case 0: return new LevelingProfile(-30f, 3f, 6f, -4f, 10f);
            case 2: return new LevelingProfile(-40f, 8f, 14f, -6f, 30f);
            default: return new LevelingProfile(-35f, 5f, 10f, -6f, 20f);
        }
    }

    private LevelingProfile() { throw new AssertionError(); }
}
