package com.fmsys.snapdrop.utils;

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private ZipUtils() {
        // utility class
    }

    public static void createZipFromUris(final Context context, final List<Uri> uris, final OutputStream outputStream) throws IOException {
        final ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
        final byte[] buffer = new byte[1024];

        for (Uri uri : uris) {
            try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                if (inputStream == null) {
                    continue;
                }
                final String fileName = getFileNameFromUri(context, uri);
                zipOutputStream.putNextEntry(new ZipEntry(fileName));

                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    zipOutputStream.write(buffer, 0, length);
                }
                zipOutputStream.closeEntry();
            }
        }

        zipOutputStream.finish();
        zipOutputStream.close();
    }

    private static String getFileNameFromUri(final Context context, final Uri uri) {
        final DocumentFile file = DocumentFile.fromSingleUri(context, uri);
        if (file != null) {
            return file.getName();
        }
        return uri.getLastPathSegment();
    }
}
