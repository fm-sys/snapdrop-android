package com.fmsys.snapdrop;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import java.lang.reflect.Method;

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

    public static boolean isWifiAvailable() {
        final NetworkInfo activeNetworkInfo = getConnManager().getActiveNetworkInfo();

        if (activeNetworkInfo != null && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) { // WiFi
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
}
