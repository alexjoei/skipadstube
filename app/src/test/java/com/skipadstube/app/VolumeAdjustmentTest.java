package com.skipadstube.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class VolumeAdjustmentTest {
    private static final class Output implements VolumeAdjustment.Output {
        int value = 7;
        int steps;
        boolean rejectAbsolute;
        boolean rejectSteps;
        public int current() { return value; }
        public int maximum() { return 16; }
        public void absolute(int target) { if (!rejectAbsolute) value = target; }
        public void step(boolean up) {
            steps++;
            if (!rejectSteps) value += up ? 1 : -1;
        }
    }
    @Test public void ordinaryDevicesNeedNoSteps() {
        Output out = new Output();
        VolumeAdjustment.set(out, 0);
        assertEquals(0, out.value);
        assertEquals(0, out.steps);
    }
    @Test public void rejectedAbsoluteWriteStillSilencesAndRestores() {
        Output out = new Output(); out.rejectAbsolute = true;
        VolumeAdjustment.set(out, 0);
        assertEquals(0, out.value);
        VolumeAdjustment.set(out, 7);
        assertEquals(7, out.value);
        assertEquals(14, out.steps);
    }
    @Test public void rejectedStepsStopImmediately() {
        Output out = new Output(); out.rejectAbsolute = true; out.rejectSteps = true;
        VolumeAdjustment.set(out, 0);
        assertEquals(7, out.value);
        assertEquals(1, out.steps);
    }
}
