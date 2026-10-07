package com.skipadstube.app;

import android.media.audiofx.DynamicsProcessing;

/** A compressor/limiter attached to one audio session (one player of another app). */
final class SessionLeveler {
    private static final int CHANNELS = 2;
    private final DynamicsProcessing effect;
    final int sessionId;

    SessionLeveler(int sessionId, LevelingProfile profile) {
        this.sessionId = sessionId;
        DynamicsProcessing.Config config = new DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION, CHANNELS,
            false, 0, true, 1, false, 0, true).build();
        effect = new DynamicsProcessing(0, sessionId, config);
        apply(profile);
        effect.setEnabled(true);
    }

    void apply(LevelingProfile p) {
        for (int channel = 0; channel < CHANNELS; channel++) {
            effect.setMbcBandByChannelIndex(channel, 0, new DynamicsProcessing.MbcBand(
                true, 20000f, p.attackMs, p.releaseMs, p.compressorRatio, p.compressorThresholdDb,
                p.kneeDb, -90f, 1f, 0f, p.makeupGainDb));
            effect.setLimiterByChannelIndex(channel, new DynamicsProcessing.Limiter(
                true, true, 0, 1f, 60f, p.limiterRatio, p.limiterThresholdDb, 0f));
        }
    }

    void release() {
        try { effect.setEnabled(false); } catch (RuntimeException ignored) { /* already gone */ }
        effect.release();
    }
}
