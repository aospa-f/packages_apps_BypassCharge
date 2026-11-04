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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.android.bypasscharge.R;

public class ChargeSpeedTileService extends TileService {

    private final BroadcastReceiver mStateReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (ChargeSpeedConfig.ACTION_CHARGE_SPEED_CHANGED.equals(
                            intent.getAction())) {
                        updateUI();
                    }
                }
            };

    private void updateUI() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }

        if (!ChargeSpeedConfig.isSupported(this)) {
            tile.setState(Tile.STATE_UNAVAILABLE);
            tile.setSubtitle(getString(R.string.charge_speed_unavailable));
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setSubtitle(
                    getString(
                            R.string.charge_speed_level,
                            ChargeSpeedConfig.getCurrentLevel(this)));
        }
        tile.updateTile();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateUI();
        registerReceiver(mStateReceiver, new IntentFilter(ChargeSpeedConfig.ACTION_CHARGE_SPEED_CHANGED),
                Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onStopListening() {
        unregisterReceiver(mStateReceiver);
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();

        Intent intent = new Intent(this, ChargeSpeedActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(pendingIntent);
        } else {
            startActivityAndCollapse(intent);
        }
    }
}
