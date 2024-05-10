/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.managedprovisioning.ota;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.android.managedprovisioning.common.ProvisionLogger;

/**
 * This receiver is invoked after a system update.
 */
public class PreBootListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ProvisionLogger.logd("Received PreBoot broadcast " + intent.getAction());
        if (Intent.ACTION_PRE_BOOT_COMPLETED.equals(intent.getAction())) {
            final PendingResult result = goAsync();
            Thread thread = new Thread(() -> {
                new OtaController(context).run();
                result.finish();
            });
            thread.setPriority(Thread.MAX_PRIORITY);
            ProvisionLogger.logd("PreBoot completed, starting OTA controller job");
            thread.start();
        } else {
            ProvisionLogger.logw("Unexpected intent action: " + intent.getAction());
        }
    }
}
