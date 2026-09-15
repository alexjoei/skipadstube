package com.skipadstube.app;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

final class SoftChime {
    private static final int RATE = 48000;

    static void play(final boolean start) {
        final int usage = RuntimeStatus.connected
            ? AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY : AudioAttributes.USAGE_MEDIA;
        new Thread(new Runnable() {
            @Override public void run() {
                AudioTrack track = null;
                try {
                short[] pcm = create(start);
                track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(usage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(pcm.length * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC).build();
                if (track.getState() != AudioTrack.STATE_INITIALIZED
                        || track.write(pcm, 0, pcm.length) != pcm.length) {
                    throw new IllegalStateException("No se pudo preparar el sonido");
                }
                track.setVolume(0.45f);
                track.play();
                RuntimeStatus.sound = "Sonido enviado a " + (usage == AudioAttributes.USAGE_MEDIA
                    ? "multimedia" : "Accesibilidad") + " (confirma si lo escuchas)";
                try { Thread.sleep(360); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                track.stop();
                } catch (IllegalArgumentException | IllegalStateException | SecurityException error) {
                    RuntimeStatus.sound = "Error de sonido: " + error.getClass().getSimpleName();
                } finally {
                    if (track != null) track.release();
                }
            }
        }, "skipadstube-chime").start();
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
