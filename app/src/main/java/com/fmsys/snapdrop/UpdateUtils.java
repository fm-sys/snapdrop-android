package com.fmsys.snapdrop;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;


public final class UpdateUtils {

    private UpdateUtils() {
        // utility class
    }

    public static String checkUpdate() throws JSONException, IOException {

        final OkHttpClient client = new OkHttpClient();
        final Request request = new Request.Builder()
                .url("https://fm-sys.github.io/snapdrop-android/output-metadata.json")
                .build();

        final Response response = client.newCall(request).execute();
        final JSONObject obj = new JSONObject(response.body().string());
        final JSONObject recentApp = obj.getJSONArray("elements").getJSONObject(0);
        final int versionCode = recentApp.getInt("versionCode");

        if (BuildConfig.VERSION_CODE < versionCode) {
            return recentApp.getString("outputFile");
        }

        return null;
    }

    public static void showUpdatesInBrowserIntent(final Activity context) {
        final Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("https://github.com/fm-sys/snapdrop-android/releases/latest"));
        context.startActivity(i);
    }

    public static void showAppInMarket(final Activity context) {
        final String appPackageName = context.getPackageName();
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    public static boolean isInstalledViaGooglePlay(final Activity context) {
        final List<String> validInstallers = new ArrayList<>(Arrays.asList("com.android.vending", "com.google.android.feedback")); // list with valid installer package names
        final String installer = context.getPackageManager().getInstallerPackageName(context.getPackageName());
        return installer != null && validInstallers.contains(installer);
    }
}
