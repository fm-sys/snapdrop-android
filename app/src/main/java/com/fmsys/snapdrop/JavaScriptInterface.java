package com.fmsys.snapdrop;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.widget.FrameLayout;

import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class JavaScriptInterface {
    private MainActivity context;

    public JavaScriptInterface(MainActivity context) {
        this.context = context;
    }

    @JavascriptInterface
    public void getBase64FromBlobData(String base64Data, String contentDisposition, String mimetype) throws IOException {
        convertBase64StringToPdfAndStoreIt(base64Data, contentDisposition, mimetype);
    }

    public static String getBase64StringFromBlobUrl(String blobUrl, String mimetype) {
        if (blobUrl.startsWith("blob")) {
            return "javascript: var xhr = new XMLHttpRequest();" +
                    "xhr.open('GET', '" + blobUrl + "', true);" +
                    "xhr.setRequestHeader('Content-type','" + mimetype + "');" +
                    "xhr.responseType = 'blob';" +
                    "xhr.onload = function(e) {" +
                    "    if (this.status == 200) {" +
                    "        var blobPdf = this.response;" +
                    "        var reader = new FileReader();" +
                    "        reader.readAsDataURL(blobPdf);" +
                    "        reader.onloadend = function() {" +
                    "            base64data = reader.result;" +
                    "            fileName = document.querySelector('a[href=\"" + blobUrl + "\"]').getAttribute('download');" +
                    "            SnapdropAndroid.getBase64FromBlobData(base64data, fileName, \"" + mimetype + "\");" +
                    "        }" +
                    "    }" +
                    "};" +
                    "xhr.send();";
        }
        return "javascript: console.log('It is not a Blob URL');";
    }

    private void convertBase64StringToPdfAndStoreIt(String base64PDf, String contentDisposition, String mimetype) throws IOException {
        Log.e("BASE 64", base64PDf);
        Log.e("Filename", contentDisposition);
        final int notificationId = 1;

        final File dwldsPath = getFinalNewDestinationFile(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), contentDisposition);
        byte[] pdfAsBytes = Base64.decode(base64PDf.replaceFirst("^data:" + mimetype + ";base64,", ""), 0);
        if (dwldsPath.createNewFile()) {
            FileOutputStream os = new FileOutputStream(dwldsPath, false);
            os.write(pdfAsBytes);
            os.flush();
        }

        if (dwldsPath.exists()) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            Uri apkURI = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", dwldsPath);
            intent.setDataAndType(apkURI, MimeTypeMap.getSingleton().getMimeTypeFromExtension(getFileExtension(contentDisposition)));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            String CHANNEL_ID = "MYCHANNEL";
            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "download notification", NotificationManager.IMPORTANCE_LOW);
                Notification notification = new Notification.Builder(context, CHANNEL_ID)
                        .setContentText(dwldsPath.getName())
                        .setContentTitle("Download successful")
                        .setContentIntent(pendingIntent)
                        .setChannelId(CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .build();
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(notificationChannel);
                    notificationManager.notify(notificationId, notification);
                }

            } else {
                NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setWhen(System.currentTimeMillis())
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        //.setContentIntent(pendingIntent)
                        .setContentTitle("MY TITLE")
                        .setContentText("MY TEXT CONTENT");

                if (notificationManager != null) {
                    notificationManager.notify(notificationId, b.build());
                    Handler h = new Handler();
                    long delayInMilliseconds = 1000;
                    h.postDelayed(() -> notificationManager.cancel(notificationId), delayInMilliseconds);
                }
            }

            View coordinatorLayout = context.findViewById(R.id.coordinatorLayout);
            Snackbar snackbar = Snackbar.make(coordinatorLayout, "file downloaded", Snackbar.LENGTH_SHORT);

            final FrameLayout snackBarView = (FrameLayout) snackbar.getView();
            snackBarView.setBackground(context.getResources().getDrawable(R.drawable.snackbar_larger_margin));
            snackbar.show();
            context.uploadIntent = null;
        }
    }

    public static File getFinalNewDestinationFile(File destinationFolder, String filename) {

        File newFile = new File(destinationFolder, filename);
        if (!newFile.exists()) {
            return newFile;
        }

        String nameWithoutExtensionOrIncrement;
        String extension = getFileExtension(filename);

        if (extension != null) {
            extension = "." + extension;
            int extInd = filename.lastIndexOf(extension);
            nameWithoutExtensionOrIncrement = new StringBuilder(filename).replace(extInd, extInd + extension.length(), "").toString();
        } else {
            extension = "";
            nameWithoutExtensionOrIncrement = filename;
        }

        int c = 0;
        while (newFile.exists()) {
            c++;
            newFile = new File(destinationFolder, nameWithoutExtensionOrIncrement + " (" + c + ")" + extension);
        }
        return newFile;
    }


    public static String getFileExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int lastUnixPos = filename.lastIndexOf('/');
        int lastWindowsPos = filename.lastIndexOf('\\');
        int indexOfLastSeparator = Math.max(lastUnixPos, lastWindowsPos);
        int extensionPos = filename.lastIndexOf('.');
        int indexOfExtension = indexOfLastSeparator > extensionPos ? -1 : extensionPos;
        if (indexOfExtension == -1) {
            return null;
        } else {
            return filename.substring(indexOfExtension + 1).toLowerCase();
        }
    }
}