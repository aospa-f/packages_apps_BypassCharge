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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.UserHandle;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.android.bypasscharge.R;
import com.android.bypasscharge.utils.FileUtils;

/**
 * Shared state for the charge-speed sysfs control.
 *
 * <p>The OPlus charger exposes a 1..16 level in {@code cool_down}; level 8 is the stock default.
 * The value is persisted so it can be restored at boot.
 */
public final class ChargeSpeedConfig {
    private static final String TAG = "ChargeSpeed";

    public static final String KEY = "charge_speed_level";
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 16;
    public static final int DEFAULT_LEVEL = 8;

    public static final String ACTION_CHARGE_SPEED_CHANGED =
            "com.android.bypasscharge.battery.CHARGE_SPEED_CHANGED";
    public static final String EXTRA_CHARGE_SPEED_LEVEL = "charge_speed_level";

    private ChargeSpeedConfig() {
        // This class is not supposed to be instantiated
    }

    public static String getPath(Context context) {
        return context.getString(R.string.config_ChargeSpeedPath);
    }

    public static boolean isSupported(Context context) {
        return FileUtils.fileExists(getPath(context));
    }

    public static int clamp(int level) {
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
    }

    public static int getSavedLevel(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return clamp(prefs.getInt(KEY, DEFAULT_LEVEL));
    }

    /** Reads the live sysfs value, falling back to the saved value if it is unavailable. */
    public static int getCurrentLevel(Context context) {
        String value = FileUtils.readOneLine(getPath(context));
        if (value != null) {
            try {
                return clamp(Integer.parseInt(value.trim()));
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid charge speed value: " + value);
            }
        }
        return getSavedLevel(context);
    }

    /**
     * Writes a new level, persists it, and notifies the tile.
     *
     * @return true when the sysfs write succeeded
     */
    public static boolean setLevel(Context context, int level) {
        level = clamp(level);
        if (!FileUtils.writeLine(getPath(context), Integer.toString(level))) {
            Log.w(TAG, "Failed to write charge speed level " + level);
            return false;
        }

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(KEY, level)
                .apply();

        Intent intent = new Intent(ACTION_CHARGE_SPEED_CHANGED);
        intent.putExtra(EXTRA_CHARGE_SPEED_LEVEL, level);
        intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcastAsUser(intent, UserHandle.CURRENT);

        try {
            TileService.requestListeningState(
                    context, new ComponentName(context, ChargeSpeedTileService.class));
        } catch (Exception e) {
            Log.w(TAG, "Failed to request tile update", e);
        }
        return true;
    }

    /** Restores the saved level at boot without sending a tile refresh. */
    public static boolean applySavedLevel(Context context) {
        if (!isSupported(context)) {
            return false;
        }
        return FileUtils.writeLine(getPath(context), Integer.toString(getSavedLevel(context)));
    }
}
