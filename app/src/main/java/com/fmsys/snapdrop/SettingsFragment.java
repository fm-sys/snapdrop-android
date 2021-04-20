package com.fmsys.snapdrop;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;


public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        final Preference versionPref = initUrlPreference(R.string.pref_version, "https://github.com/fm-sys/snapdrop-android/releases/latest");
        versionPref.setSummary("v" + BuildConfig.VERSION_NAME);

        initUrlPreference(R.string.pref_developers, "https://github.com/fm-sys/snapdrop-android");
        initUrlPreference(R.string.pref_crowdin, "https://crowdin.com/project/snapdrop-android");
        initUrlPreference(R.string.pref_twitter, "https://twitter.com/SnapdropAndroid");
        initUrlPreference(R.string.pref_license, "https://www.gnu.org/licenses/gpl-3.0.html");

        final Preference deviceNamePref = findPreference(getString(R.string.pref_device_name));
        deviceNamePref.setOnPreferenceClickListener(pref -> {

            final View dialogView = this.getLayoutInflater().inflate(R.layout.device_name_dialog, null);
            final EditText editText = dialogView.findViewById(R.id.textInput);
            editText.setText(PreferenceManager.getDefaultSharedPreferences(getContext()).getString(getString(R.string.pref_device_name), ""));
            editText.requestFocus();

            final AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                    .setTitle(R.string.pref_device_name_title)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.ok, (dialog, id) -> setDeviceName(editText.getText().toString()))
                    .setNegativeButton(R.string.reset, (dialog, id) -> setDeviceName(null));
            builder.create().show();
            return true;
        });
        
        final Preference baseUrlPref = findPreference(getString(R.string.pref_baseurl));
        baseUrlPref.setSummary(PreferenceManager.getDefaultSharedPreferences(getContext()).getString(baseUrlPref.getKey(),getString(R.string.baseURL)));
        baseUrlPref.setOnPreferenceChangeListener((preference, newValue) -> {
            baseUrlPref.setSummary(newValue.toString());
            return true;
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
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                return true;
            });
        }
        return preference;
    }

    private void setDeviceName(final String s) {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(getContext().getString(R.string.pref_device_name), s).apply();
    }
}
