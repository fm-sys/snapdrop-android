package com.fmsys.snapdrop.utils;

import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;

import com.fmsys.snapdrop.R;
import com.fmsys.snapdrop.SnapdropApplication;
import com.google.android.material.snackbar.Snackbar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class NetworkUtils {
    private NetworkUtils() {
        // utility class
    }

    private static ConnectivityManager getConnManager() {
        return (ConnectivityManager) SnapdropApplication.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    private static WifiManager getWiFiManager() {
        return (WifiManager) SnapdropApplication.getInstance().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public static boolean isWiFi(final NetworkInfo networkInfo) {
        return networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI && networkInfo.getState() == NetworkInfo.State.CONNECTED;
    }

    public static boolean isWifiAvailable() {
        if (isWiFi(getConnManager().getActiveNetworkInfo())) { // WiFi
            return true;
        } else if (isInternetAvailable()) { // Maybe sharing the internet connection via hotspot?
            final WifiManager wifiManager = getWiFiManager();
            try {
                final Method method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
                method.setAccessible(true);
                if ((boolean) method.invoke(wifiManager)) {
                    return true;
                }
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean isInternetAvailable() {
        final NetworkInfo activeNetworkInfo = getConnManager().getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void checkInstance(final Fragment fragment, final String url, final Consumer<Boolean> result) {
        final Dialog dialog = new Dialog(fragment.getContext());
        dialog.setContentView(R.layout.progress_dialog);

        final Future<?> request = Executors.newSingleThreadExecutor().submit(() -> {
            try {
                final Document doc = Jsoup.connect(url).get();
                fragment.requireActivity().runOnUiThread(() -> {
                    if (doc.selectFirst("x-peers") != null) {
                        // website seems to be similar to snapdrop... The check could be improved of course.
                        result.accept(true);
                        Snackbar.make(fragment.requireView(), R.string.baseurl_instance_verified, Snackbar.LENGTH_LONG).show();
                    } else {
                        Snackbar.make(fragment.requireView(), R.string.baseurl_no_snapdrop_instance, Snackbar.LENGTH_LONG).show();
                        result.accept(false);
                    }
                });
            } catch (Exception e) {
                Log.e("BaseUrlChange", "Failed to verify Snapdrop instance: " + e.getMessage());
                fragment.requireActivity().runOnUiThread(() -> {
                    Snackbar.make(fragment.requireView(), R.string.baseurl_check_instance_failed, Snackbar.LENGTH_LONG).show();
                    result.accept(false);
                });
            }
            dialog.dismiss();
        });

        dialog.setOnCancelListener(d -> request.cancel(true));
        dialog.show();
    }
}
