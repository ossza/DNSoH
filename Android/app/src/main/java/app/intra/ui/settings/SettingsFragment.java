/*
Copyright 2018 Jigsaw Operations LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package app.intra.ui.settings;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.MainThread;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import app.intra.R;
import app.intra.sys.firebase.LogWrapper;
import app.intra.sys.PersistentState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SettingsFragment extends PreferenceFragmentCompat {
  private static final String APPS_KEY = "apps";

  private AppEnumerationTask enumerator = null;
  private ArrayList<AppInfo> appList = null;
  private MultiSelectListPreference appPref = null;

  private static class AppEnumerationTask extends AsyncTask<PackageManager, Void,
      ArrayList<AppInfo>> {
    @Override
    protected ArrayList<AppInfo> doInBackground(PackageManager... pms) {
      PackageManager pm = pms[0];
      List<ApplicationInfo> infos = pm.getInstalledApplications(0);
      ArrayList<AppInfo> entries = new ArrayList<>(infos.size());
      for (ApplicationInfo info : infos) {
        if ("app.intra".equals(info.packageName)) continue;
        if (pm.getLaunchIntentForPackage(info.packageName) == null) continue;
        entries.add(new AppInfo(info.loadLabel(pm).toString(), info.packageName));
      }
      Collections.sort(entries);
      return entries;
    }
  }

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    ArrayList<AppInfo> appList = getAppList();
    if (appList != null) {
      savedInstanceState.putParcelableArrayList(APPS_KEY, appList);
    }
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    if (savedInstanceState != null) {
      appList = savedInstanceState.getParcelableArrayList(APPS_KEY);
    }
    addPreferencesFromResource(R.xml.preferences);
    appPref = (MultiSelectListPreference)findPreference(PersistentState.APPS_KEY);
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      appPref.setEnabled(false);
      appPref.setSummary(R.string.old_android);
    }
    Preference parental = findPreference("parental_setup");
    if (parental != null) {
      parental.setOnPreferenceClickListener(pref -> {
        showParentalSetup();
        return true;
      });
    }
  }

  private void showParentalSetup() {
    EditText pinInput = new EditText(getContext());
    pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
    pinInput.setHint("4-digit PIN (leave empty to clear)");
    String existing = PersistentState.getParentalPin(getContext());
    if (existing != null) pinInput.setText(existing);

    new AlertDialog.Builder(getContext())
        .setTitle("Control setup")
        .setMessage("1. Set a PIN below so the service cannot be turned off without it.\n\n" +
            "2. Leave Secure DNS ON. It restarts after reboot if the phone allows autostart.\n\n" +
            "3. On Huawei: Settings → Battery → Launch Manager. Search for Secure DNS, turn the switch off, then enable Auto-launch, Secondary launch and Run in background.\n\n" +
            "4. Settings → More Connections → VPN. Make sure Secure DNS is not switched off.\n\n" +
            "Some phones (Huawei, Xiaomi, Samsung) also need autostart allowed in the phone's app settings.")
        .setView(pinInput)
        .setPositiveButton("Save PIN", (d, w) -> {
          PersistentState.setParentalPin(getContext(), pinInput.getText().toString().trim());
          Toast.makeText(getContext(), "PIN saved", Toast.LENGTH_SHORT).show();
        })
        .setNeutralButton("Ignore battery", (d, w) -> {
          Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
          intent.setData(Uri.parse("package:" + getContext().getPackageName()));
          startActivity(intent);
        })
        .setNegativeButton("Close", null)
        .show();
  }

  @Override
  public void onDisplayPreferenceDialog(Preference preference) {
    if (preference instanceof ServerChooser) {
      DialogFragment dialogFragment = ServerChooserFragment.newInstance(preference.getKey());
      dialogFragment.setTargetFragment(this, 0);
      dialogFragment.show(getFragmentManager(), null);
    } else {
      final ArrayList<AppInfo> appList = getAppList();
      if (appList != null) {
        String[] labels = new String[appList.size()];
        String[] packageNames = new String[appList.size()];
        for (int i = 0; i < appList.size(); ++i) {
          AppInfo entry = appList.get(i);
          labels[i] = entry.label;
          packageNames[i] = entry.packageName;
        }
        appPref.setEntries(labels);
        appPref.setEntryValues(packageNames);
      }
      super.onDisplayPreferenceDialog(preference);
    }
  }

  @MainThread
  public void collectInstalledApps(PackageManager pm) {
    enumerator = new AppEnumerationTask();
    enumerator.execute(pm);
  }

  @MainThread
  private ArrayList<AppInfo> getAppList() {
    if (appList != null) return appList;
    if (enumerator != null) {
      try {
        appList = enumerator.get();
      } catch (InterruptedException e) {
      } catch (ExecutionException e) {
        LogWrapper.logException(e);
      }
    }
    return appList;
  }
}
