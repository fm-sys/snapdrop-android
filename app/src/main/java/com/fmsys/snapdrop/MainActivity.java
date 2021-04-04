package com.fmsys.snapdrop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.os.Handler;
import android.os.Message;
import android.util.Pair;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 12321;
    private static final int LAUNCH_SETTINGS_ACTIVITY = 12;
    public static final int REQUEST_SELECT_FILE = 100;

    private static final String baseURL = "https://fm-sys.github.io/snapdrop/client/";
    //private static final String baseURL = "https://snapdrop.net/";

    public WebView webView;
    public SharedPreferences prefs;
    public LinearLayoutCompat imageViewLayout;
    public SwipeRefreshLayout pullToRefresh;

    public ValueCallback<Uri[]> uploadMessage;

    private boolean loadAgain = true; // workaround cause Snapdrop website does not show the correct devices after first load
    private boolean currentlyOffline = true;
    private boolean currentlyLoading = false;

    public boolean onlyText = false;
    public List<Pair<String, String>> downloadFilesList = new ArrayList<>(); // name - size
    public boolean transfer = false;
    private boolean forceRefresh = false;

    public Intent uploadIntent = null;

    private ConnectivityManager connMgr = null;
    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            if ((!isInitialStickyBroadcast()) && currentlyOffline) {
                final Handler handler = new Handler();
                handler.postDelayed(() -> {
                    if (isInternetAvailable()) {
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

        // check if the last server connection was in the past 3 minutes - if yes we don't create a new UUID as the "old peer" might still be visible
        if (System.currentTimeMillis() - prefs.getLong(getString(R.string.pref_last_server_connection), 0) > 1000 * 60 * 3) {
            WebStorage.getInstance().deleteAllData();

            cookieManager.setCookie("https://snapdrop.net/",
                    "peerid=" + UUID.randomUUID().toString() + ";" +
                            "path=/server;" +
                            "max-age=86400;"
            );
        }

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            String filename = null;
            int pos = 0;
            for (Pair<String, String> file : downloadFilesList) {
                pos++;

                if (file.second.equals(String.valueOf(contentLength))) {
                    filename = file.first;
                    break;
                }
            }
            downloadFilesList = downloadFilesList.subList(pos, downloadFilesList.size());
            webView.loadUrl(JavaScriptInterface.getBase64StringFromBlobUrl(url, filename, mimetype));
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
        if (isInternetAvailable() && !transfer || forceRefresh) {
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
        webView.loadUrl("file:///android_asset/offline.html?" + getString(R.string.error_network));
        currentlyOffline = true;
    }

    public boolean isInternetAvailable() {
        if (connMgr != null) {
            final NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
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
            } else {
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

    @Override
    public void onResume() {
        super.onResume();
        refreshWebsite();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        webView.loadUrl("about:blank");
        super.onDestroy();
    }

    private void uploadFromIntent(final Intent intent) {
        uploadMessage.onReceiveValue(getUploadFromIntentUris(intent));
        uploadMessage = null;
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

                if (loadAgain) {
                    loadAgain = false;
                    new Handler().postDelayed(MainActivity.this::refreshWebsite, 400);
                } else {
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
            }
            super.onPageFinished(view, url);
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
}
