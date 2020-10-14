package com.fmsys.snapdrop;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.core.content.FileProvider;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;


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

    public static void runUpdate(final Activity context, final String fileName) {

        final String downloadLink = "https://github.com/fm-sys/snapdrop-android/releases/latest/download/" + fileName;

        final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
        if (file.exists()) {
            launchInstallIntent(context, file);
            return;
        }

        final ProgressDialog progressdialog = new ProgressDialog(context, R.style.AlertDialogTheme);
        progressdialog.setMessage(context.getString(R.string.app_update_download_text));
        progressdialog.setTitle(R.string.app_update_download_info);
        progressdialog.setCancelable(false);
        progressdialog.show();

        //set downloadManager
        final DownloadManager.Request downloadRequest = new DownloadManager.Request(Uri.parse(downloadLink))
                .setDescription(context.getString(R.string.app_update_download_info))
                .setTitle(fileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(file));

        // get download service and enqueue file
        final DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        final long downloadId = manager.enqueue(downloadRequest);

        //set BroadcastReceiver to install app when .apk is downloaded
        final BroadcastReceiver onComplete = new BroadcastReceiver() {
            public void onReceive(final Context cntxt, final Intent intent) {
                progressdialog.dismiss();
                context.unregisterReceiver(this);
                launchInstallIntent(context, file);
            }
        };
        //register receiver for when .apk download is compete
        context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));


    }

    private static void launchInstallIntent(final Activity context, final File file) {

        final Intent install = new Intent(Intent.ACTION_INSTALL_PACKAGE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            install.setData(FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file));
            install.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            install.setData(Uri.fromFile(file));
        }
        context.startActivity(install);
    }

    public static void showUpdatesInBrowserIntent(final Activity context) {
        final Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("https://github.com/fm-sys/snapdrop-android/releases/latest"));
        context.startActivity(i);
    }

}
