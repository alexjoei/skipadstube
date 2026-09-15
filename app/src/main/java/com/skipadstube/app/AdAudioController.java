package com.skipadstube.app;

final class AdAudioController {
    interface Output {
        int volume();
        void volume(int value);
        boolean muted();
        void mute(boolean value);
        void chime(boolean start);
    }
    private final Output output;
    private int savedVolume = -1;
    private boolean notified;
    private boolean savedMuted;
    AdAudioController(Output output) { this.output = output; }
    void update(boolean ad, boolean mute, boolean sounds) {
        if (!ad || !mute) { finish(!ad && sounds); return; }
        boolean first = savedVolume < 0;
        if (first) {
            savedVolume = output.volume();
            savedMuted = output.muted();
        }
        if (output.volume() != 0) output.volume(0);
        if (first || !output.muted()) output.mute(true);
        if (!notified && (output.volume() == 0 || output.muted())) {
            notified = true;
            if (sounds) output.chime(true);
        }
    }
    void finish(boolean sounds) {
        if (savedVolume < 0) return;
        if (!savedMuted) output.volume(savedVolume);
        if (output.muted() != savedMuted) output.mute(savedMuted);
        // Some devices reject audio changes temporarily. Keep the original state
        // until a subsequent poll confirms restoration, including between ads.
        if (output.muted() != savedMuted || (!savedMuted && output.volume() != savedVolume)) return;
        savedVolume = -1;
        boolean wasMuted = notified;
        notified = false;
        if (sounds && wasMuted) output.chime(false);
    }
}
