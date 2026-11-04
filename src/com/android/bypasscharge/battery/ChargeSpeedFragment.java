/*
 * Copyright (C) 2026 The AOSPA Project
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

import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;

import com.android.bypasscharge.R;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

public class ChargeSpeedFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private SeekBarPreference mChargeSpeedPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.charge_speed_settings, rootKey);

        mChargeSpeedPreference = findPreference(ChargeSpeedConfig.KEY);
        if (mChargeSpeedPreference == null) {
            return;
        }

        if (!ChargeSpeedConfig.isSupported(getContext())) {
            mChargeSpeedPreference.setEnabled(false);
            mChargeSpeedPreference.setSummary(R.string.charge_speed_summary_not_supported);
            return;
        }

        mChargeSpeedPreference.setValue(ChargeSpeedConfig.getCurrentLevel(getContext()));
        mChargeSpeedPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!ChargeSpeedConfig.KEY.equals(preference.getKey())) {
            return true;
        }

        int level = ChargeSpeedConfig.clamp((Integer) newValue);
        if (!ChargeSpeedConfig.setLevel(getContext(), level)) {
            Toast.makeText(
                            getContext(),
                            R.string.charge_speed_failed,
                            Toast.LENGTH_SHORT)
                    .show();
            return false;
        }
        return true;
    }
}
