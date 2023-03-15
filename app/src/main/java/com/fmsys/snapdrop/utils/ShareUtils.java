package com.fmsys.snapdrop.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.fragment.app.Fragment;

import com.fmsys.snapdrop.OpenUrlActivity;
import com.fmsys.snapdrop.R;
import com.google.android.material.snackbar.Snackbar;

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

    public static void openUrl(final Fragment fragment, final String url) {
        try {
            fragment.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Snackbar.make(fragment.requireView(), R.string.err_no_browser, Snackbar.LENGTH_LONG).show();
        }
    }
}
