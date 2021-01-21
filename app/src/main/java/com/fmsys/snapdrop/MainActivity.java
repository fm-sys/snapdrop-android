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
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 12321;

    private static final String baseURL = "https://fm-sys.github.io/snapdrop/client/";
    //private static final String baseURL = "https://snapdrop.net/";

    public WebView webView;
    public SharedPreferences prefs;
    public LinearLayoutCompat imageViewLayout;
    public SwipeRefreshLayout pullToRefresh;

    public ValueCallback<Uri[]> uploadMessage;
    public static final int REQUEST_SELECT_FILE = 100;

    private boolean loadAgain = true; // workaround cause Snapdrop website does not show the correct devices after first load
    private boolean currentlyOffline = true;
    private boolean currentlyLoading = false;

    public boolean onlyText = false;
    public List<Pair<String, String>> downloadFilesList = new ArrayList<>(); // name - size

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
        webView.clearCache(true);

        // Allow webContentsDebugging if APK was build as debuggable
        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        final CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // check if the last server connection was in the past minute - if yes we don't create a new UUID as the "old peer" might still be visible
        if (System.currentTimeMillis() - prefs.getLong(getString(R.string.pref_last_server_connection), 0) > 60000) {
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

        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                && (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
        }

        pullToRefresh.setOnRefreshListener(() -> {
            refreshWebsite();
            pullToRefresh.setRefreshing(false);
        });

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(receiver, intentFilter);

        new UpdateChecker().execute("");
    }

    private void refreshWebsite() {
        if (isInternetAvailable()) {
            webView.loadUrl(baseURL);
        } else {
            showScreenNoConnection();
        }
    }

    private void showScreenNoConnection() {
        final String offlineHtml = "<html><body><div style=\"width: 100%; text-align:center; position: absolute; top: 20%; font-size: 36px;\"><svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" width=\"500px\" height=\"500px\" fill=\"gray\" width=\"48px\" height=\"48px\"><path d=\"M0 0h24v24H0zm0 0h24v24H0z\" fill=\"none\"/><path d=\"M22 6V4H6.82l2 2H22zM1.92 1.65L.65 2.92l1.82 1.82C2.18 5.08 2 5.52 2 6v11H0v3h17.73l2.35 2.35 1.27-1.27L3.89 3.62 1.92 1.65zM4 6.27L14.73 17H4V6.27zM23 8h-6c-.55 0-1 .45-1 1v4.18l2 2V10h4v7h-2.18l3 3H23c.55 0 1-.45 1-1V9c0-.55-.45-1-1-1z\"/></svg><br/><br/>" + getString(R.string.error_network) + "</div></body></html>";
        webView.loadData(offlineHtml, "text/html", "utf-8");
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
                    .setAction(android.R.string.cancel, button -> resetUploadIntent())
                    .setActionTextColor(getResources().getColor(R.color.colorAccent));
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

        if (resultCode == RESULT_OK) {
            uploadFromIntent(intent);
        } else {
            uploadMessage.onReceiveValue(null);
            uploadMessage = null;
        }

    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
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
            final Intent browserIntent;
            if (url.endsWith("update.html#settings")) {
                browserIntent = new Intent(MainActivity.this, AboutActivity.class);
            } else {
                browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            }
            startActivity(browserIntent);
            return false;
        }

    }

    private class CustomWebViewClient extends WebViewClient {

        @Override
        public void onPageFinished(final WebView view, final String url) {
            currentlyLoading = false;

            if (url.startsWith(baseURL)) {
                currentlyOffline = false;

                if (loadAgain) {
                    loadAgain = false;
                    new Handler().postDelayed(MainActivity.this::refreshWebsite, 300);
                } else {
                    //website initialisation
                    webView.loadUrl(JavaScriptInterface.initialiseWebsite());
                    webView.loadUrl(JavaScriptInterface.getSendTextDialogWithPreInsertedString(getTextFromUploadIntent()));
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
                if (result == null) {
                    return;
                }

                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogTheme)
                        .setTitle(R.string.app_update)
                        .setMessage(R.string.app_update_summary)
                        .setPositiveButton(R.string.app_update_install, (dialog, id) -> UpdateUtils.runUpdate(MainActivity.this, result))
                        .setNegativeButton(R.string.app_update_show_details, (dialog, id) -> UpdateUtils.showUpdatesInBrowserIntent(MainActivity.this));
                builder.create().show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
