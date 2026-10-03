package com.skipadstube.app;

import android.content.Context;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Appends AdStatsRecorder rows to ad_stats.csv in app-specific storage. The file
 * stays on the device: this app requests no Internet permission and sends nothing
 * anywhere; it is meant to be pulled later (file manager or adb) for analysis.
 */
final class AdStatsFile implements AdStatsRecorder.Sink {
    private final File file;
    private int count;

    AdStatsFile(Context context) {
        File dir = context.getExternalFilesDir(null);
        file = new File(dir != null ? dir : context.getFilesDir(), "ad_stats.csv");
    }

    @Override public void append(String row) {
        try {
            boolean writeHeader = !file.exists();
            try (FileWriter writer = new FileWriter(file, true)) {
                if (writeHeader) writer.write(AdStatsRecorder.HEADER + "\n");
                writer.write(row);
                writer.write("\n");
            }
            count++;
            RuntimeStatus.stats = count + (count == 1 ? " anuncio registrado en " : " anuncios registrados en ")
                + file.getAbsolutePath();
        } catch (IOException error) {
            RuntimeStatus.stats = "No se pudo guardar la estadística: " + error.getMessage();
        }
    }
}
