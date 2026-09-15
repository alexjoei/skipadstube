package com.tubequiet.app;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

final class SoftChime {
    private static final int RATE = 48000;

    static void play(final boolean start) {
        new Thread(new Runnable() {
            @Override public void run() {
                short[] pcm = create(start);
                AudioTrack track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(pcm.length * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC).build();
                track.write(pcm, 0, pcm.length);
                track.setVolume(0.16f);
                track.play();
                try { Thread.sleep(360); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                track.stop();
                track.release();
            }
        }, "TubeQuiet-chime").start();
    }

    private static short[] create(boolean start) {
        int length = (int) (RATE * 0.32);
        short[] result = new short[length];
        double first = start ? 523.25 : 659.25;
        double second = start ? 659.25 : 523.25;
        for (int i = 0; i < length; i++) {
            double seconds = i / (double) RATE;
            double frequency = i < length / 2 ? first : second;
            double local = (i % (length / 2)) / (double) (length / 2);
            double envelope = Math.sin(Math.PI * local) * Math.exp(-1.8 * local);
            result[i] = (short) (Math.sin(2.0 * Math.PI * frequency * seconds) * envelope * 9000);
        }
        return result;
    }

    private SoftChime() {}
}
