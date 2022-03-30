package com.fmsys.snapdrop.utils;

import android.content.Context;
import android.content.Intent;

import com.fmsys.snapdrop.OpenUrlActivity;

public class ShareUtils {
    private ShareUtils() {
        // utility class
    }

    public static void shareUrl(final Context context, final String text) {
        final Intent sendIntent = new Intent()
                .setAction(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, text);

        final Intent[] extraIntents = { new Intent(context, OpenUrlActivity.class)
                .putExtra(Intent.EXTRA_TEXT, text) };

        final Intent shareIntent = Intent.createChooser(sendIntent, null); // pass null as title, as it will otherwise trigger the ugly EMUI share sheet
        shareIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);

        context.startActivity(shareIntent);
    }
}
