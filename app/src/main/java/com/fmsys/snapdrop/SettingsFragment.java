package com.fmsys.snapdrop;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;

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
import com.fmsys.snapdrop.utils.Link;
import com.fmsys.snapdrop.utils.LogUtils;
import com.fmsys.snapdrop.utils.ShareUtils;
import com.fmsys.snapdrop.utils.ViewUtils;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.util.SpecialButton;

import java.util.concurrent.Executors;


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

        initUrlPreference(R.string.pref_support, "https://github.com/fm-sys/snapdrop-android/blob/master/FUNDING.md");

        final Preference openSourceComponents = findPreference(getString(R.string.pref_about));
        openSourceComponents.setOnPreferenceClickListener(pref -> {
            new LibsBuilder()
                    .withAboutAppName(getString(R.string.app_name_long))
                    .withAboutIconShown(true)
                    .withAboutVersionShownName(true)
                    .withAboutDescription("<big><b>Credits</b></big><br><br>" +
                            "This app and its launcher icon is based on the snapdrop.net project by RobinLinus<br>" +
                            "<a href=\"https://github.com/RobinLinus/snapdrop\">github.com/RobinLinus/snapdrop</a><br><br>" +
                            "<big><b>" + getString(R.string.support_us) + "</b></big><br><br>" +
                            getString(R.string.support_us_summary) + "<br>" +
                            "<a href=\"https://github.com/fm-sys/snapdrop-android/blob/master/FUNDING.md\">" + getString(R.string.read_more) + "</a>")
                    .withAboutSpecial1("GitHub")
                    .withAboutSpecial2("Twitter")
                    .withAboutSpecial3("Crowdin")
                    .withListener(new AboutLibrariesListener() {
                        @Override
                        public boolean onIconLongClicked(final @NonNull View view) {
                            final Dialog dialog = new Dialog(view.getContext());
                            dialog.setContentView(R.layout.progress_dialog);
                            dialog.show();

                            Executors.newSingleThreadExecutor().submit(() -> {
                                final View dialogView = SettingsFragment.this.getLayoutInflater().inflate(R.layout.debug_logs_dialog, null);
                                final TextView textView = dialogView.findViewById(R.id.textview);
                                textView.setText(LogUtils.getLogs(prefs, true));
                                dialog.dismiss();

                                view.post(() -> new AlertDialog.Builder(view.getContext())
                                        .setIcon(R.drawable.pref_debug)
                                        .setTitle(R.string.logs)
                                        .setView(dialogView)
                                        .setPositiveButton(android.R.string.ok, null)
                                        .setNeutralButton(R.string.copy, (d, id) -> ClipboardUtils.copy(view.getContext(), LogUtils.getLogs(prefs, false)))
                                        .show());
                            });

                            return true;
                        }

                        @Override
                        public void onIconClicked(final @NonNull View view) {
                            ShareUtils.openUrl(SettingsFragment.this, "https://github.com/fm-sys/snapdrop-android");
                        }

                        @Override
                        public boolean onExtraClicked(final @NonNull View view, final @NonNull SpecialButton specialButton) {
                            if (specialButton == SpecialButton.SPECIAL1) {
                                ShareUtils.openUrl(SettingsFragment.this, "https://github.com/fm-sys/snapdrop-android");
                            } else if (specialButton == SpecialButton.SPECIAL2) {
                                ShareUtils.openUrl(SettingsFragment.this, "https://twitter.com/intent/tweet?text=@SnapdropAndroid%20-%20%22Snapdrop%20for%20Android%22%20is%20an%20Android%20client%20for%20%23snapdrop%0A%0Ahttps://snapdrop.net");
                            } else if (specialButton == SpecialButton.SPECIAL3) {
                                ShareUtils.openUrl(SettingsFragment.this, "https://crowdin.com/project/snapdrop-android");
                            }
                            return true;
                        }
                    })
                    .start(requireContext());
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
        deviceNamePref.setOnPreferenceClickListener(pref -> showEditTextPreferenceWithResetPossibility(pref, "Android ", "", null, newValue -> updateDeviceNameSummary(deviceNamePref)));
        updateDeviceNameSummary(deviceNamePref);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        final Preference baseUrlPref = findPreference(getString(R.string.pref_baseurl));
        baseUrlPref.setOnPreferenceClickListener(pref -> {
            OnboardingActivity.launchServerSelection(requireActivity());
            return true;
        });
        baseUrlPref.setSummary(preferences.getString(baseUrlPref.getKey(), getString(R.string.baseurl_not_set)));

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

    private Preference initUrlPreference(final @StringRes int pref, final String url) {
        final Preference preference = findPreference(getString(pref));
        if (preference != null) {
            preference.setOnPreferenceClickListener(p -> {
                ShareUtils.openUrl(this, url);
                return true;
            });
        }
        return preference;
    }

    private boolean showEditTextPreferenceWithResetPossibility(final Preference pref, final String prefix, final @NonNull String defaultValue, final Link link, final Consumer<String> onPreferenceChangeCallback) {
        ViewUtils.showEditTextWithResetPossibility(this, pref.getTitle(), prefix, PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(pref.getKey(), defaultValue), link, newValue -> setPreferenceValue(pref.getKey(), newValue, onPreferenceChangeCallback));
        return true;
    }

    @Override
    public void onSaveInstanceState(final @NonNull Bundle outState) {
        storageHelper.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }
}
