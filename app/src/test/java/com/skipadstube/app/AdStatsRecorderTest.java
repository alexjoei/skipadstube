package com.skipadstube.app;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class AdStatsRecorderTest {
    @Test public void ordinaryVideoNeverAppendsARow() {
        FakeClock clock = new FakeClock();
        FakeSink sink = new FakeSink();
        AdStatsRecorder recorder = new AdStatsRecorder(clock, sink);
        for (int i = 0; i < 5; i++) recorder.update(false, null, false);
        assertTrue(sink.rows.isEmpty());
    }

    @Test public void oneAdAppendsOneRowWithDurationAndLabel() {
        FakeClock clock = new FakeClock();
        FakeSink sink = new FakeSink();
        AdStatsRecorder recorder = new AdStatsRecorder(clock, sink);
        clock.value = 1_000L;
        recorder.update(true, "Anuncio - 15", false);
        clock.value = 1_500L;
        recorder.update(true, null, false);
        clock.value = 4_200L;
        recorder.update(false, null, false);
        assertEquals(1, sink.rows.size());
        assertTrue(sink.rows.get(0).endsWith(",3200,false,\"Anuncio - 15\""));
    }

    @Test public void skipClickDuringAdIsRecorded() {
        FakeClock clock = new FakeClock();
        FakeSink sink = new FakeSink();
        AdStatsRecorder recorder = new AdStatsRecorder(clock, sink);
        recorder.update(true, "Anuncio", false);
        recorder.update(true, null, true);
        clock.value = 800L;
        recorder.update(false, null, false);
        assertTrue(sink.rows.get(0).contains(",800,true,"));
    }

    @Test public void consecutiveAdsProduceSeparateRows() {
        FakeClock clock = new FakeClock();
        FakeSink sink = new FakeSink();
        AdStatsRecorder recorder = new AdStatsRecorder(clock, sink);
        recorder.update(true, null, false);
        clock.value = 1_000L; recorder.update(false, null, false);
        clock.value = 5_000L; recorder.update(true, null, false);
        clock.value = 5_300L; recorder.update(false, null, false);
        assertEquals(2, sink.rows.size());
    }

    @Test public void firstLabelSeenDuringAnAdWins() {
        FakeClock clock = new FakeClock();
        FakeSink sink = new FakeSink();
        AdStatsRecorder recorder = new AdStatsRecorder(clock, sink);
        recorder.update(true, "Primero", false);
        recorder.update(true, "Segundo", false);
        recorder.update(false, null, false);
        assertTrue(sink.rows.get(0).endsWith("\"Primero\""));
    }

    @Test public void missingLabelLeavesTheColumnEmpty() {
        FakeClock clock = new FakeClock();
        FakeSink sink = new FakeSink();
        AdStatsRecorder recorder = new AdStatsRecorder(clock, sink);
        recorder.update(true, null, false);
        recorder.update(false, null, false);
        assertTrue(sink.rows.get(0).endsWith(",false,"));
    }

    @Test public void labelWithQuotesIsEscapedForCsv() {
        FakeClock clock = new FakeClock();
        FakeSink sink = new FakeSink();
        AdStatsRecorder recorder = new AdStatsRecorder(clock, sink);
        String raw = "Anuncio \"premium\"";
        recorder.update(true, raw, false);
        recorder.update(false, null, false);
        String expected = "\"" + raw.replace("\"", "\"\"") + "\"";
        assertTrue(sink.rows.get(0).endsWith("," + expected));
    }

    @Test public void stayingMidAdNeverAppendsUntilItEnds() {
        FakeClock clock = new FakeClock();
        FakeSink sink = new FakeSink();
        AdStatsRecorder recorder = new AdStatsRecorder(clock, sink);
        for (int i = 0; i < 20; i++) recorder.update(true, null, false);
        assertTrue(sink.rows.isEmpty());
    }

    private static final class FakeClock implements AdStatsRecorder.Clock {
        long value;
        public long now() { return value; }
    }
    private static final class FakeSink implements AdStatsRecorder.Sink {
        final List<String> rows = new ArrayList<>();
        public void append(String row) { rows.add(row); }
    }
}
