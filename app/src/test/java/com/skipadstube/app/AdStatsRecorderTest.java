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
        for (int i = 0; i < 5; i++) recorder.update(false, null, false, false, null, true);
        assertTrue(sink.rows.isEmpty());
    }

    @Test public void oneAdAppendsOneRowWithDurationAndLabel() {
        FakeClock clock = new FakeClock();
        FakeSink sink = new FakeSink();
        AdStatsRecorder recorder = new AdStatsRecorder(clock, sink);
        clock.value = 1_000L;
        recorder.update(true, "Anuncio - 15", false, false, null, false);
        clock.value = 1_500L;
        recorder.update(true, null, false, false, null, false);
        clock.value = 4_200L;
        recorder.update(false, null, false, false, null, true);
        assertEquals(1, sink.rows.size());
        assertTrue(sink.rows.get(0).startsWith(iso(1_000L) + "," + iso(4_200L) + ",3200,15,"));
    }

    @Test public void skipClickDuringAdIsRecorded() {
        FakeClock clock = new FakeClock();
        FakeSink sink = new FakeSink();
        AdStatsRecorder recorder = new AdStatsRecorder(clock, sink);
        recorder.update(true, "Anuncio", false, true, null, false);
        recorder.update(true, null, true, true, null, false);
        clock.value = 800L;
        recorder.update(false, null, false, false, null, true);
        assertTrue(sink.rows.get(0).contains(",800,,0,true,true,1,"));
    }

    @Test public void skippableWithoutAClickIsRecordedAsNotSkipped() {
        FakeClock clock = new FakeClock();
        FakeSink sink = new FakeSink();
        AdStatsRecorder recorder = new AdStatsRecorder(clock, sink);
        recorder.update(true, null, false, true, null, false);
        recorder.update(false, null, false, false, null, true);
        assertTrue(sink.rows.get(0).contains(",true,false,1,"));
    }

    @Test public void nonSkippableAdHasEmptyTimeToSkipAndFalseSkippable() {
        FakeClock clock = new FakeClock();
        FakeSink sink = new FakeSink();
        AdStatsRecorder recorder = new AdStatsRecorder(clock, sink);
        recorder.update(true, null, false, false, null, false);
        recorder.update(false, null, false, false, null, true);
        String row = sink.rows.get(0);
        assertTrue(row.contains(",false,false,1,"));
    }

    @Test public void timeToSkipIsMeasuredFromAdStartToFirstAvailability() {
        FakeClock clock = new FakeClock();
        FakeSink sink = new FakeSink();
        AdStatsRecorder recorder = new AdStatsRecorder(clock, sink);
        clock.value = 0L; recorder.update(true, null, false, false, null, false);
        clock.value = 5_000L; recorder.update(true, null, false, true, null, false);
        clock.value = 9_000L; recorder.update(true, null, false, true, null, false);
        clock.value = 12_000L; recorder.update(false, null, false, false, null, true);
        assertTrue(sink.rows.get(0).contains(",12000,,5000,true,"));
    }

    @Test public void consecutiveAdsWithinGapShareAPodAndIncrementPosition() {
        FakeClock clock = new FakeClock();
        FakeSink sink = new FakeSink();
        AdStatsRecorder recorder = new AdStatsRecorder(clock, sink);
        recorder.update(true, null, false, false, null, false);
        clock.value = 1_000L; recorder.update(false, null, false, false, null, true);
        clock.value = 3_500L; recorder.update(true, null, false, false, null, false);
        clock.value = 3_800L; recorder.update(false, null, false, false, null, true);
        assertEquals(2, sink.rows.size());
        assertTrue(sink.rows.get(0).endsWith(",1,0,,"));
        assertTrue(sink.rows.get(1).endsWith(",2,0,,"));
    }

    @Test public void aLongGapStartsANewPod() {
        FakeClock clock = new FakeClock();
        FakeSink sink = new FakeSink();
        AdStatsRecorder recorder = new AdStatsRecorder(clock, sink);
        recorder.update(true, null, false, false, null, false);
        clock.value = 1_000L; recorder.update(false, null, false, false, null, true);
        clock.value = 20_000L; recorder.update(true, null, false, false, null, false);
        clock.value = 20_300L; recorder.update(false, null, false, false, null, true);
        assertTrue(sink.rows.get(1).endsWith(",1,0,,"));
    }

    @Test public void firstLabelAndGuessSeenDuringAnAdWin() {
        FakeClock clock = new FakeClock();
        FakeSink sink = new FakeSink();
        AdStatsRecorder recorder = new AdStatsRecorder(clock, sink);
        recorder.update(true, "Primero", false, false, "MarcaX", false);
        recorder.update(true, "Segundo", false, false, "MarcaY", false);
        recorder.update(false, null, false, false, null, true);
        String row = sink.rows.get(0);
        assertTrue(row.endsWith("\"Primero\",\"MarcaX\""));
    }

    @Test public void labelWithQuotesIsEscapedForCsv() {
        FakeClock clock = new FakeClock();
        FakeSink sink = new FakeSink();
        AdStatsRecorder recorder = new AdStatsRecorder(clock, sink);
        String raw = "Anuncio \"premium\"";
        recorder.update(true, raw, false, false, null, false);
        recorder.update(false, null, false, false, null, true);
        String expected = "\"" + raw.replace("\"", "\"\"") + "\"";
        assertTrue(sink.rows.get(0).contains("," + expected + ","));
    }

    @Test public void missingLabelAndGuessLeaveThoseColumnsEmpty() {
        FakeClock clock = new FakeClock();
        FakeSink sink = new FakeSink();
        AdStatsRecorder recorder = new AdStatsRecorder(clock, sink);
        recorder.update(true, null, false, false, null, false);
        recorder.update(false, null, false, false, null, true);
        assertTrue(sink.rows.get(0).endsWith(",,"));
    }

    @Test public void contentTimeWatchedBeforeTheAdIsAttachedToThatAdsRow() {
        FakeClock clock = new FakeClock();
        FakeSink sink = new FakeSink();
        AdStatsRecorder recorder = new AdStatsRecorder(clock, sink);
        clock.value = 0L; recorder.update(false, null, false, false, null, true);
        clock.value = 4_000L; recorder.update(false, null, false, false, null, true);
        clock.value = 9_000L; recorder.update(false, null, false, false, null, true);
        clock.value = 9_100L; recorder.update(true, null, false, false, null, false);
        clock.value = 9_600L; recorder.update(false, null, false, false, null, true);
        assertTrue(sink.rows.get(0).contains(",1,9000,,"));
    }

    @Test public void contentTimeResetsAfterEachAdAndIsNotDoubleCounted() {
        FakeClock clock = new FakeClock();
        FakeSink sink = new FakeSink();
        AdStatsRecorder recorder = new AdStatsRecorder(clock, sink);
        clock.value = 0L; recorder.update(false, null, false, false, null, true);
        clock.value = 5_000L; recorder.update(false, null, false, false, null, true);
        clock.value = 5_000L; recorder.update(true, null, false, false, null, false);
        clock.value = 5_200L; recorder.update(false, null, false, false, null, true);
        clock.value = 7_000L; recorder.update(false, null, false, false, null, true);
        clock.value = 7_200L; recorder.update(true, null, false, false, null, false);
        clock.value = 7_300L; recorder.update(false, null, false, false, null, true);
        assertEquals(2, sink.rows.size());
        assertTrue(sink.rows.get(0).contains(",1,5000,,"));
        assertTrue(sink.rows.get(1).contains(",2,1800,,"));
    }

    @Test public void backgroundingTheAppDoesNotCountAsWatchedContentTime() {
        FakeClock clock = new FakeClock();
        FakeSink sink = new FakeSink();
        AdStatsRecorder recorder = new AdStatsRecorder(clock, sink);
        clock.value = 0L; recorder.update(false, null, false, false, null, true);
        clock.value = 1_000L; recorder.update(false, null, false, false, null, true);
        // Leaves YouTube for a long time; the service flushes with playerVisible=false.
        clock.value = 600_000L; recorder.update(false, null, false, false, null, false);
        clock.value = 600_200L; recorder.update(false, null, false, false, null, true);
        clock.value = 601_000L; recorder.update(true, null, false, false, null, false);
        clock.value = 601_500L; recorder.update(false, null, false, false, null, true);
        // Only the pre-background 1000ms counts; the 599,800ms background gap does not.
        assertTrue(sink.rows.get(0).contains(",1,1000,,"));
    }

    @Test public void parseDeclaredSecondsHandlesMinutesAndPlainSeconds() {
        assertEquals(Integer.valueOf(15), AdStatsRecorder.parseDeclaredSeconds("Anuncio - 15"));
        assertEquals(Integer.valueOf(75), AdStatsRecorder.parseDeclaredSeconds("Ad - 1:15"));
        assertNull(AdStatsRecorder.parseDeclaredSeconds("Anuncio"));
        assertNull(AdStatsRecorder.parseDeclaredSeconds(null));
    }

    private static String iso(long epochMillis) {
        java.text.SimpleDateFormat format =
            new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.ROOT);
        return format.format(new java.util.Date(epochMillis));
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
