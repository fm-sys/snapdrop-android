package com.fmsys.snapdrop;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.fmsys.snapdrop.utils.LogUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class SnapdropApplication extends Application {

    private static SnapdropApplication instance;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US);

    public SnapdropApplication() {
        instance = this;
    }

    public static Application getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        installUncaughtExceptionHandler();
        setAppTheme(getApplicationContext());
        super.onCreate();
    }

    public static void setAppTheme(final @NonNull Context context) {
        setAppTheme(getAppTheme(context));
        context.setTheme(R.style.AppTheme);
    }

    public static void setAppTheme(final DarkModeSetting setting) {
        AppCompatDelegate.setDefaultNightMode(setting.getModeId());
    }

    private static DarkModeSetting getAppTheme(final @NonNull Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return DarkModeSetting.valueOf(prefs.getString(context.getString(R.string.pref_theme_setting), DarkModeSetting.SYSTEM_DEFAULT.getPreferenceValue(context)));
    }

    private static boolean isDarkThemeActive(final @NonNull Context context, final DarkModeSetting setting) {
        if (setting == DarkModeSetting.SYSTEM_DEFAULT) {
            return isDarkThemeActive(context);
        } else {
            return setting == DarkModeSetting.DARK;
        }
    }

    private static boolean isDarkThemeActive(final @NonNull Context context) {
        final int uiMode = context.getResources().getConfiguration().uiMode;
        return (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    public static boolean isDarkTheme(final @NonNull Context context) {
        return isDarkThemeActive(context, getAppTheme(context));
    }

    @SuppressLint("ApplySharedPref")
    private void installUncaughtExceptionHandler() {
        final Thread.UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {


            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putString(getString(R.string.pref_last_crash), "--------- Last crash\n" + sdf.format(new Date()) + " " + LogUtils.getStacktrace(ex))
                    .commit();

            // Call the default handler
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, ex);
            }
        });
    }
}
