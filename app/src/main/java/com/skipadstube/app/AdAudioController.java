package com.skipadstube.app;

final class AdAudioController {
    interface Output {
        int volume();
        void volume(int value);
        void chime(boolean start);
    }
    private final Output output;
    private int savedVolume = -1;
    private boolean notified;
    AdAudioController(Output output) { this.output = output; }
    void update(boolean ad, boolean mute, boolean sounds) {
        if (!ad || !mute) { finish(!ad && sounds); return; }
        if (savedVolume < 0) savedVolume = output.volume();
        if (output.volume() != 0) output.volume(0);
        if (!notified && output.volume() == 0) {
            notified = true;
            if (sounds) output.chime(true);
        }
    }
    void finish(boolean sounds) {
        if (savedVolume < 0) return;
        output.volume(savedVolume);
        savedVolume = -1;
        boolean wasMuted = notified;
        notified = false;
        if (sounds && wasMuted) output.chime(false);
    }
}
