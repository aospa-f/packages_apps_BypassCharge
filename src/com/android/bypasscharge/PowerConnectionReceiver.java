/*
 * Copyright (C) 2024 Paranoid Android
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

package com.android.bypasscharge;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.android.bypasscharge.battery.BypassChargeConfig;
import com.android.bypasscharge.battery.BypassChargeTileService;
import com.android.bypasscharge.utils.FileUtils;

public class PowerConnectionReceiver extends BroadcastReceiver {
    private static final String TAG = "BypassCharge";

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            Log.d(TAG, "Power disconnected: automatically turning off bypass charging");
            BypassChargeConfig config = BypassChargeConfig.getInstance(context);

            // Re-enable charging in sysfs
            FileUtils.writeLine(config.getBypassChargePath(), "1");

            // Reset shared preferences
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            sharedPrefs.edit().putBoolean(config.BYPASSCHARGE_KEY, false).apply();

            // Clear system setting
            try {
                Settings.System.putInt(context.getContentResolver(), "bypass_charging_active", 0);
            } catch (Exception e) {
                Log.e(TAG, "Failed to clear bypass_charging_active in Settings.System", e);
            }

            // Notify UI components
            Intent stateIntent = new Intent(config.ACTION_BYPASS_CHARGE_SERVICE_CHANGED);
            stateIntent.putExtra(config.EXTRA_BYPASS_CHARGE_STATE, false);
            stateIntent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
            context.sendBroadcastAsUser(stateIntent, UserHandle.CURRENT);

            // Request QS tile to refresh
            try {
                TileService.requestListeningState(context,
                        new ComponentName(context, BypassChargeTileService.class));
            } catch (Exception e) {
                Log.e(TAG, "Failed to request tile update", e);
            }
        }
    }
}
