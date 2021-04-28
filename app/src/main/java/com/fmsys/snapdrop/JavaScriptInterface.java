package com.fmsys.snapdrop;

import  android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.webkit.JavascriptInterface;

import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.webkit.WebViewFeature;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class JavaScriptInterface {
    private MainActivity context;
    private File tempFile;
    private FileOutputStream fileOutputStream;
    private FileHeader fileHeader;

    public JavaScriptInterface(final MainActivity context) {
        this.context = context;
    }

    @JavascriptInterface
    public void newFile(final String fileName, final String mimeType, final String fileSize) throws IOException {
        File outputDir = context.getCacheDir();
        String[] nameSplit = fileName.split("\\.");

        tempFile = File.createTempFile(nameSplit[0], nameSplit[nameSplit.length-1], outputDir);
        fileOutputStream = new FileOutputStream(tempFile);
        fileHeader = new FileHeader(fileName, mimeType, fileSize, tempFile);
    }

    @JavascriptInterface
    public void onBytes(String dec) throws IOException {
        if (fileOutputStream == null) {return;}
        //https://stackoverflow.com/questions/27034897/is-there-a-way-to-pass-an-arraybuffer-from-javascript-to-java-on-android
        byte[] bytes = dec.getBytes("windows-1252");
        fileOutputStream.write(bytes);
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

    /**
     * Initialises the website (GUI and JS changes)
     *
     * Good practice: We should always try to touch the js as little as possible (add code instead of overriding) not to break anything should there be minor changes to the website
     *
     * @return java script string which needs to be executed in the webview.
     */
    public static String initialiseWebsite() {
        return "javascript: " +
                //change ReceiveTextDialog._onCopy to connect to JavaScriptInterface (don't call super method as it will throw an NotAllowedError)
                "ReceiveTextDialog.prototype._onCopy = function(){" +
                "               SnapdropAndroid.copyToClipboard(this.$text.textContent);" +
                "               Events.fire('notify-user', 'Copied to clipboard');" +
                "            };" +
                //change PeerUI.setProgress(progress) to connect to JavaScriptInterface
                "PeerUI.prototype.sP = PeerUI.prototype.setProgress;" +
                "PeerUI.prototype.setProgress = function(progress){" +
                "               SnapdropAndroid.setProgress(progress);" +
                "               this.sP(progress);" +
                "            };" +
            
                //change tweet link
                "document.querySelector('.icon-button[title~=\"Tweet\"]').href = 'https://twitter.com/intent/tweet?text=@SnapdropAndroid%20-%20An%20Android%20client%20for https://snapdrop.net%20by%20@robin_linus%20&';" +
            
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

                "let aboutIconButton = document.querySelector('.icon-button[href=\"#about\"]');" +
                "aboutIconButton.parentElement.insertBefore(settingsIconButton, aboutIconButton.nextSibling);" +

                //add footer to about page
                "let websiteLinkDiv = document.createElement('div');" +
                "websiteLinkDiv.style.cssText = 'height:10%; position:absolute; bottom:0px;';" +

                "let websiteLink = document.createElement('a');" +
                "websiteLink.href = 'https://snapdrop.net/';" +
                "websiteLink.target = '_blank';" +

                "let websiteLinkText = document.createElement('h4');" +
                "websiteLinkText.style.cssText = 'font-size: 16px; font-weight: 400; letter-spacing: .5em; margin: 16px 0;';" +
                "websiteLinkText.innerText = 'WWW.SNAPDROP.NET';" +

                "let aboutScreen = document.querySelector('#about>section');" +
                "websiteLinkDiv.appendChild(websiteLink);" +
                "websiteLink.appendChild(websiteLinkText);" +
                "aboutScreen.appendChild(websiteLinkDiv);" +

                //remove donation button (play guidelines)
                "document.querySelector('.icon-button[href*=\"paypal\"]').hidden = true;" +
            
                //remove "safari hack"                 
                "document.body.onclick = null;" +
            
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
                "                   Events.fire('text-recipient', this._peer.id);" +
                "               }" +
                "            };" +
            
                "window.addEventListener('file-received', e => {" +
                "   SnapdropAndroid.saveDownloadFileName(e.detail.name, e.detail.size);" +
                "}, false);" +

                //catch chunks
                "Peer.prototype._oFH = Peer.prototype._onFileHeader;" +
                "Peer.prototype._onFileHeader = function(header){" +
                "               SnapdropAndroid.newFile(header.name,header.mime, header.size);" +
                "               this._oFH(header);" +
                "            };" +
                "Peer.prototype._oCR = Peer.prototype._onChunkReceived;" +
                "Peer.prototype._onChunkReceived = function(chunk){" +
                "               let decoder = new TextDecoder('iso-8859-1');" +
                "               SnapdropAndroid.onBytes(decoder.decode(chunk));" +
                "               this._oCR(chunk);" +
                "            };" +

                (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK) ? "document.getElementById('theme').hidden = true;" : "");
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
            context.transfer = true;
        } else {
            context.transfer = false;
            context.forceRefresh = false; //reset forceRefresh after transfer finished so pullToRefresh doesn't unexpectedly force refreshes by "first time"
        }
    }

    public class FileHeader {
        String name;
        String mime;
        String size;
        File path;

        public FileHeader(String name, String mime, String size, File path) {
            this.name = name;
            this.mime = mime;
            this.size = size;
            this.path = path;
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

        public File getPath() {
            return path;
        }
    }

}
