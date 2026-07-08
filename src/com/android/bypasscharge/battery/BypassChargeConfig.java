/*
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

import android.content.Context;
import android.content.res.Resources;

import com.android.bypasscharge.utils.FileUtils;

public class BypassChargeConfig {

    private static BypassChargeConfig instance = null;

    public static BypassChargeConfig getInstance(Context context) {

        if (instance == null) {
            instance = new BypassChargeConfig(context.getApplicationContext());
        }

        return instance;
    }

    public static final String BYPASSCHARGE_KEY = "bypass_charging";

    private final String config_BypassChargePath;

    public static final String ACTION_BYPASS_CHARGE_SERVICE_CHANGED = "com.android.bypasscharge.battery.BYPASS_CHARGE_SERVICE_CHANGED";
    public static final String EXTRA_BYPASS_CHARGE_STATE = "bypasschargingenabled";

    private BypassChargeConfig(Context context) {

	Resources res = context.getResources();

	config_BypassChargePath = res.getString(com.android.bypasscharge.R.string.config_BypassChargePath);
    }

    public String getBypassChargePath() {
        return config_BypassChargePath;
    }

    public boolean isCurrentlyEnabled(String node) {
        return FileUtils.getNodeValueAsBoolean(node, false);
    }
 }
