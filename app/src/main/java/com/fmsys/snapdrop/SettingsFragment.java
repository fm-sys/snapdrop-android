package com.fmsys.snapdrop;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;
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
        deviceNamePref.setOnPreferenceClickListener(pref -> showEditTextPreferenceWithResetPossibility(pref, "Android ", "", null));
        
        final Preference baseUrlPref = findPreference(getString(R.string.pref_baseurl));
        baseUrlPref.setOnPreferenceClickListener(pref -> showEditTextPreferenceWithResetPossibility(pref, "", getString(R.string.baseURL), newValue -> baseUrlPref.setSummary(newValue != null ? newValue : getString(R.string.baseURL))));
        baseUrlPref.setSummary(PreferenceManager.getDefaultSharedPreferences(getContext()).getString(baseUrlPref.getKey(), getString(R.string.baseURL)));

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
}
