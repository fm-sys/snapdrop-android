package com.fmsys.snapdrop;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;

import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.webkit.WebViewFeature;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaScriptInterface {
    private MainActivity context;

    public JavaScriptInterface(final MainActivity context) {
        this.context = context;
    }

    @JavascriptInterface
    public void getBase64FromBlobData(final String base64Data, final String contentDisposition) throws IOException {
        convertBase64StringToFileAndStoreIt(base64Data, contentDisposition);
    }

    public static String getBase64StringFromBlobUrl(final String blobUrl, final String filename, final String mimetype) {
        if (blobUrl.startsWith("blob")) {
            return "javascript: " +
                    (filename != null ? "fileName = \"" + filename + "\";" : "fileName = document.querySelector('a[href=\"" + blobUrl + "\"]').getAttribute('download');") + // querySelector sometimes returns null - that's why we try to hand over the filename explicitly
                    "" +
                    "var xhr = new XMLHttpRequest();" +
                    "xhr.open('GET', '" + blobUrl + "', true);" +
                    "xhr.setRequestHeader('Content-type','" + mimetype + "');" +
                    "xhr.responseType = 'blob';" +
                    "xhr.onload = function(e) {" +
                    "    if (this.status == 200) {" +
                    "        var blobFile = this.response;" +

                    //TODO: Do not load the complete file at once
                    "        var reader = new FileReader();" +
                    "        reader.readAsDataURL(blobFile);" +
                    "        reader.onloadend = function() {" +
                    "            base64data = reader.result;" +
                    "            SnapdropAndroid.getBase64FromBlobData(base64data, fileName);" +
                    "        }" +
                    "    }" +
                    "};" +
                    "xhr.send();";
        }
        return "javascript: console.log('It is not a Blob URL');";
    }

    private void convertBase64StringToFileAndStoreIt(final String base64file, final String contentDisposition) throws IOException {
        final int notificationId = (int) SystemClock.uptimeMillis();

        final Matcher m = Pattern.compile("^data:(.+);base64,").matcher(base64file.substring(0, 100));
        String mimetype = null;
        if (m.find()) {
            mimetype = m.group(1);
        }

        android.util.Log.e("name", contentDisposition);
        android.util.Log.e("mimetype", mimetype);

        final byte[] fileAsBytes = Base64.decode(base64file.replaceFirst("^data:.+;base64,", ""), 0);

        Uri uri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            final ContentResolver resolver = context.getContentResolver();
            final ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Downloads.DISPLAY_NAME, contentDisposition);
            contentValues.put(MediaStore.Downloads.MIME_TYPE, mimetype);
            contentValues.put(MediaStore.Downloads.DATE_ADDED, System.currentTimeMillis());
            contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            final OutputStream outputStream = resolver.openOutputStream(uri);
            outputStream.write(fileAsBytes);
            outputStream.close();
        } else {
            final File dwldsPath = getFinalNewDestinationFile(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), contentDisposition);
            if (dwldsPath.createNewFile()) {
                final FileOutputStream os = new FileOutputStream(dwldsPath, false);
                os.write(fileAsBytes);
                os.flush();
                uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", dwldsPath);
            }
            // This part can raise errors when the downloaded file is not a media file
            try {
                final DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                downloadManager.addCompletedDownload(contentDisposition, contentDisposition, true, MimeTypeMap.getSingleton().getMimeTypeFromExtension(getFileExtension(contentDisposition)), dwldsPath.getAbsolutePath(), dwldsPath.length(), false);
                MediaScannerConnection.scanFile(context, new String[]{dwldsPath.getPath()}, null, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (uri != null) {
            final Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, MimeTypeMap.getSingleton().getMimeTypeFromExtension(getFileExtension(contentDisposition)));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            final PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            final String channelId = "MYCHANNEL";
            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                final NotificationChannel notificationChannel = new NotificationChannel(channelId, context.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
                final Notification notification = new Notification.Builder(context, channelId)
                        .setContentText(contentDisposition)
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
                        .setContentText(contentDisposition);

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

                    });
            snackbar.show();

            // the shown snackbar will dismiss the older one which tells, that a file was selected for sharing. So to be consistent, we also remove the related intent
            context.resetUploadIntent();
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
                "var x = document.getElementById(\"textInput\").innerHTML=\"" + TextUtils.htmlEncode(text) + "\";";
    }
    public static String initialiseWebsite() {
        return "javascript: " +
                //change ReceiveTextDialog._onCopy to connect to JavaScriptInterface
                "ReceiveTextDialog.prototype._oC = ReceiveTextDialog.prototype._onCopy;" +
                "ReceiveTextDialog.prototype._onCopy = function(){" +
                "               this._oC();" +
                "               SnapdropAndroid.copyToClipboard(this.$text.textContent);" +
                "            };" +
                //change PeerUI.setProgress(progress) to connect to JavaScriptInterface
                "PeerUI.prototype.sP = PeerUI.prototype.setProgress;" +
                "PeerUI.prototype.setProgress = function(progress){" +
                "               SnapdropAndroid.setProgress(progress);" +
                "               this.sP(progress);" +
                "            };" +
            
                //change tweet link
                "document.querySelector('.icon-button[title~='Tweet']').href = 'https://twitter.com/intent/tweet?text=@SnapdropAndroid%20-%20An%20Android%20client%20for https://snapdrop.net%20by%20@robin_linus%20&';" +
            
                //add settings icon-button
                "let settingsIconButton = document.createElement('a');" +
                "settingsIconButton.className = 'icon-button';" +
                "settingsIconButton.target = '_blank';" +
                "settingsIconButton.href = 'update.html#settings';" +
                "settingsIconButton.title = 'App Settings';" +
                "settingsIconButton.rel = 'noreferrer';" +

                "let settingsSvg = document.createElementNS('http://www.w3.org/2000/svg','svg');" +
                "settingsSvg.setAttribute('class', 'icon');" +

                "let settingsPath = document.createElementNS('http://www.w3.org/2000/svg','path');" +
                "settingsPath.setAttribute('d', 'M19.43 12.98c.04-.32.07-.64.07-.98 0-.34-.03-.66-.07-.98l2.11-1.65c.19-.15.24-.42.12-.64l-2-3.46c-.09-.16-.26-.25-.44-.25-.06 0-.12.01-.17.03l-2.49 1c-.52-.4-1.08-.73-1.69-.98l-.38-2.65C14.46 2.18 14.25 2 14 2h-4c-.25 0-.46.18-.49.42l-.38 2.65c-.61.25-1.17.59-1.69.98l-2.49-1c-.06-.02-.12-.03-.18-.03-.17 0-.34.09-.43.25l-2 3.46c-.13.22-.07.49.12.64l2.11 1.65c-.04.32-.07.65-.07.98 0 .33.03.66.07.98l-2.11 1.65c-.19.15-.24.42-.12.64l2 3.46c.09.16.26.25.44.25.06 0 .12-.01.17-.03l2.49-1c.52.4 1.08.73 1.69.98l.38 2.65c.03.24.24.42.49.42h4c.25 0 .46-.18.49-.42l.38-2.65c.61-.25 1.17-.59 1.69-.98l2.49 1c.06.02.12.03.18.03.17 0 .34-.09.43-.25l2-3.46c.12-.22.07-.49-.12-.64l-2.11-1.65zm-1.98-1.71c.04.31.05.52.05.73 0 .21-.02.43-.05.73l-.14 1.13.89.7 1.08.84-.7 1.21-1.27-.51-1.04-.42-.9.68c-.43.32-.84.56-1.25.73l-1.06.43-.16 1.13-.2 1.35h-1.4l-.19-1.35-.16-1.13-1.06-.43c-.43-.18-.83-.41-1.23-.71l-.91-.7-1.06.43-1.27.51-.7-1.21 1.08-.84.89-.7-.14-1.13c-.03-.31-.05-.54-.05-.74s.02-.43.05-.73l.14-1.13-.89-.7-1.08-.84.7-1.21 1.27.51 1.04.42.9-.68c.43-.32.84-.56 1.25-.73l1.06-.43.16-1.13.2-1.35h1.39l.19 1.35.16 1.13 1.06.43c.43.18.83.41 1.23.71l.91.7 1.06-.43 1.27-.51.7 1.21-1.07.85-.89.7.14 1.13zM12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm0 6c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z');" +
                "settingsSvg.appendChild(settingsPath);" +
                "settingsIconButton.appendChild(settingsSvg);" +

                "let aboutIconButton = document.querySelector('.icon-button[href='#about']');" +
                "aboutIconButton.parentElement.insertBefore(settingsIconButton, aboutIconButton.nextSibling);" +
            
                //change ServerConnection.send(message) to connect to JavaScriptInterface
                "ServerConnection.prototype.s = ServerConnection.prototype.send;" +
                "ServerConnection.prototype.send = function(message){" +
                "               this.s(message);" +
                "               if (message.type == 'pong') {" +
                "                   SnapdropAndroid.updateLastOnlineTime();" +
                "               }" +
                "            };" +
                
                //change PeerUI._onTouchEnd(e) to connect to JavaScriptInterface
                "PeerUI.prototype._oTE = PeerUI.prototype._onTouchEnd;" +
                "PeerUI.prototype._onTouchEnd = function(e){" +
                "               this._oTE(e);" +
                "               if ((Date.now() - this._touchStart < 500) && SnapdropAndroid.shouldOpenSendTextDialog()) {" +
                "                   Events.fire('text-recipient', e.detail);" +
                "               }" +
                "            };" +
            
                "window.addEventListener('file-received', e => {" +
                "   SnapdropAndroid.saveDownloadFileName(e.detail.name, e.detail.size);" +
                "}, false);" + (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK) ? "document.getElementById('theme').hidden = true;" : "");
    }

    @JavascriptInterface
    public void copyToClipboard(final String text) {
        final ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        final ClipData clip = ClipData.newPlainText("SnapdropAndroid", text);
        clipboard.setPrimaryClip(clip);
    }

    @JavascriptInterface
    public int getVersionId() {
        return BuildConfig.VERSION_CODE;
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
