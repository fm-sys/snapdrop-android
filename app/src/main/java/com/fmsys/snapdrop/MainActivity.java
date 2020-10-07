package com.fmsys.snapdrop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends Activity {
    private static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 12321;
    public WebView webView;
    public SwipeRefreshLayout pullToRefresh;

    public ValueCallback<Uri[]> uploadMessage;
    public static final int REQUEST_SELECT_FILE = 100;

    // workaround because snapdrop website do not show the correct devices after first load
    private boolean loadAgain = true;

    public Intent uploadIntent = null;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        pullToRefresh = findViewById(R.id.pullToRefresh);

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
        webView.addJavascriptInterface(new JavaScriptInterface(MainActivity.this), "SnapdropAndroid");
        webView.setWebChromeClient(new MyWebChromeClient());
        webView.clearCache(true);
        webView.setWebViewClient(new WebViewClient() {

            public void onPageFinished(WebView view, String url) {
                if (loadAgain && url.equals("https://snapdrop.net/")) {
                    loadAgain = false;
                    refreshWebsite();
                }
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> webView.loadUrl(JavaScriptInterface.getBase64StringFromBlobUrl(url, mimetype)));

        refreshWebsite();

        Intent intent = getIntent();
        if ((Intent.ACTION_SEND.equals(intent.getAction()) || Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) && intent.getType() != null) {
            uploadIntent = intent;

            View coordinatorLayout = findViewById(R.id.coordinatorLayout);
            Snackbar snackbar = Snackbar
                    .make(coordinatorLayout, "A file is selected for sharing", Snackbar.LENGTH_INDEFINITE)
                    .setAction("drop", button -> uploadIntent = null)
                    .setActionTextColor(getResources().getColor(R.color.colorAccent));


            final FrameLayout snackBarView = (FrameLayout) snackbar.getView();
            snackBarView.setBackground(getResources().getDrawable(R.drawable.snackbar_larger_margin));
            snackbar.show();
        }

        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                && (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
        }

        pullToRefresh.setOnRefreshListener(() -> {
            refreshWebsite();
            pullToRefresh.setRefreshing(false);
        });

    }

    private void refreshWebsite() {
        if (isInternetAvailable()) {
            webView.loadUrl("https://snapdrop.net/");
        } else {
            String offlineHtml = "<html><body><div style=\"width: 100%; text-align:center; position: absolute; top: 20%; font-size: 30px;\"><svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" width=\"500px\" height=\"500px\" fill=\"gray\" width=\"48px\" height=\"48px\"><path d=\"M0 0h24v24H0zm0 0h24v24H0z\" fill=\"none\"/><path d=\"M22 6V4H6.82l2 2H22zM1.92 1.65L.65 2.92l1.82 1.82C2.18 5.08 2 5.52 2 6v11H0v3h17.73l2.35 2.35 1.27-1.27L3.89 3.62 1.92 1.65zM4 6.27L14.73 17H4V6.27zM23 8h-6c-.55 0-1 .45-1 1v4.18l2 2V10h4v7h-2.18l3 3H23c.55 0 1-.45 1-1V9c0-.55-.45-1-1-1z\"/></svg><br/><br/>Please verify that you are connected with a WiFi network</div></body></html>";
            webView.loadData(offlineHtml, "text/html", "utf-8");
        }
    }

    public boolean isInternetAvailable() {
        final ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr != null) {
            NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == RESULT_OK) {
            uploadFromIntent(intent);
        } else {
            uploadMessage.onReceiveValue(null);
            uploadMessage = null;
        }


    }

    private void uploadFromIntent(final Intent intent) {
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
            Toast.makeText(MainActivity.this, "File chooser failed", Toast.LENGTH_SHORT).show();
        }
        uploadMessage.onReceiveValue(results);
        uploadMessage = null;
    }

    class MyWebChromeClient extends WebChromeClient {

        public boolean onShowFileChooser(final WebView mWebView, final ValueCallback<Uri[]> filePathCallback, final FileChooserParams fileChooserParams) {
            if (uploadMessage != null) {
                uploadMessage.onReceiveValue(null);
                uploadMessage = null;
            }

            uploadMessage = filePathCallback;

            if (uploadIntent != null) {
                uploadFromIntent(uploadIntent);
                return true;
            }


            final Intent intent = fileChooserParams.createIntent();
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            try {
                startActivityForResult(intent, REQUEST_SELECT_FILE);
            } catch (ActivityNotFoundException e) {
                uploadMessage = null;
                Toast.makeText(MainActivity.this, "Cannot Open File Chooser", Toast.LENGTH_LONG).show();
                return false;
            }
            return true;
        }

        @Override
        public boolean onCreateWindow(final WebView view, final boolean dialog, final boolean userGesture, final Message resultMsg) {
            final String data = view.getHitTestResult().getExtra();
            final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(data));
            startActivity(browserIntent);
            return false;
        }


    }

}
