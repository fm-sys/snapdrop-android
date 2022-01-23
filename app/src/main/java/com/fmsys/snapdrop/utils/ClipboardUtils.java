package com.fmsys.snapdrop.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

public class ClipboardUtils {
    private ClipboardUtils() {
        // utility class
    }

    public static void copy(final Context context, final String text) {
        final ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        final ClipData clip = ClipData.newPlainText("SnapdropAndroid", text);
        clipboard.setPrimaryClip(clip);
    }
}
