/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2019 The LineageOS Project
 *               2023-2024 cyberknight777
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
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import com.android.bypasscharge.battery.BypassChargeConfig;
import com.android.bypasscharge.utils.FileUtils;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final boolean DEBUG = false;
    private static final String TAG = "BypassCharge";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (DEBUG)
            Log.d(TAG, "Received boot completed intent");

        BypassChargeConfig mConfig = BypassChargeConfig.getInstance(context);

        // Bypass charging must never be restored on boot: it conflicts with charging
        // control and can leave the battery in an unexpected state. Always start with
        // normal charging enabled and let the user turn bypass on explicitly.
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPrefs.edit().putBoolean(mConfig.BYPASSCHARGE_KEY, false).apply();
        try {
            Settings.System.putInt(context.getContentResolver(), "bypass_charging_active", 0);
        } catch (Exception ignored) {}
        FileUtils.writeLine(mConfig.getBypassChargePath(), "1");

    }
}
