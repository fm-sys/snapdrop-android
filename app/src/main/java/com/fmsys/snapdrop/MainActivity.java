package com.fmsys.snapdrop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.anggrayudi.storage.callback.FileCallback;
import com.anggrayudi.storage.file.DocumentFileUtils;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 12321;
    private static final int LAUNCH_SETTINGS_ACTIVITY = 12;
    public static final int REQUEST_SELECT_FILE = 100;

    //private static final String baseURL = "https://fm-sys.github.io/snapdrop/client/";
    private static final String baseURL = "https://snapdrop.net/";

    public WebView webView;
    public SharedPreferences prefs;
    public LinearLayoutCompat imageViewLayout;
    public SwipeRefreshLayout pullToRefresh;

    public ValueCallback<Uri[]> uploadMessage;

    private boolean currentlyOffline = true;
    private boolean currentlyLoading = false;
    public boolean forceRefresh = false;
    public boolean transfer = false;
    public boolean onlyText = false;

    public List<JavaScriptInterface.FileHeader> downloadFilesList = new ArrayList<>(); // name - size

    public Intent uploadIntent = null;

    private ConnectivityManager connMgr = null;
    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            if ((!isInitialStickyBroadcast()) && currentlyOffline) {
                final Handler handler = new Handler();
                handler.postDelayed(() -> {
                    if (isWifiAvailable()) {
                        refreshWebsite();
                    }
                }, 1500); // wait a bit until connection is ready

            }
        }
    };


    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (prefs.getBoolean(getString(R.string.pref_switch_keep_on), false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        pullToRefresh = findViewById(R.id.pullToRefresh);
        imageViewLayout = findViewById(R.id.splashImage);
        connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        webView.getSettings().setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setSupportMultipleWindows(true);
        if (prefs.contains(getString(R.string.pref_device_name))) {
            webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + "; Build/HUAWEI" + prefs.getString(getString(R.string.pref_device_name), getString(R.string.app_name)) + ") Version/" + BuildConfig.VERSION_NAME + (isTablet(this) ? " Tablet " : " Mobile ") + "Safari/537.36");
        }
        webView.addJavascriptInterface(new JavaScriptInterface(MainActivity.this), "SnapdropAndroid");
        webView.setWebChromeClient(new MyWebChromeClient());
        webView.setWebViewClient(new CustomWebViewClient());

        // Allow webContentsDebugging if APK was build as debuggable
        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        if (SnapdropApplication.isDarkTheme(this) && WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(webView.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
        }

        final CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            for (JavaScriptInterface.FileHeader file : downloadFilesList) {
                if (file.size.equals(String.valueOf(contentLength))) {
                    copyTempToDownloads(file);
                    downloadFilesList.remove(file);
                    break;
                }
            }
        });

        refreshWebsite();
        onNewIntent(getIntent());

        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                && (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
        }

        pullToRefresh.setOnRefreshListener(() -> {
            refreshWebsite(true);
            pullToRefresh.setRefreshing(false);
        });

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(receiver, intentFilter);

        new UpdateChecker().execute("");
    }

    private void refreshWebsite(final boolean pulled) {
        if (isWifiAvailable() && !transfer || forceRefresh) {
            webView.loadUrl(baseURL);
            forceRefresh = false;
        } else if (transfer) {
            forceRefresh = pulled; //reset forceRefresh if after pullToRefresh the refresh request did come from another source eg onResume, so pullToRefresh doesn't unexpectedly force refreshes by "first time"
        } else {
            showScreenNoConnection();
        }
    }

    private void refreshWebsite() {
        refreshWebsite(false);
    }

    private void showScreenNoConnection() {
        webView.loadUrl("file:///android_asset/offline.html?text=" + getString(R.string.error_network) + "&button=" + getString(R.string.ignore_error_network));
        currentlyOffline = true;
    }

    private boolean isWifiAvailable() {
        if (connMgr != null) {
            final NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        }
        return false;
    }

    private boolean isInternetAvailable() {
        if (connMgr != null) {
            final NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    public static boolean isTablet(final Context ctx) {
        return (ctx.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        if ((Intent.ACTION_SEND.equals(intent.getAction()) || Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) && intent.getType() != null) {
            uploadIntent = intent;

            final String clipText = getTextFromUploadIntent();
            webView.loadUrl(JavaScriptInterface.getSendTextDialogWithPreInsertedString(clipText));

            final View coordinatorLayout = findViewById(R.id.coordinatorLayout);
            final Snackbar snackbar = Snackbar
                    .make(coordinatorLayout, clipText.isEmpty() ? (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()) ? R.string.intent_files : R.string.intent_file) : R.string.intent_content, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.cancel, button -> resetUploadIntent());
            snackbar.show();

            onlyText = true;
            final Uri[] results = getUploadFromIntentUris(intent);
            if (results != null) {
                for (Uri uri : results) {
                    if (uri != null) {
                        onlyText = false;
                        break;
                    }
                }
            }
        }
    }

    public void resetUploadIntent() {
        uploadIntent = null;
        onlyText = false;
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUEST_SELECT_FILE) {
            if (resultCode == RESULT_OK) {
                uploadFromIntent(intent);
            } else if (uploadMessage != null) {
                uploadMessage.onReceiveValue(null);
                uploadMessage = null;
            }
        } else if (requestCode == LAUNCH_SETTINGS_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK);
                recreate();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.getUrl().endsWith("#about")) {
            webView.loadUrl(baseURL + "#");
        } else {
            super.onBackPressed();
        }
    }

    /**
     * @return true if there was no server connection for more than three minutes
     */
    private boolean onlinePastThreeMin() {
        return System.currentTimeMillis() - prefs.getLong(getString(R.string.pref_last_server_connection), 0) > 1000 * 60 * 3;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (onlinePastThreeMin()) {
            refreshWebsite();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        webView.loadUrl("about:blank");
        super.onDestroy();
    }

    private void uploadFromIntent(final Intent intent) {
        if (uploadMessage != null) {
            uploadMessage.onReceiveValue(getUploadFromIntentUris(intent));
            uploadMessage = null;
        }
    }

    private Uri[] getUploadFromIntentUris(final Intent intent) {
        Uri[] results = null;
        try {
            final String dataString = intent.getDataString();
            final ClipData clipData = intent.getClipData();
            if (clipData != null) {
                results = new Uri[clipData.getItemCount()];
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    final ClipData.Item item = clipData.getItemAt(i);
                    results[i] = item.getUri();
                }
            }
            if (dataString != null) {
                results = new Uri[]{Uri.parse(dataString)};
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    private String getTextFromUploadIntent() {
        final StringBuilder result = new StringBuilder();
        if (uploadIntent != null) {
            final ClipData clip = uploadIntent.getClipData();
            if (clip != null && clip.getItemCount() > 0) {
                for (int i = 0; i < clip.getItemCount(); i++) {
                    final ClipData.Item item = clip.getItemAt(i);
                    if (item.getText() != null) {
                        result.append(item.getText()).append(" ");
                    }
                }
            }
        }
        return result.toString().trim();
    }


    class MyWebChromeClient extends WebChromeClient {

        public boolean onShowFileChooser(final WebView mWebView, final ValueCallback<Uri[]> filePathCallback, final FileChooserParams fileChooserParams) {
            if (uploadMessage != null) {
                uploadMessage.onReceiveValue(null);
                uploadMessage = null;
            }

            uploadMessage = filePathCallback;

            if (uploadIntent != null) {
                try {
                    uploadFromIntent(uploadIntent);
                    return true;
                } catch (Exception e) {
                    // pass - can happen, when a text is selected for sharing instead of a file
                }
            }


            final Intent intent = fileChooserParams.createIntent();
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            try {
                startActivityForResult(intent, REQUEST_SELECT_FILE);
            } catch (ActivityNotFoundException e) {
                uploadMessage = null;
                Toast.makeText(MainActivity.this, R.string.error_filechooser, Toast.LENGTH_LONG).show();
                return false;
            }
            return true;
        }

        @Override
        public boolean onCreateWindow(final WebView view, final boolean dialog, final boolean userGesture, final Message resultMsg) {
            final String url = view.getHitTestResult().getExtra();
            if (url.endsWith("update.html#settings")) {
                final Intent browserIntent = new Intent(MainActivity.this, AboutActivity.class);
                startActivityForResult(browserIntent, LAUNCH_SETTINGS_ACTIVITY);
            } else if (url.endsWith("offlineButForceRefresh")) {
                forceRefresh = true;
                imageViewLayout.setVisibility(View.VISIBLE);
                pullToRefresh.setVisibility(View.GONE);
                refreshWebsite();
            } else {
                final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
            return false;
        }

    }

    private class CustomWebViewClient extends WebViewClient {

        @Override
        public void onPageFinished(final WebView view, final String url) {
            currentlyLoading = false;

            if (url.startsWith(baseURL) || currentlyOffline) {
                currentlyOffline = !url.startsWith(baseURL);

                initSnapdrop();

            }
            super.onPageFinished(view, url);
        }

        private void initSnapdrop() {
            //website initialisation
            if (!currentlyOffline) {
                webView.loadUrl(JavaScriptInterface.initialiseWebsite());
                webView.loadUrl(JavaScriptInterface.getSendTextDialogWithPreInsertedString(getTextFromUploadIntent()));

                // welcome dialog
                if (prefs.getBoolean(getString(R.string.pref_first_use), true)) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                            .setCancelable(false)
                            .setTitle(R.string.app_welcome)
                            .setMessage(R.string.app_welcome_summary)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> prefs.edit().putBoolean(getString(R.string.pref_first_use), false).apply());
                    builder.create().show();
                }
            }

            imageViewLayout.setVisibility(View.GONE);
            pullToRefresh.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            final Handler handler = new Handler();
            final int delay = 500; // milliseconds
            currentlyLoading = true;

            handler.postDelayed(new Runnable() {
                public void run() {
                    //do something
                    if (currentlyLoading) {
                        if (isInternetAvailable()) {
                            handler.postDelayed(this, delay);
                        } else {
                            refreshWebsite();
                        }
                    }
                }
            }, delay);

        }

        @Override
        public void onReceivedError(final WebView view, final WebResourceRequest request, final WebResourceError error) {
            showScreenNoConnection();
        }

    }

    private class UpdateChecker extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(final String... params) {
            try {
                return UpdateUtils.checkUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(final String result) {
            try {
                if (result == null || uploadIntent != null) {
                    return;
                }

                if (UpdateUtils.isInstalledViaGooglePlay(MainActivity.this)) { // simplified and less disturbing message for PlayStore users
                    final Snackbar snackbar = Snackbar
                            .make(findViewById(R.id.coordinatorLayout), R.string.app_update_short_summary, Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.app_update_install, button -> UpdateUtils.showAppInMarket(MainActivity.this));
                    snackbar.show();
                    new Handler().postDelayed(snackbar::dismiss, 10000); // 10 seconds
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.app_update)
                            .setMessage(R.string.app_update_summary)
                            .setPositiveButton(R.string.app_update_install, (dialog, id) -> UpdateUtils.showAppInMarket(MainActivity.this))
                            .setNegativeButton(R.string.app_update_show_details, (dialog, id) -> UpdateUtils.showUpdatesInBrowserIntent(MainActivity.this));
                    builder.create().show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());

    public void copyTempToDownloads(JavaScriptInterface.FileHeader fileHeader) {

        executor.execute(() -> {

            DocumentFileUtils.moveFileTo(DocumentFile.fromFile(fileHeader.path), this, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileHeader.name, new FileCallback() {
                @Override
                public void onFailed(@NotNull FileCallback.ErrorCode errorCode) {
                    Log.d("SimpleStorage", errorCode.toString());
                }

                @Override
                public void onCompleted(@NotNull Object file) {
                    DocumentFile documentFile = (DocumentFile) file;
                    Uri uri = DocumentFileUtils.isRawFile(documentFile) ? FileProvider.getUriForFile(MainActivity.this, getApplicationContext().getPackageName() + ".provider", DocumentFileUtils.toRawFile(documentFile)) : documentFile.getUri();

                    //UI Thread
                    handler.post(() -> fileDownloadedIntent(uri, fileHeader));

                }
            });

        });

    }

    private void fileDownloadedIntent(Uri uri, JavaScriptInterface.FileHeader fileHeader) {
        final int notificationId = (int) SystemClock.uptimeMillis();

        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, fileHeader.mime);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        final PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        final String channelId = "MYCHANNEL";
        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            final NotificationChannel notificationChannel = new NotificationChannel(channelId, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
            final Notification notification = new Notification.Builder(MainActivity.this, channelId)
                    .setContentText(fileHeader.name)
                    .setContentTitle(getString(R.string.download_successful))
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
            final NotificationCompat.Builder b = new NotificationCompat.Builder(MainActivity.this, channelId)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setContentTitle(getString(R.string.download_successful))
                    .setContentText(fileHeader.name);

            if (notificationManager != null) {
                notificationManager.notify(notificationId, b.build());
            }
        }

        final View coordinatorLayout = MainActivity.this.findViewById(R.id.coordinatorLayout);
        final Snackbar snackbar = Snackbar.make(coordinatorLayout, R.string.download_successful, Snackbar.LENGTH_LONG)
                .setAction(R.string.open, button -> {
                    try {
                        startActivity(intent);
                        notificationManager.cancel(notificationId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                });
        snackbar.show();

        // the shown snackbar will dismiss the older one which tells, that a file was selected for sharing. So to be consistent, we also remove the related intent
        resetUploadIntent();
    }

}
