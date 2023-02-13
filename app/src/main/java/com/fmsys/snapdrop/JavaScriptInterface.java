package com.fmsys.snapdrop;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.fmsys.snapdrop.utils.ClipboardUtils;

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

    /**
     * This method is really slow. Use only as fallback.
     */
    public static String downloadBlobUrlIntoTemp(final String blobUrl, final String filename, final String mimetype, final long size) {
        if (blobUrl.startsWith("blob")) {
            return "javascript: " +
                    (filename != null ? "fileName = \"" + filename + "\";" : "fileName = document.querySelector('a[href=\"" + blobUrl + "\"]').getAttribute('download');") + // querySelector sometimes returns null - that's why we try to hand over the filename explicitly
                    "var decoder = new TextDecoder(\"iso-8859-1\");" +
                    "var xhr = new XMLHttpRequest();" +
                    "xhr.open('GET', '" + blobUrl + "', true);" +
                    "xhr.setRequestHeader('Content-type','" + mimetype + "');" +
                    "xhr.responseType = 'blob';" +
                    "xhr.onload = function(e) {" +
                    "    if (this.status == 200) {" +
                    "        var blobFile = this.response;" +
                    "        SnapdropAndroid.newFile(fileName,'" + mimetype + "', '" + size + "');" +
                    "        const reader = blobFile.stream().getReader();" +
                    "        function push() {" +
                    "            reader.read().then(({ done, value }) => {" +
                    "               if (done) {" +
                    "                   SnapdropAndroid.saveDownloadFileName(fileName, '" + size + "');" +
                    "                   return;" +
                    "               }" +
                    "               SnapdropAndroid.onBytes(decoder.decode(value));" +
                    "               push();" +
                    "            });" +
                    "        };" +
                    "        push();" +
                    "    }" +
                    "};" +
                    "xhr.send();";
        }
        return "javascript: console.log('It is not a Blob URL');";
    }

    @JavascriptInterface
    public void newFile(final String fileName, final String mimeType, final String fileSize) throws IOException {
        Log.e("DownloadListener", "Create file of size " + fileSize);
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
        Log.e("DownloadListener", "Write bytes...");

        if (fileOutputStream == null) {
            Log.e("DownloadListener", "No input stream open!");
            return;
        }
        //https://stackoverflow.com/questions/27034897/is-there-a-way-to-pass-an-arraybuffer-from-javascript-to-java-on-android
        final byte[] bytes = dec.getBytes("windows-1252");
        fileOutputStream.write(bytes);
        fileOutputStream.flush();
    }

    @JavascriptInterface
    public void saveDownloadFileName(final String name, final String size) throws IOException {
        Log.e("DownloadListener", "Finished file of size " + size);

        fileOutputStream.flush();
        fileOutputStream.close();

        context.downloadFilesList.add(fileHeader);

        fileOutputStream = null;
        fileHeader = null;
    }


    public static String getSendTextDialogWithPreInsertedString(final String text) {
        return "javascript: " +
                "var x = document.getElementById(\"textInput\").innerHTML=\"" + TextUtils.htmlEncode(text) + "\";";
    }

    @JavascriptInterface
    public void copyToClipboard(final String text) {
        ClipboardUtils.copy(context, text);
    }

    @JavascriptInterface
    public String getYouAreKnownAsTranslationString(final String displayName) {
        return context.getString(R.string.website_footer_known_as, displayName);
    }

    @JavascriptInterface
    public int getVersionId() {
        return BuildConfig.VERSION_CODE;
    }

    @JavascriptInterface
    public boolean shouldOpenSendTextDialog() {
        return context.onlyText;
    }

    @JavascriptInterface
    public void dialogShown() {
        context.dialogVisible = true;
    }

    @JavascriptInterface
    public void dialogHidden() {
        context.dialogVisible = false;
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
