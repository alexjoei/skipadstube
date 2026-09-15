package com.skipadstube.app;

/** Falls back to ordinary volume steps when an OEM ignores absolute writes. */
final class VolumeAdjustment {
    interface Output {
        int current();
        int maximum();
        void absolute(int value);
        void step(boolean up);
    }

    static void set(Output output, int value) {
        output.absolute(value);
        int current = output.current();
        for (int attempt = 0; current != value && attempt <= output.maximum(); attempt++) {
            output.step(current < value);
            int next = output.current();
            if (next == current) break;
            current = next;
        }
    }
}
