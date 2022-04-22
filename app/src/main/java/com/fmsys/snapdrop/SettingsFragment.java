package com.fmsys.snapdrop;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.anggrayudi.storage.SimpleStorageHelper;
import com.anggrayudi.storage.file.DocumentFileUtils;
import com.fmsys.snapdrop.utils.ClipboardUtils;
import com.fmsys.snapdrop.utils.LogUtils;

public class SettingsFragment extends PreferenceFragmentCompat {

    private final SimpleStorageHelper storageHelper = new SimpleStorageHelper(this);
    private SharedPreferences prefs;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        if (savedInstanceState != null) {
            storageHelper.onRestoreInstanceState(savedInstanceState);
        }

        final Preference versionPref = initUrlPreference(R.string.pref_version, "https://github.com/fm-sys/snapdrop-android/releases/latest");
        versionPref.setSummary("v" + BuildConfig.VERSION_NAME);

        initUrlPreference(R.string.pref_developers, "https://github.com/fm-sys/snapdrop-android");
        initUrlPreference(R.string.pref_crowdin, "https://crowdin.com/project/snapdrop-android");
        initUrlPreference(R.string.pref_support, "https://github.com/fm-sys/snapdrop-android/blob/master/FUNDING.md");
        initUrlPreference(R.string.pref_twitter, "https://twitter.com/intent/tweet?text=@SnapdropAndroid%20-%20%22Snapdrop%20for%20Android%22%20is%20an%20Android%20client%20for%20https://snapdrop.net&");
        initUrlPreference(R.string.pref_license, "https://www.gnu.org/licenses/gpl-3.0.html");

        final Preference openSourceComponents = findPreference(getString(R.string.pref_components));
        openSourceComponents.setOnPreferenceClickListener(pref -> {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.components)
                    .setMessage(R.string.components_long_text)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return true;
        });

        final Preference logsPref = findPreference(getString(R.string.pref_logs));
        logsPref.setOnPreferenceClickListener(pref -> {
            final View dialogView = this.getLayoutInflater().inflate(R.layout.debug_logs_dialog, null);
            final TextView textView = dialogView.findViewById(R.id.textview);
            textView.setText(LogUtils.getLogs(prefs, true));

            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.logs)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNeutralButton(R.string.copy, (d, id) -> ClipboardUtils.copy(this.getContext(), LogUtils.getLogs(prefs, false)))
                    .show();
            return true;
        });


        final Preference deviceNamePref = findPreference(getString(R.string.pref_device_name));
        deviceNamePref.setOnPreferenceClickListener(pref -> showEditTextPreferenceWithResetPossibility(pref, "Android ", "", newValue -> updateDeviceNameSummary(deviceNamePref)));
        updateDeviceNameSummary(deviceNamePref);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        final Preference baseUrlPref = findPreference(getString(R.string.pref_baseurl));
        baseUrlPref.setOnPreferenceClickListener(pref -> showEditTextPreferenceWithResetPossibility(pref, "", getString(R.string.baseURL), newValue -> baseUrlPref.setSummary(newValue != null ? newValue : getString(R.string.baseURL))));
        baseUrlPref.setSummary(preferences.getString(baseUrlPref.getKey(), getString(R.string.baseURL)));

        final Preference saveLocationPref = findPreference(getString(R.string.pref_save_location));
        saveLocationPref.setOnPreferenceClickListener(preference -> {
            storageHelper.openFolderPicker();
            return true;
        });
        final String downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        saveLocationPref.setSummary(preferences.getString(saveLocationPref.getKey(), downloadsFolder));
        storageHelper.setOnFolderSelected((requestCode, folder) -> {
            final String path = DocumentFileUtils.getAbsolutePath(folder, requireContext());
            setPreferenceValue(saveLocationPref.getKey(), path, null);
            saveLocationPref.setSummary(path);
            return null;
        });

        final Preference themePref = findPreference(getString(R.string.pref_theme_setting));
            themePref.setOnPreferenceChangeListener((Preference preference, Object newValue) -> {
                final DarkModeSetting darkTheme = DarkModeSetting.valueOf((String) newValue);
                SnapdropApplication.setAppTheme(darkTheme);
                requireActivity().setResult(Activity.RESULT_OK);
                requireActivity().recreate();
                return true;
            });
    }

    private Preference initUrlPreference(final @StringRes int pref, final String url) {
        final Preference preference = findPreference(getString(pref));
        if (preference != null) {
            preference.setOnPreferenceClickListener(p -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this.getContext(), R.string.err_no_browser, Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }
        return preference;
    }

    private void setPreferenceValue(final String preferenceKey, final String s, final Consumer<String> onPreferenceChangeCallback) {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(preferenceKey, s).apply();

        if (onPreferenceChangeCallback != null) {
            onPreferenceChangeCallback.accept(s);
        }
    }

    private void updateDeviceNameSummary(final Preference pref) {
        if (prefs.contains(getString(R.string.pref_device_name))) {
            pref.setSummary("Android " + prefs.getString(getString(R.string.pref_device_name), getString(R.string.app_name)));
        } else {
            pref.setSummary(R.string.pref_device_name_summary);
        }
    }

    private boolean showEditTextPreferenceWithResetPossibility(final Preference pref, final String prefix, final @NonNull String defaultValue, final Consumer<String> onPreferenceChangeCallback) {
        final View dialogView = this.getLayoutInflater().inflate(R.layout.edit_text_dialog, null);
        final EditText editText = dialogView.findViewById(R.id.textInput);
        editText.setTag(prefix);
        editText.setText(PreferenceManager.getDefaultSharedPreferences(getContext()).getString(pref.getKey(), defaultValue));
        editText.requestFocus();

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(pref.getTitle())
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> setPreferenceValue(pref.getKey(), editText.getText().toString(), onPreferenceChangeCallback))
                .setNegativeButton(R.string.reset, (dialog, id) -> setPreferenceValue(pref.getKey(), null, onPreferenceChangeCallback));
        builder.create().show();
        return true;
    }

    @Override
    public void onSaveInstanceState(final @NonNull Bundle outState) {
        storageHelper.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

//    final String appPackageName = context.getPackageName();
//        try {
//        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
//    } catch (android.content.ActivityNotFoundException anfe) {
//        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
//    }
}
