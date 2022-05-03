package com.fmsys.snapdrop;

import com.anggrayudi.storage.extension.UriUtils;
import com.anggrayudi.storage.file.DocumentFileCompat;
import com.anggrayudi.storage.file.DocumentFileUtils;
import com.anggrayudi.storage.media.FileDescription;
import com.fmsys.snapdrop.utils.ClipboardUtils;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import androidx.documentfile.provider.DocumentFile;

public class JavaScriptInterface {
    private final MainActivity context;

    private OutputStream fileOutputStream;
    private FileHeader fileHeader;

    public JavaScriptInterface(final MainActivity context) {
        this.context = context;
    }

    @JavascriptInterface
    public void newFile(final String fileName, final String mimeType, final String fileSize) throws IOException {
        final Context context = this.context.getApplicationContext();
        Uri fileUri = null;
        if (Build.VERSION.SDK_INT > 28) {
            /*
            Make file transfer faster 2x on scoped storage by writing to media store database directly,
            instead of writing to temporary file first. It could save storage lifetime because
            the file is written once only.
             */
            final DocumentFile saveLocation = MainActivity.getSaveLocation();
            if (saveLocation != null) {
                final DocumentFile file = DocumentFileUtils.makeFile(saveLocation, context, fileName, mimeType);
                if (file != null) {
                    fileUri = file.getUri();
                }
            }
            if (fileUri == null) {
                final FileDescription description = new FileDescription(fileName, "Snapdrop", mimeType);
                fileUri = DocumentFileCompat.createDownloadWithMediaStoreFallback(context, description);
            }
        } else {
            /*
            Prior to scoped storage restriction, SimpleStorage will use File#renameTo(), so no need to worry
            about the storage's lifetime.
             */
            final String[] nameSplit = fileName.split("\\.");
            while (nameSplit[0].length() < 3) {
                nameSplit[0] += nameSplit[0];
            }
            fileUri = Uri.fromFile(File.createTempFile(nameSplit[0], "." + nameSplit[nameSplit.length - 1], context.getCacheDir()));
        }
        if (fileUri == null) {
            throw new IOException("Missing storage permissions");
        }
        fileOutputStream = UriUtils.openOutputStream(fileUri, context);
        if (fileOutputStream == null) {
            throw new IOException("Cannot write target file");
        }
        fileHeader = new FileHeader(fileName, mimeType, fileSize, fileUri);
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
    public void updateLastOnlineTime() {
        context.setLastServerConnection(System.currentTimeMillis());
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
        private final Uri fileUri;

        public FileHeader(final String name, final String mime, final String size, final Uri fileUri) {
            this.name = name;
            this.mime = mime;
            this.size = size;
            this.fileUri = fileUri;
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

        public Uri getFileUri() {
            return fileUri;
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
