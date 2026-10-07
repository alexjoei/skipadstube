package com.skipadstube.app;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class AudioSessionParserTest {
    @Test public void parsesUidSessionAndState() {
        String dump = "  AudioPlaybackConfiguration piid:12 deviceIds:[2] type:android.media.AudioTrack "
            + "u/pid:10207/4321 state:started attr:AudioAttributes: usage=USAGE_MEDIA sessionId:2347 mutedState:none\n";
        List<AudioSessionParser.Player> players = AudioSessionParser.parse(dump);
        assertEquals(1, players.size());
        assertEquals(10207, players.get(0).uid);
        assertEquals(2347, players.get(0).sessionId);
        assertEquals("started", players.get(0).state);
    }
    @Test public void missingSessionIdIsReportedNotDropped() {
        List<AudioSessionParser.Player> players = AudioSessionParser.parse(
            "AudioPlaybackConfiguration piid:3 u/pid:10100/55 state:paused attr:AudioAttributes: usage=USAGE_MEDIA");
        assertEquals(1, players.size());
        assertEquals(-1, players.get(0).sessionId);
    }
    @Test public void ignoresUnrelatedLinesAndNull() {
        assertTrue(AudioSessionParser.parse("players:\nsomething else u/pid:1/2\n").isEmpty());
        assertTrue(AudioSessionParser.parse(null).isEmpty());
    }
}
