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

package com.android.bypasscharge.display;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Keeps the OPlus ADFR minimum refresh rate in sync with the user's refresh-rate mode.
 *
 * <p>The OPlus display driver only enters its MIPI idle mode when the ADFR minimum FPS is below
 * the panel's idle-off threshold. The stock display HAL resets the node to the current display
 * mode's rate (60/90/120), so an adaptive mode never reaches 1 Hz. This service rewrites the node
 * after every display or setting change. The node is chowned to the system user by
 * init.oplus.ltpo.rc.
 */
public class LtpoService extends Service {
    private static final String TAG = "LtpoService";
    private static final String MIN_FPS_PATH = "/sys/kernel/oplus_display/min_fps";
    private static final int ADAPTIVE_IDLE_FPS = 1;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private DisplayManager mDisplayManager;
    private ContentResolver mResolver;
    private boolean mRegistered;

    private final DisplayManager.DisplayListener mDisplayListener =
            new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {}

                @Override
                public void onDisplayRemoved(int displayId) {}

                @Override
                public void onDisplayChanged(int displayId) {
                    if (displayId == Display.DEFAULT_DISPLAY) {
                        mHandler.postDelayed(LtpoService.this::apply, 150);
                        mHandler.postDelayed(LtpoService.this::apply, 700);
                    }
                }
            };

    private final ContentObserver mSettingsObserver =
            new ContentObserver(mHandler) {
                @Override
                public void onChange(boolean selfChange) {
                    mHandler.postDelayed(LtpoService.this::apply, 150);
                }
            };

    private final BroadcastReceiver mScreenReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    mHandler.postDelayed(LtpoService.this::apply, 300);
                    mHandler.postDelayed(LtpoService.this::apply, 900);
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();
        mResolver = getContentResolver();
        mDisplayManager = getSystemService(DisplayManager.class);
        if (mDisplayManager != null) {
            mDisplayManager.registerDisplayListener(mDisplayListener, mHandler);
        }
        mResolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.MIN_REFRESH_RATE),
                false,
                mSettingsObserver);
        mResolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.PEAK_REFRESH_RATE),
                false,
                mSettingsObserver);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(mScreenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        mRegistered = true;

        apply();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        apply();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mDisplayManager != null) {
            mDisplayManager.unregisterDisplayListener(mDisplayListener);
        }
        if (mRegistered) {
            mResolver.unregisterContentObserver(mSettingsObserver);
            unregisterReceiver(mScreenReceiver);
            mRegistered = false;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void apply() {
        final float min =
                Settings.System.getFloat(
                        mResolver, Settings.System.MIN_REFRESH_RATE, 0f);
        final int target = min > 0 ? Math.round(min) : ADAPTIVE_IDLE_FPS;

        final File node = new File(MIN_FPS_PATH);
        if (!node.exists()) {
            return;
        }

        final Integer current = readValue(node);
        if (current != null && current == target) {
            return;
        }

        try (FileWriter writer = new FileWriter(node)) {
            writer.write(Integer.toString(target));
        } catch (IOException e) {
            Log.w(TAG, "Unable to write " + MIN_FPS_PATH + "=" + target, e);
        }
    }

    private static Integer readValue(File node) {
        try (BufferedReader reader = new BufferedReader(new FileReader(node), 32)) {
            return Integer.parseInt(reader.readLine().trim());
        } catch (IOException | NumberFormatException e) {
            return null;
        }
    }
}
