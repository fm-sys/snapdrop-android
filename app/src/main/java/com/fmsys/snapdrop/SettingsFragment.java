package com.fmsys.snapdrop;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.anggrayudi.storage.SimpleStorageHelper;
import com.anggrayudi.storage.file.DocumentFileUtils;
import com.fmsys.snapdrop.utils.ClipboardUtils;
import com.fmsys.snapdrop.utils.Link;
import com.fmsys.snapdrop.utils.LogUtils;
import com.fmsys.snapdrop.utils.ShareUtils;
import com.fmsys.snapdrop.utils.ViewUtils;
import com.google.android.material.snackbar.Snackbar;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.util.SpecialButton;

import java.util.concurrent.Executors;


public class SettingsFragment extends PreferenceFragmentCompat {
    private final SimpleStorageHelper storageHelper = new SimpleStorageHelper(this);
    private SharedPreferences prefs;

    private final ActivityResultLauncher<String> storagePpermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
        final SwitchPreferenceCompat retainLocationMetadataPref = findPreference(getString(R.string.pref_retain_location_metadata));
        retainLocationMetadataPref.setChecked(result);
        if (result) {
            retainLocationMetadataPref.setEnabled(false);
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_MEDIA_LOCATION)) {
            Snackbar.make(requireView(), R.string.permission_not_granted, Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(requireView(), R.string.permission_not_granted_fallback, Snackbar.LENGTH_LONG)
                    .setAction(R.string.open_settings, v ->
                            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.fromParts("package", requireContext().getPackageName(), null))))
                    .show();
        }
    });

    private final ActivityResultLauncher<String> notificationsPpermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
        final SwitchPreferenceCompat notificationsPref = findPreference(getString(R.string.pref_notifications));
        if (result) {
            notificationsPref.setChecked(true);
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.POST_NOTIFICATIONS)) {
            Snackbar.make(requireView(), R.string.permission_not_granted, Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(requireView(), R.string.permission_not_granted_fallback, Snackbar.LENGTH_LONG)
                    .setAction(R.string.open_settings, v ->
                            startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName())))
                    .show();
        }
    });

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
                    .withAboutSpecial2("Twitter/X")
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
                                ShareUtils.openUrl(SettingsFragment.this, "https://twitter.com/intent/tweet?text=@SnapdropAndroid%20-%20%22Snapdrop%20%26%20PairDrop%20for%20Android%22%20is%20an%20Android%20client%20for%20https://snapdrop.net%20and%20https://pairdrop.net%0A%0A%23snapdrop");
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

        final Preference notificationsPref = findPreference(getString(R.string.pref_notifications));
        notificationsPref.setOnPreferenceChangeListener((pref, newValue) -> {
            if ((boolean) newValue) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    if (isNotificationsCorrectlyEnabled()) {
                        return true;
                    } else {
                        final Intent settingsIntent = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                                new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, getContext().getPackageName()) :
                                new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        .setData(Uri.fromParts("package", getContext().getPackageName(), null));

                        startActivity(settingsIntent);
                        return true;
                    }
                } else {
                    notificationsPpermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    return false;
                }
            }
            return true;
        });


        final Preference deviceNamePref = findPreference(getString(R.string.pref_device_name));
        deviceNamePref.setOnPreferenceClickListener(pref -> showEditTextPreferenceWithResetPossibility(pref, "Android ", "", null, newValue -> updateDeviceNameSummary(deviceNamePref)));
        updateDeviceNameSummary(deviceNamePref);

        final Preference baseUrlPref = findPreference(getString(R.string.pref_baseurl));
        baseUrlPref.setOnPreferenceClickListener(pref -> {
            startActivity(OnboardingActivity.getServerSelectionIntent(requireActivity()));
            return true;
        });

        final Preference saveLocationPref = findPreference(getString(R.string.pref_save_location));
        saveLocationPref.setOnPreferenceClickListener(preference -> {
            storageHelper.openFolderPicker();
            return true;
        });
        final String downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        saveLocationPref.setSummary(prefs.getString(saveLocationPref.getKey(), downloadsFolder));
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

        final SwitchPreferenceCompat locationMetadataPref = findPreference(getString(R.string.pref_retain_location_metadata));
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            locationMetadataPref.setVisible(true);
            final boolean granted = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED;
            locationMetadataPref.setChecked(granted);
            if (!granted) {
                locationMetadataPref.setOnPreferenceChangeListener((pref, newValue) -> {
                    if ((Boolean) newValue) {
                        storagePpermissionLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION);
                    }
                    return false;
                });
            } else {
                locationMetadataPref.setEnabled(false);
            }
        }
    }

    private boolean isNotificationsCorrectlyEnabled() {
        final NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        final boolean enabled = (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || notificationManager.areNotificationsEnabled()) &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || notificationManager.getNotificationChannel("MYCHANNEL") == null || notificationManager.getNotificationChannel("MYCHANNEL").getImportance() != NotificationManager.IMPORTANCE_NONE);
        return enabled;
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

    @Override
    public void onResume() {
        super.onResume();

        final boolean enabled = isNotificationsCorrectlyEnabled();
        final SwitchPreferenceCompat notificationsPref = findPreference(getString(R.string.pref_notifications));

        if (!enabled && notificationsPref.isChecked()) {
            notificationsPref.setChecked(false);
        }

        final Preference baseUrlPref = findPreference(getString(R.string.pref_baseurl));
        baseUrlPref.setSummary(prefs.getString(baseUrlPref.getKey(), getString(R.string.baseurl_not_set)));
    }
}
