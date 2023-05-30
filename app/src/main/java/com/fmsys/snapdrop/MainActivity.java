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
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.splashscreen.SplashScreen;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.anggrayudi.storage.callback.FileCallback;
import com.anggrayudi.storage.file.DocumentFileCompat;
import com.anggrayudi.storage.file.DocumentFileType;
import com.anggrayudi.storage.file.DocumentFileUtils;
import com.anggrayudi.storage.media.FileDescription;
import com.anggrayudi.storage.media.MediaFile;
import com.fmsys.snapdrop.databinding.ActivityMainBinding;
import com.fmsys.snapdrop.utils.NetworkUtils;
import com.fmsys.snapdrop.utils.Nullable;
import com.fmsys.snapdrop.utils.ShareUtils;
import com.fmsys.snapdrop.utils.StateHandler;
import com.google.android.material.internal.ToolbarUtils;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 12321;

    private String baseURL;

    private ActivityMainBinding binding;
    public SharedPreferences prefs;

    public ValueCallback<Uri[]> uploadMessage;

    private final StateHandler state = new StateHandler(); // todo: view model instead?
    public boolean forceRefresh = false;
    public ObservableProperty<Boolean> transfer = new ObservableProperty<>(false); // todo: use view model
    public boolean onlyText = false;

    public final List<JavaScriptInterface.FileHeader> downloadFilesList = Collections.synchronizedList(new ArrayList<>());
    public boolean dialogVisible = false;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public Intent uploadIntent = null;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final boolean wasMobileData = binding.connectivityCard.getVisibility() == View.VISIBLE;
            final boolean wifiAvailable = NetworkUtils.isWiFi(intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO));
            prefs = PreferenceManager.getDefaultSharedPreferences(context);

            final boolean show = prefs.getBoolean(getString(R.string.pref_show_connectivity_card), true);
            if (show) {
                binding.connectivityCard.setVisibility(wifiAvailable ? View.GONE : View.VISIBLE);
            } else {
                binding.connectivityCard.setVisibility(View.GONE);
            }

            if (state.isCurrentlyOffline() || wasMobileData) {
                if (wifiAvailable) {
                    refreshWebsite();
                }
            }
        }
    };

    private final ActivityResultLauncher<Intent> openSettingsResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK);
            recreate();
        }
    });

    private final ActivityResultLauncher<Intent> selectFilesResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            uploadFromIntent(result.getData());
        } else if (uploadMessage != null) {
            uploadMessage.onReceiveValue(null);
            uploadMessage = null;
        }
    });


    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility", "RestrictedApi"})
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SplashScreen splashScreen = SplashScreen.installSplashScreen(this);


        SnapdropApplication.setAppTheme(this);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        baseURL = prefs.getString(getString(R.string.pref_baseurl), null);

        if (prefs.getBoolean(getString(R.string.pref_first_use), true) || baseURL == null) {
            OnboardingActivity.launchOnboarding(this);
            return; // all this doesn't make sense if we have no base URL
        }


        if (prefs.getBoolean(getString(R.string.pref_switch_keep_on), true)) {
            transfer.setOnChangedListener(transferActive -> runOnUiThread(() -> {
                if (transferActive) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }));
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.connectivityTextview.setText(baseURL.equals(getString(R.string.onboarding_server_pairdrop)) ? R.string.error_network_no_wifi : R.string.error_network);
        binding.retryButton.setOnClickListener(v -> {
            binding.noConnectionScreen.setVisibility(View.GONE);
            refreshWebsite();
        });
        binding.urlChangeButton.setOnClickListener(v -> OnboardingActivity.launchServerSelection(this));

        setSupportActionBar(binding.toolbar);
        setTitle(null);

        final ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setSubtitle(baseURL);
            Nullable.of(ToolbarUtils.getSubtitleTextView(binding.toolbar)).ifNotNull(tv -> tv.setOnClickListener(v -> OnboardingActivity.launchServerSelection(this)));
            actionbar.setHomeAsUpIndicator(R.drawable.ic_launcher_actionbar);
            actionbar.setHomeActionContentDescription(R.string.home_as_up_indicator_about);
            actionbar.setDisplayHomeAsUpEnabled(true);
        }

        final WebSettings webSettings = binding.webview.getSettings();
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE); // there are transfer problems when using cached resources
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setSupportMultipleWindows(true);
        if (prefs.contains(getString(R.string.pref_device_name))) {
            // Fake to user argent to be able to inject a custom string into the Snapdrop device name
            webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + "; Build/HUAWEI" + prefs.getString(getString(R.string.pref_device_name), getString(R.string.app_name)) + ") Version/" + BuildConfig.VERSION_NAME + (isTablet(this) ? " Tablet " : " Mobile ") + "Safari/537.36");
        }
        binding.webview.addJavascriptInterface(new JavaScriptInterface(MainActivity.this), "SnapdropAndroid");
        binding.webview.setWebChromeClient(new MyWebChromeClient());
        binding.webview.setWebViewClient(new CustomWebViewClient());

        // Allow webContentsDebugging if APK was build as debuggable
        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        if (SnapdropApplication.isDarkTheme(this) && WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(binding.webview.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
        }

        CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webview, true);

        binding.webview.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            Log.d("download listener", "search file of size " + contentLength);
            synchronized (downloadFilesList) {
                final Iterator<JavaScriptInterface.FileHeader> iterator = downloadFilesList.iterator();
                while (iterator.hasNext()) {
                    final JavaScriptInterface.FileHeader file = iterator.next();
                    Log.d("download listener", file.toString());
                    copyTempToDownloads(file);
                    iterator.remove();
                }
            }
        });

        binding.webview.setLongClickable(true);
        binding.webview.setOnLongClickListener(view -> {
            final WebView.HitTestResult hitTestResult = ((WebView) view).getHitTestResult();
            if (hitTestResult.getExtra() == null || !Patterns.WEB_URL.matcher(hitTestResult.getExtra()).matches()) {
                return false;
            }
            ShareUtils.shareUrl(this, hitTestResult.getExtra());
            return true;
        });

        refreshWebsite();
        onNewIntent(getIntent());

        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                && (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
        }

        binding.pullToRefresh.setOnRefreshListener(() -> {
            binding.noConnectionScreen.setVisibility(View.GONE);
            refreshWebsite(true);
        });

        final AnimatedVectorDrawableCompat loadAnimationDrawable = AnimatedVectorDrawableCompat.create(this, R.drawable.snapdrop_anim);
        binding.loadAnimator.setImageDrawable(loadAnimationDrawable);
        loadAnimationDrawable.registerAnimationCallback(new Animatable2Compat.AnimationCallback() {
            @Override
            public void onAnimationEnd(final Drawable drawable) {
                binding.loadAnimator.post(loadAnimationDrawable::start);
            }
        });
        loadAnimationDrawable.start();

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // file not added to download manager for android 7 and below, so this doesn't make sense (see #219)
        menu.findItem(R.id.menu_view_downloads).setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            toggleAbout();
            return true;
        } else if (item.getItemId() == R.id.menu_settings) {
            final Intent browserIntent = new Intent(MainActivity.this, SettingsActivity.class);
            openSettingsResultLauncher.launch(browserIntent);
            return true;
        } else if (item.getItemId() == R.id.menu_view_downloads) {
            startActivity(new Intent(this, FileBrowserActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleAbout() {
        if (binding.webview.getUrl() != null && binding.webview.getUrl().endsWith("#about")) {
            binding.webview.loadUrl(baseURL + "#");
        } else {
            binding.webview.loadUrl(baseURL + "#about");
        }
    }

    private void refreshWebsite(final boolean pulled) {
        Log.w("SnapdropAndroid", "refresh triggered");
        if (NetworkUtils.isInternetAvailable() && !transfer.get() && !dialogVisible || forceRefresh) {
            binding.connectivityCard.setVisibility(NetworkUtils.isWifiAvailable() ? View.GONE : View.VISIBLE);
            binding.webview.loadUrl(baseURL);
            forceRefresh = false;
            binding.webview.animate().alpha(0).start();
        } else if (transfer.get() || dialogVisible) {
            binding.pullToRefresh.setRefreshing(false);
            forceRefresh = pulled; //reset forceRefresh if after pullToRefresh the refresh request did come from another source eg onResume, so pullToRefresh doesn't unexpectedly force refreshes by "first time"
        } else {
            binding.pullToRefresh.setRefreshing(false);
            state.setCurrentlyLoading(false);
            showScreenNoConnection(true);
        }
    }

    private void refreshWebsite() {
        refreshWebsite(false);
    }

    private void showScreenNoConnection(boolean offline) {
        state.setCurrentlyLoading(false);
        state.setCurrentlyOffline(true);
        binding.noConnectionScreen.setVisibility(View.VISIBLE);
        binding.urlChangeButton.setVisibility(offline ? View.GONE : View.VISIBLE);
        binding.noConnectionTextview.setText(offline ? R.string.error_network_offline : R.string.error_server_not_reachable);
    }

    public static boolean isTablet(final Context ctx) {
        return (ctx.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        if ((Intent.ACTION_SEND.equals(intent.getAction()) || Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()) || Intent.ACTION_PROCESS_TEXT.equals(intent.getAction())) && intent.getType() != null) {
            uploadIntent = intent;

            binding.webview.clearFocus(); // remove potential text selections

            final String clipText = getTextFromUploadIntent();
            binding.webview.evaluateJavascript(JavaScriptInterface.getSendTextDialogWithPreInsertedString(clipText), null);

            final Snackbar snackbar = Snackbar
                    .make(binding.pullToRefresh, clipText.isEmpty() ? (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()) ? R.string.intent_files : R.string.intent_file) : R.string.intent_content, Snackbar.LENGTH_INDEFINITE)
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
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData().getHost().equals(Uri.parse(baseURL).getHost())) {
            binding.webview.loadUrl(intent.getDataString()); // e.g. paring URL
        } else {
            super.onNewIntent(intent);
        }
    }

    public void resetUploadIntent() {
        uploadIntent = null;
        onlyText = false;
    }

    @Override
    public void onBackPressed() {
        if (binding.webview.getUrl() != null && binding.webview.getUrl().endsWith("#about")) {
            binding.webview.loadUrl(baseURL + "#");
        } else if (dialogVisible) {
            binding.webview.loadUrl(JavaScriptInterface.getAssetsJS(this, "closeDialogs.js"));
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onRestart() {
        super.onRestart();
        if (binding.webview.getUrl() == null || !binding.webview.getUrl().startsWith(baseURL)) {
            refreshWebsite();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {
            Log.w("MainActivity.onStop", "No BroadcastReceiver registered");
        }
        if (!transfer.get() && !dialogVisible && uploadMessage == null) {
            binding.webview.loadUrl("about:blank");
        }
    }

    @Override
    protected void onDestroy() {
        if (binding != null) {
            binding.webview.loadUrl("about:blank");
        }
        CookieManager.getInstance().flush();
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

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && uploadIntent.hasExtra(Intent.EXTRA_PROCESS_TEXT)) {
                result.append(uploadIntent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT));
            }

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
                selectFilesResultLauncher.launch(intent);
            } catch (ActivityNotFoundException e) {
                uploadMessage = null;
                Snackbar.make(binding.pullToRefresh, R.string.error_filechooser, Snackbar.LENGTH_LONG).show();
                return false;
            }
            return true;
        }

        @Override
        public boolean onCreateWindow(final WebView view, final boolean dialog, final boolean userGesture, final Message resultMsg) {
            final String url = view.getHitTestResult().getExtra();
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (ActivityNotFoundException e) {
                Snackbar.make(binding.pullToRefresh, R.string.err_no_browser, Snackbar.LENGTH_SHORT).show();
                resetUploadIntent(); // the snackbar will dismiss the "files are selected" message, therefore also reset the upload intent.
            }
            return false;
        }

        @Override
        public boolean onConsoleMessage(final ConsoleMessage consoleMessage) {
            if (consoleMessage.messageLevel().equals(ConsoleMessage.MessageLevel.ERROR)) {
                Log.e("WebViewConsole", consoleMessage.message());
            } else if (consoleMessage.messageLevel().equals(ConsoleMessage.MessageLevel.WARNING)) {
                Log.w("WebViewConsole", consoleMessage.message());
            } else {
                Log.d("WebViewConsole", consoleMessage.message());
            }
            return true;
        }

        @Override
        public void onProgressChanged(final WebView view, final int newProgress) {
            super.onProgressChanged(view, newProgress);
            state.setLoadProgress(newProgress);
        }
    }

    private class CustomWebViewClient extends WebViewClient {

        @Override
        public void onPageFinished(final WebView view, final String url) {

            state.setCurrentlyLoading(false);
            binding.pullToRefresh.setRefreshing(false);

            if (url.startsWith(baseURL)) {
                Log.w("WebView", "refresh finished, init snapdrop...");
                state.setCurrentlyOffline(false);
                initSnapdrop();
            } else {
                binding.webview.animate().alpha(1).start();
                Log.w("WebView", "finished loading " + url);
            }

            super.onPageFinished(view, url);
        }

        private void initSnapdrop() {
            if (MainActivity.this.isFinishing()) {
                return; // too late to do anything at this point in time...
            }
            //website initialisation
            Log.w("WebView", "load init script...");
            binding.webview.evaluateJavascript(JavaScriptInterface.getAssetsJS(MainActivity.this, "init.js"),
                    returnValue -> binding.loadAnimator.animate().alpha(0).withEndAction(() -> binding.webview.animate().alpha(1).start()));
            binding.webview.evaluateJavascript(JavaScriptInterface.getSendTextDialogWithPreInsertedString(getTextFromUploadIntent()), null);
            WebsiteLocalizer.localize(binding.webview);
            Log.w("WebView", "init end.");
        }

        @Override
        public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            final Handler handler = new Handler();
            final int delay = 500; // milliseconds
            state.setCurrentlyLoading(true);

            // discover network loss while currently loading
            handler.postDelayed(new Runnable() {
                public void run() {
                    //do something
                    if (state.isCurrentlyLoading()) {
                        if (NetworkUtils.isInternetAvailable()) {
                            handler.postDelayed(this, delay);
                        } else {
                            refreshWebsite();
                        }
                    }
                }
            }, delay);

            // progress returns 10% even if server is unavailable.
            // Discover timeouts way before net::ERR_CONNECTION_TIMED_OUT gets triggered.
            // Don't use WebView.getProgress() as it will return zero if load is finished.
            handler.postDelayed(() -> {
                if (state.getLoadProgress() <= 10) {
                    handleTimeout();
                }
            }, 5000);

        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(final WebView view, final WebResourceRequest request, final WebResourceError error) {

            state.setCurrentlyLoading(false);

            Log.e("WebViewError", "Error on accessing " + request.getUrl() + ", " + error.getDescription() + " (ErrorCode " + error.getErrorCode() + ")");

            if (error.getErrorCode() == ERROR_CONNECT || error.getErrorCode() == ERROR_TIMEOUT) {
                Log.e("WebViewError", "Connection timed out!");
                showScreenNoConnection(false);
            } else {
                showScreenNoConnection(true);
            }
        }

        private void handleTimeout() {
            Log.e("WebViewError", "Connection timed out!");
            showScreenNoConnection(false);
        }
    }

    private void copyTempToDownloads(final JavaScriptInterface.FileHeader fileHeader) {
        if (Long.parseLong(fileHeader.getSize()) > 25 * 1024 * 1024) {
            Snackbar.make(binding.pullToRefresh, R.string.download_save_pending, Snackbar.LENGTH_INDEFINITE).show();
            resetUploadIntent(); // the snackbar will dismiss the "files are selected" message, therefore also reset the upload intent.
        }

        if (Build.VERSION.SDK_INT > 28) {
            fileDownloadedIntent(fileHeader.getFileUri(), fileHeader);
        } else {
            executor.execute(() -> {
                final FileDescription fileDescription = new FileDescription(fileHeader.getName(), "", fileHeader.getMime());
                final DocumentFile saveLocation = getSaveLocation();
                final DocumentFile source = DocumentFile.fromFile(new File(fileHeader.getFileUri().getPath()));
                if (saveLocation != null) {
                    DocumentFileUtils.moveFileTo(source, getApplicationContext(), saveLocation, fileDescription, fileCallback(fileHeader));
                } else {
                    onFailedMovingTempFile("Missing storage permissions");
                }
            });
        }
    }

    public static DocumentFile getSaveLocation() {
        final String downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        final Context context = SnapdropApplication.getInstance();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String path = preferences.getString(context.getString(R.string.pref_save_location), downloadsFolder);
        return DocumentFileCompat.fromFullPath(context, path, DocumentFileType.FOLDER, true);
    }

    public static boolean isCustomSaveLocation() {
        final Context context = SnapdropApplication.getInstance();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(context.getString(R.string.pref_save_location), null) != null;
    }

    private void onFailedMovingTempFile(final String errorMessage) {
        Log.d("SimpleStorage", errorMessage);
        handler.post(() -> {
            Snackbar.make(binding.pullToRefresh, errorMessage, Snackbar.LENGTH_LONG).show();
            resetUploadIntent(); // the snackbar will dismiss the "files are selected" message, therefore also reset the upload intent.
        });
    }

    private FileCallback fileCallback(final JavaScriptInterface.FileHeader fileHeader) {
        return new FileCallback() {
            @Override
            public void onFailed(@NonNull final FileCallback.ErrorCode errorCode) {
                onFailedMovingTempFile(errorCode.toString());
            }

            @Override
            public void onCompleted(@NonNull final Object file) {
                final Uri uri;

                if (file instanceof MediaFile) {
                    final MediaFile mediaFile = (MediaFile) file;
                    uri = mediaFile.getUri();
                } else if (file instanceof DocumentFile) {
                    final DocumentFile documentFile = (DocumentFile) file;
                    final Context context = getApplicationContext();
                    uri = DocumentFileUtils.isRawFile(documentFile)
                            ? FileProvider.getUriForFile(context, getPackageName() + ".provider", DocumentFileUtils.toRawFile(documentFile, context))
                            : documentFile.getUri();
                } else {
                    return;
                }

                fileDownloadedIntent(uri, fileHeader);
            }
        };
    }

    private void fileDownloadedIntent(final Uri uri, final JavaScriptInterface.FileHeader fileHeader) {
        final int notificationId = (int) SystemClock.uptimeMillis();
        final boolean isApk = fileHeader.getName().toLowerCase().endsWith(".apk");

        final Intent intent = new Intent();
        intent.setAction(isApk ? Intent.ACTION_INSTALL_PACKAGE : Intent.ACTION_VIEW);
        intent.setDataAndType(uri, isApk ? "application/vnd.android.package-archive" : fileHeader.getMime());
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        final PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 1, intent, Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT : PendingIntent.FLAG_CANCEL_CURRENT);
        final String channelId = "MYCHANNEL";
        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            final NotificationChannel notificationChannel = new NotificationChannel(channelId, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
            final Notification notification = new Notification.Builder(MainActivity.this, channelId)
                    .setContentText(fileHeader.getName())
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
                    .setContentText(fileHeader.getName());

            if (notificationManager != null) {
                notificationManager.notify(notificationId, b.build());
            }
        }

        final Snackbar snackbar = Snackbar.make(binding.pullToRefresh, R.string.download_successful, Snackbar.LENGTH_LONG)
                .setAction(isApk ? R.string.install : R.string.open, button -> {
                    try {
                        startActivity(intent);
                        notificationManager.cancel(notificationId);
                    } catch (Exception e) {
                        Snackbar.make(binding.pullToRefresh, R.string.err_no_app, Snackbar.LENGTH_SHORT).show();
                    }

                });
        snackbar.show();

        // the snackbar will dismiss the "files are selected" message, therefore also reset the upload intent.
        resetUploadIntent();
    }

}
