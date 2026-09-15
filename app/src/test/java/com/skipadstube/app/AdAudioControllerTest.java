package com.skipadstube.app;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class AdAudioControllerTest {
    @Test public void ordinaryVideoNeverChangesAudio() {
        Output out = new Output();
        AdAudioController controller = new AdAudioController(out);
        for (int i = 0; i < 10; i++) controller.update(false, true, true);
        assertEquals(7, out.value);
        assertFalse(out.muted);
        assertTrue(out.events.isEmpty());
    }
    @Test public void firstContentCheckRestoresAudioAfterAd() {
        Output out = new Output();
        AdAudioController controller = new AdAudioController(out);
        controller.update(true, true, false);
        controller.update(false, true, false);
        assertEquals(7, out.value);
        assertFalse(out.muted);
    }
    private static class Output implements AdAudioController.Output {
        int value = 7;
        boolean fixed;
        boolean muted;
        boolean rejectVolume;
        List<String> events = new ArrayList<>();
        public int volume() { return value; }
        public void volume(int next) { if (!fixed && !rejectVolume) value = next; events.add("volume:" + next); }
        public boolean muted() { return muted; }
        public void mute(boolean next) { if (!fixed) muted = next; events.add("mute:" + next); }
        public void chime(boolean start) { events.add(start ? "start" : "end"); }
    }
    @Test public void muteBeforeChimeAndRestoreBeforeEndChime() {
        Output out = new Output();
        AdAudioController controller = new AdAudioController(out);
        controller.update(true, true, true);
        controller.update(true, true, true);
        controller.update(false, true, true);
        controller.update(false, true, true);
        assertEquals(Arrays.asList("volume:0", "mute:true", "start", "volume:7", "mute:false", "end"), out.events);
    }
    @Test public void disabledMutingDoesNotPlayAdChimes() {
        Output out = new Output();
        AdAudioController controller = new AdAudioController(out);
        controller.update(true, false, true);
        controller.update(false, false, true);
        assertTrue(out.events.isEmpty());
    }
    @Test public void disablingMuteDuringAdRestoresImmediately() {
        Output out = new Output();
        AdAudioController controller = new AdAudioController(out);
        controller.update(true, true, false);
        controller.update(true, false, false);
        assertEquals(7, out.value);
        assertEquals(Arrays.asList("volume:0", "mute:true", "volume:7", "mute:false"), out.events);
    }
    @Test public void reassertMuteWithoutLosingOriginalVolume() {
        Output out = new Output();
        AdAudioController controller = new AdAudioController(out);
        controller.update(true, true, false);
        out.value = 3;
        controller.update(true, true, false);
        assertEquals(0, out.value);
        controller.finish(false);
        assertEquals(7, out.value);
        assertEquals(Arrays.asList("volume:0", "mute:true", "volume:0", "volume:7", "mute:false"), out.events);
    }
    @Test public void fixedVolumeDoesNotAnnounceSuccessfulMute() {
        Output out = new Output(); out.fixed = true;
        AdAudioController controller = new AdAudioController(out);
        controller.update(true, true, true);
        controller.finish(true);
        assertFalse(out.events.contains("start"));
        assertFalse(out.events.contains("end"));
    }
    @Test public void alreadySilentVolumeRemainsSilentAfterAd() {
        Output out = new Output(); out.value = 0;
        AdAudioController controller = new AdAudioController(out);
        controller.update(true, true, false);
        controller.finish(false);
        assertEquals(0, out.value);
    }
    @Test public void explicitMuteWorksWhenVolumeWriteIsIgnored() {
        Output out = new Output(); out.rejectVolume = true;
        AdAudioController controller = new AdAudioController(out);
        controller.update(true, true, true);
        assertTrue(out.muted);
        assertTrue(out.events.contains("start"));
        controller.finish(false);
        assertFalse(out.muted);
        assertEquals(7, out.value);
    }
    @Test public void neverUnmutesPreviouslyMutedAudio() {
        Output out = new Output(); out.value = 0; out.muted = true;
        AdAudioController controller = new AdAudioController(out);
        controller.update(true, true, false);
        controller.finish(false);
        assertTrue(out.muted);
        assertFalse(out.events.contains("mute:false"));
    }
    @Test public void retriesRejectedRestorationWithoutLosingOriginalVolume() {
        Output out = new Output();
        AdAudioController controller = new AdAudioController(out);
        controller.update(true, true, true);
        out.fixed = true;
        controller.update(false, true, true);
        assertTrue(out.muted);
        assertFalse(out.events.contains("end"));
        controller.update(true, true, true);
        out.fixed = false;
        controller.update(false, true, true);
        assertEquals(7, out.value);
        assertFalse(out.muted);
        assertEquals(1, java.util.Collections.frequency(out.events, "start"));
        assertEquals(1, java.util.Collections.frequency(out.events, "end"));
    }
}
