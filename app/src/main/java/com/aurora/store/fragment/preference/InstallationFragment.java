package com.aurora.store.fragment.preference;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.activity.SettingsActivity;
import com.aurora.store.utility.ContextUtil;
import com.aurora.store.utility.PackageUtil;
import com.aurora.store.utility.PathUtil;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.utility.Root;
import com.aurora.store.utility.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.scottyab.rootbeer.RootBeer;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InstallationFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String ROOT = "1";
    private static final String SERVICES = "2";

    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(Constants.SHARED_PREFERENCES_KEY);
        setPreferencesFromResource(R.xml.preferences_installation, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SharedPreferences mPrefs = Util.getPrefs(context);
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        ListPreference listInstallMethod = findPreference(Constants.PREFERENCE_INSTALLATION_METHOD);
        assert listInstallMethod != null;
        listInstallMethod.setOnPreferenceChangeListener((preference, newValue) -> {
            String installMethod = (String) newValue;
            if (installMethod.equals(ROOT)) {
                RootBeer rootBeer = new RootBeer(context);
                if (rootBeer.isRooted()) {
                    PrefUtil.putString(context, Constants.PREFERENCE_INSTALLATION_METHOD, installMethod);
                    Root.requestRoot();
                    showDownloadDialog();
                    return true;
                } else {
                    showNoRootDialog();
                    return false;
                }
            } else if (installMethod.equals(SERVICES)) {
                if (PrefUtil.getBoolean(context, Constants.PREFERENCE_DOWNLOAD_INTERNAL)) {
                    showInternalDialog();
                    return false;
                }

                if (PackageUtil.isInstalled(context, Constants.SERVICE_PACKAGE)) {
                    PrefUtil.putString(context, Constants.PREFERENCE_INSTALLATION_METHOD, installMethod);
                    PrefUtil.putString(context, Constants.PREFERENCE_DOWNLOAD_DIRECTORY, PathUtil.getExtBaseDirectory(context));
                    return true;
                } else {
                    showNoServicesDialog();
                    return false;
                }
            } else {
                PrefUtil.putString(context, Constants.PREFERENCE_INSTALLATION_METHOD, installMethod);
                return true;
            }
        });

        Preference servicePreference = findPreference(Constants.PREFERENCE_LAUNCH_SERVICES);
        assert servicePreference != null;
        if (PackageUtil.isInstalled(context, Constants.SERVICE_PACKAGE)) {
            servicePreference.setEnabled(true);
            servicePreference.setOnPreferenceClickListener(preference -> {
                Intent intent = context.getPackageManager().getLaunchIntentForPackage(Constants.SERVICE_PACKAGE);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                context.startActivity(intent);
                return false;
            });
        } else {
            servicePreference.setEnabled(false);
            servicePreference.setSummary(getString(R.string.pref_services_desc_alt));
        }

        Preference abandonPreference = findPreference(Constants.PREFERENCE_INSTALLATION_ABANDON_SESSION);
        assert abandonPreference != null;
        abandonPreference.setOnPreferenceClickListener(preference -> {
            Util.clearOldInstallationSessions(context);
            ContextUtil.toastLong(context, getString(R.string.toast_abandon_sessions));
            return false;
        });

        ListPreference userProfilePreference = findPreference(Constants.PREFERENCE_INSTALLATION_PROFILE);
        assert userProfilePreference != null;
        try {
            addUserInfoData(userProfilePreference);
        } catch (Exception e) {
            userProfilePreference.setEnabled(false);
        }
        userProfilePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            String installMethod = (String) newValue;
            PrefUtil.putString(context, Constants.PREFERENCE_INSTALLATION_PROFILE, installMethod);
            return true;
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Constants.PREFERENCE_INSTALLATION_TYPE:
            case Constants.PREFERENCE_INSTALLATION_METHOD:
                SettingsActivity.shouldRestart = true;
                break;
        }
    }

    private void addUserInfoData(ListPreference listPreference) {
        Root root = new Root();
        if (!root.isAcquired()) {
            listPreference.setEnabled(false);
            return;
        }

        List<String> entryList = new ArrayList<>();
        List<String> entryValueList = new ArrayList<>();

        String result = root.exec("pm list users");
        Scanner scanner = new Scanner(result);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Pattern p = Pattern.compile("\\{(.*):");
            Matcher m = p.matcher(line);
            while (m.find()) {
                String rawUser = m.group(1);
                String[] rawUserArray = rawUser.split(":");
                entryValueList.add(rawUserArray[0]);
                entryList.add(rawUserArray[1]);
            }
        }

        CharSequence[] entries = new CharSequence[entryList.size()];
        CharSequence[] entryValues = new CharSequence[entryValueList.size()];
        for (int i = 0; i < entryList.size(); i++) {
            entries[i] = entryList.get(i);
            entryValues[i] = entryValueList.get(i);
        }

        listPreference.setEntries(entries);
        listPreference.setEntryValues(entryValues);
    }

    private void showInternalDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.pref_app_download);
        builder.setMessage(R.string.pref_install_mode_internal_warn);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        builder.create();
        builder.show();
    }

    private void showDownloadDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.pref_app_download);
        builder.setMessage(R.string.pref_install_mode_root_warn);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        builder.create();
        builder.show();
    }

    private void showNoRootDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.action_installations);
        builder.setMessage(R.string.pref_install_mode_no_root);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        builder.create();
        builder.show();
    }

    private void showNoServicesDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.action_installations);
        builder.setMessage(R.string.pref_install_mode_no_services);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        builder.create();
        builder.show();
    }
}