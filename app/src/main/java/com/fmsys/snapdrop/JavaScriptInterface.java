package com.fmsys.snapdrop;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class JavaScriptInterface {
    private final MainActivity context;

    private FileOutputStream fileOutputStream;
    private FileHeader fileHeader;

    public JavaScriptInterface(final MainActivity context) {
        this.context = context;
    }

    @JavascriptInterface
    public void newFile(final String fileName, final String mimeType, final String fileSize) throws IOException {
        final File outputDir = context.getCacheDir();
        final String[] nameSplit = fileName.split("\\.");
        while (nameSplit[0].length() < 3) {
            nameSplit[0] += nameSplit[0];
        }
        final File tempFile = File.createTempFile(nameSplit[0], "." + nameSplit[nameSplit.length - 1], outputDir);
        fileOutputStream = new FileOutputStream(tempFile);
        fileHeader = new FileHeader(fileName, mimeType, fileSize, tempFile);
    }

    @JavascriptInterface
    public void onBytes(final String dec) throws IOException {
        if (fileOutputStream == null) {
            return;
        }
        //https://stackoverflow.com/questions/27034897/is-there-a-way-to-pass-an-arraybuffer-from-javascript-to-java-on-android
        final byte[] bytes = dec.getBytes("windows-1252");
        fileOutputStream.write(bytes);
        fileOutputStream.flush();
    }

    @JavascriptInterface
    public void saveDownloadFileName(final String name, final String size) throws IOException {
        fileOutputStream.flush();
        fileOutputStream.close();

        context.downloadFilesList.add(fileHeader);
    }


    public static String getSendTextDialogWithPreInsertedString(final String text) {
        return "javascript: " +
                "var x = document.getElementById(\"textInput\").innerHTML=\"" + TextUtils.htmlEncode(text) + "\";";
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
    public boolean shouldOpenSendTextDialog() {
        return context.onlyText;
    }
    
    @JavascriptInterface
    public void setProgress(final float progress) {
        if (progress > 0) {
            context.transfer.set(true);
        } else {
            context.transfer.set(false);
            context.forceRefresh = false; //reset forceRefresh after transfer finished so pullToRefresh doesn't unexpectedly force refreshes by "first time"
        }
    }

    public static class FileHeader {
        private final String name;
        private final String mime;
        private final String size;
        private final File file;

        public FileHeader(final String name, final String mime, final String size, final File path) {
            this.name = name;
            this.mime = mime;
            this.size = size;
            this.file = path;
        }

        public String getName() {
            return name;
        }

        public String getMime() {
            return mime;
        }

        public String getSize() {
            return size;
        }

        public File getTempFile() {
            return file;
        }
    }

    public static String getAssetsJS(final Context context, final String fileName) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(fileName), StandardCharsets.UTF_8))) {
            final StringBuilder text = new StringBuilder("javascript:");
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                if (!currentLine.startsWith("//")) {
                    text.append(currentLine);
                }
            }
            return text.toString();
        } catch (IOException e) {
            Log.e("JavaScriptInterface", "unable to read assets file '" + fileName + "'", e);
        }
        return null;
    }
}
