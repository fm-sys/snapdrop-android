package com.fmsys.snapdrop;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.LinkMovementMethod;
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

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.anggrayudi.storage.SimpleStorageHelper;
import com.anggrayudi.storage.file.DocumentFileUtils;
import com.fmsys.snapdrop.utils.ClipboardUtils;
import com.fmsys.snapdrop.utils.LogUtils;
import com.google.android.material.snackbar.Snackbar;

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
        initUrlPreference(R.string.pref_twitter, "https://twitter.com/intent/tweet?text=@SnapdropAndroid%20-%20%22Snapdrop%20for%20Android%22%20is%20an%20Android%20client%20for%20%23snapdrop%0A%0Ahttps://snapdrop.net");
        initUrlPreference(R.string.pref_license, "https://www.gnu.org/licenses/gpl-3.0.html");

        final Preference openSourceComponents = findPreference(getString(R.string.pref_components));
        openSourceComponents.setOnPreferenceClickListener(pref -> {
            final AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setTitle(R.string.components)
                    .setMessage(R.string.components_long_text)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
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

        final Preference floatingTextSelectionPref = findPreference(getString(R.string.pref_floating_text_selection));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            floatingTextSelectionPref.setVisible(true);
            floatingTextSelectionPref.setOnPreferenceChangeListener((pref, newValue) -> {
                getContext().getPackageManager().setComponentEnabledSetting(
                        new ComponentName(getContext(), FloatingTextActivity.class),
                        (Boolean) newValue ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
                return true;
            });
        }

        final Preference deviceNamePref = findPreference(getString(R.string.pref_device_name));
        deviceNamePref.setOnPreferenceClickListener(pref -> showEditTextPreferenceWithResetPossibility(pref, "Android ", "", newValue -> updateDeviceNameSummary(deviceNamePref)));
        updateDeviceNameSummary(deviceNamePref);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        final Preference baseUrlPref = findPreference(getString(R.string.pref_baseurl));
        baseUrlPref.setOnPreferenceClickListener(pref -> showEditTextPreferenceWithResetPossibility(pref, "", getString(R.string.baseURL), newValue -> {

            if (newValue == null) {
                baseUrlPref.setSummary(getString(R.string.baseURL));
            } else {
                final Dialog dialog = new Dialog(getContext());
                dialog.setContentView(R.layout.progress_dialog);

                final RequestQueue queue = Volley.newRequestQueue(getContext());
                final StringRequest request = new StringRequest(newValue,
                        response -> {
                            dialog.cancel();
                            if (response.toLowerCase().contains("snapdrop")) {
                                baseUrlPref.setSummary(newValue);
                            } else {
                                Snackbar.make(requireView(), R.string.baseurl_no_snapdrop_instance, Snackbar.LENGTH_LONG).show();
                                baseUrlPref.setSummary(getString(R.string.baseURL));
                                setPreferenceValue(baseUrlPref.getKey(), null, null);
                            }
                        }, error -> {
                    dialog.cancel();
                    Snackbar.make(requireView(), R.string.baseurl_check_instance_failed, Snackbar.LENGTH_LONG).show();
                    baseUrlPref.setSummary(getString(R.string.baseURL));
                    setPreferenceValue(baseUrlPref.getKey(), null, null);
                });

                queue.add(request);
                dialog.setOnDismissListener(d -> request.cancel());
                dialog.show();
            }
        }));
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
                .setPositiveButton(android.R.string.ok, (dialog, id) -> setPreferenceValue(pref.getKey(), editText.getText().toString().trim(), onPreferenceChangeCallback))
                .setNegativeButton(R.string.reset, (dialog, id) -> setPreferenceValue(pref.getKey(), null, onPreferenceChangeCallback));
        builder.create().show();
        return true;
    }

    @Override
    public void onSaveInstanceState(final @NonNull Bundle outState) {
        storageHelper.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }
}
