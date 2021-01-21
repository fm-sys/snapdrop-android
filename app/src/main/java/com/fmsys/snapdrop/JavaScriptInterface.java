package com.fmsys.snapdrop;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;
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

    public JavaScriptInterface(final MainActivity context) {
        this.context = context;
    }

    @JavascriptInterface
    public void getBase64FromBlobData(final String base64Data, final String contentDisposition, final String mimetype) throws IOException {
        convertBase64StringToFileAndStoreIt(base64Data, contentDisposition, mimetype);
    }

    public static String getBase64StringFromBlobUrl(final String blobUrl, final String filename, final String mimetype) {
        if (blobUrl.startsWith("blob")) {
            return "javascript: " +
                    // "fileName = document.querySelector('a[href=\"" + blobUrl + "\"]').getAttribute('download');" + //sometimes returns null - that's why we hand over the filename explicitly
                    "fileName = \"" + filename + "\";" +
                    "" +
                    "var xhr = new XMLHttpRequest();" +
                    "xhr.open('GET', '" + blobUrl + "', true);" +
                    "xhr.setRequestHeader('Content-type','" + mimetype + "');" +
                    "xhr.responseType = 'blob';" +
                    "xhr.onload = function(e) {" +
                    "    if (this.status == 200) {" +
                    "        var blobFile = this.response;" +
                    "        var reader = new FileReader();" +
                    "        reader.readAsDataURL(blobFile);" +
                    "        reader.onloadend = function() {" +
                    "            base64data = reader.result;" +
                    "            SnapdropAndroid.getBase64FromBlobData(base64data, fileName, \"" + mimetype + "\");" +
                    "        }" +
                    "    }" +
                    "};" +
                    "xhr.send();";
        }
        return "javascript: console.log('It is not a Blob URL');";
    }

    private void convertBase64StringToFileAndStoreIt(final String base64file, final String contentDisposition, final String mimetype) throws IOException {
        final int notificationId = (int) SystemClock.uptimeMillis();

        final File dwldsPath = getFinalNewDestinationFile(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), contentDisposition);
        final byte[] fileAsBytes = Base64.decode(base64file.replaceFirst("^data:" + mimetype + ";base64,", ""), 0);
        if (dwldsPath.createNewFile()) {
            final FileOutputStream os = new FileOutputStream(dwldsPath, false);
            os.write(fileAsBytes);
            os.flush();
        }

        if (dwldsPath.exists()) {
            final Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            final Uri fileURI = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", dwldsPath);
            intent.setDataAndType(fileURI, MimeTypeMap.getSingleton().getMimeTypeFromExtension(getFileExtension(contentDisposition)));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            final PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            final String channelId = "MYCHANNEL";
            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                final NotificationChannel notificationChannel = new NotificationChannel(channelId, context.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
                final Notification notification = new Notification.Builder(context, channelId)
                        .setContentText(dwldsPath.getName())
                        .setContentTitle(context.getString(R.string.download_successful))
                        .setContentIntent(pendingIntent)
                        .setChannelId(channelId)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setAutoCancel(true)
                        .build();
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(notificationChannel);
                    notificationManager.notify(notificationId, notification);
                }

            } else {
                final NotificationCompat.Builder b = new NotificationCompat.Builder(context, channelId)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setWhen(System.currentTimeMillis())
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setContentTitle(context.getString(R.string.download_successful))
                        .setContentText(dwldsPath.getName());

                if (notificationManager != null) {
                    notificationManager.notify(notificationId, b.build());
                }
            }

            final View coordinatorLayout = context.findViewById(R.id.coordinatorLayout);
            final Snackbar snackbar = Snackbar.make(coordinatorLayout, R.string.download_successful, Snackbar.LENGTH_LONG)
                    .setAction(R.string.open, button -> {
                        try {
                            context.startActivity(intent);
                            notificationManager.cancel(notificationId);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    })
                    .setActionTextColor(context.getResources().getColor(R.color.colorAccent));

            final FrameLayout snackBarView = (FrameLayout) snackbar.getView();
            snackBarView.setBackground(context.getResources().getDrawable(R.drawable.snackbar_larger_margin));
            snackbar.show();

            // the shown snackbar will dismiss the older one which tells, that a file was selected for sharing. So to be consistent, we also remove the related intent
            context.resetUploadIntent();

            // This part can raise errors when the downloaded file is not a media file
            try {
                final DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                downloadManager.addCompletedDownload(dwldsPath.getName(), dwldsPath.getName(), true, MimeTypeMap.getSingleton().getMimeTypeFromExtension(getFileExtension(contentDisposition)), dwldsPath.getAbsolutePath(), dwldsPath.length(), false);
                MediaScannerConnection.scanFile(context, new String[]{dwldsPath.getPath()}, null, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static File getFinalNewDestinationFile(final File destinationFolder, final String filename) {

        File newFile = new File(destinationFolder, filename);
        if (!newFile.exists()) {
            return newFile;
        }

        final String nameWithoutExtensionOrIncrement;
        String extension = getFileExtension(filename);

        if (extension != null) {
            extension = "." + extension;
            final int extInd = filename.lastIndexOf(extension);
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


    public static String getFileExtension(final String filename) {
        if (filename == null) {
            return null;
        }
        final int lastUnixPos = filename.lastIndexOf('/');
        final int lastWindowsPos = filename.lastIndexOf('\\');
        final int indexOfLastSeparator = Math.max(lastUnixPos, lastWindowsPos);
        final int extensionPos = filename.lastIndexOf('.');
        final int indexOfExtension = indexOfLastSeparator > extensionPos ? -1 : extensionPos;
        if (indexOfExtension == -1) {
            return null;
        } else {
            return filename.substring(indexOfExtension + 1).toLowerCase();
        }
    }


    public static String getSendTextDialogWithPreInsertedString(final String text) {
        return "javascript: " +
                "var x = document.getElementById(\"textInput\").value=\"" + TextUtils.htmlEncode(text) + "\";";
    }
    public static String initialiseWebsite() {
        return "javascript: " +
                "window.addEventListener('file-received', e => {" +
                "   SnapdropAndroid.saveDownloadFileName(e.detail.name, e.detail.size);" +
                "}, false);" +

                "window.addEventListener('serverconnection-active', e => {" +
                "   SnapdropAndroid.updateLastOnlineTime();" +
                "}, false);" +

                "window.addEventListener('recipient-shortclicked', e => {" +
                "   if (SnapdropAndroid.shouldOpenSendTextDialog()) {" +
                "       Events.fire('text-recipient', e.detail);" +
                "   }" +
                "}, false);";
    }



    @JavascriptInterface
    public void updateLastOnlineTime() {
        context.prefs.edit().putLong(context.getString(R.string.pref_last_server_connection), System.currentTimeMillis()).apply();
    }

    @JavascriptInterface
    public void saveDownloadFileName(final String name, final String size) {
        context.downloadFilesList.add(Pair.create(name, size));
    }

    @JavascriptInterface
    public boolean shouldOpenSendTextDialog() {
        return context.onlyText;
    }

}
