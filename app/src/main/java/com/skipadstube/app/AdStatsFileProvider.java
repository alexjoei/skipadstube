package com.skipadstube.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * Serves only ad_stats.csv as a content:// Uri so MainActivity can hand it to the system share
 * sheet without exposing a raw file:// path (not allowed since Android 7) and without pulling in
 * the androidx FileProvider dependency this project does not otherwise need.
 */
public final class AdStatsFileProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    @Override public String getType(Uri uri) { return "text/csv"; }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File dir = getContext().getExternalFilesDir(null);
        File file = new File(dir != null ? dir : getContext().getFilesDir(), "ad_stats.csv");
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) {
        return null;
    }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { return 0; }
    @Override public int delete(Uri uri, String selection, String[] args) { return 0; }
}
