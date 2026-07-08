/*
 * Copyright (C) 2020 YAAP
 * Copyright (C) 2023-2024 cyberknight777
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.bypasscharge.battery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.android.bypasscharge.R;
import com.android.bypasscharge.utils.FileUtils;

public class BypassChargeFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceChangeListener {

    private SwitchPreferenceCompat mBypassChargePreference;
    private BypassChargeConfig mConfig;
    private boolean mInternalBypassChargeStart = false;

    private final BroadcastReceiver mServiceStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(mConfig.ACTION_BYPASS_CHARGE_SERVICE_CHANGED)) {
                if (mInternalBypassChargeStart) {
                        mInternalBypassChargeStart = false;
                        return;
                }

                if (mBypassChargePreference == null) return;

                final boolean bypasschargeStarted = intent.getBooleanExtra(
                            mConfig.EXTRA_BYPASS_CHARGE_STATE, false);

                mBypassChargePreference.setChecked(bypasschargeStarted);

            }
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.bypasscharge_settings, rootKey);
        mConfig = BypassChargeConfig.getInstance(getContext());
        mBypassChargePreference = (SwitchPreferenceCompat) findPreference(mConfig.BYPASSCHARGE_KEY);
        if (FileUtils.fileExists(mConfig.getBypassChargePath())) {
            mBypassChargePreference.setEnabled(true);
            mBypassChargePreference.setOnPreferenceChangeListener(this);
        } else {
            mBypassChargePreference.setSummary(R.string.bypass_charging_summary_not_supported);
            mBypassChargePreference.setEnabled(false);
        }

        mBypassChargePreference.setChecked(mConfig.isCurrentlyEnabled(mConfig.getBypassChargePath()));

        // Registering observers
        IntentFilter filter = new IntentFilter();
        filter.addAction(mConfig.ACTION_BYPASS_CHARGE_SERVICE_CHANGED);
        getContext().registerReceiver(mServiceStateReceiver, filter, Context.RECEIVER_EXPORTED);
    }

    @Override
    public void onResume() {
        super.onResume();
        mBypassChargePreference.setChecked(mConfig.isCurrentlyEnabled(mConfig.getBypassChargePath()));
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mConfig.BYPASSCHARGE_KEY.equals(preference.getKey())) {
            mInternalBypassChargeStart = true;
            Context mContext = getContext();

            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

            FileUtils.writeLine(mConfig.getBypassChargePath(), (Boolean) newValue ? "0":"1");

            boolean enabled = mConfig.isCurrentlyEnabled(mConfig.getBypassChargePath());

            sharedPrefs.edit().putBoolean(mConfig.BYPASSCHARGE_KEY, enabled).commit();

            Intent intent = new Intent(mConfig.ACTION_BYPASS_CHARGE_SERVICE_CHANGED);

            intent.putExtra(mConfig.EXTRA_BYPASS_CHARGE_STATE, enabled);
            intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
            mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);;
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(mServiceStateReceiver);
    }

}
