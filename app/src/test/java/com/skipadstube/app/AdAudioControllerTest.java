package com.skipadstube.app;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class AdAudioControllerTest {
    private static class Output implements AdAudioController.Output {
        int value = 7;
        boolean fixed;
        List<String> events = new ArrayList<>();
        public int volume() { return value; }
        public void volume(int next) { if (!fixed) value = next; events.add("volume:" + next); }
        public void chime(boolean start) { events.add(start ? "start" : "end"); }
    }
    @Test public void muteBeforeChimeAndRestoreBeforeEndChime() {
        Output out = new Output();
        AdAudioController controller = new AdAudioController(out);
        controller.update(true, true, true);
        controller.update(true, true, true);
        controller.update(false, true, true);
        controller.update(false, true, true);
        assertEquals(Arrays.asList("volume:0", "start", "volume:7", "end"), out.events);
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
        assertEquals(Arrays.asList("volume:0", "volume:7"), out.events);
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
        assertEquals(Arrays.asList("volume:0", "volume:0", "volume:7"), out.events);
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
}
