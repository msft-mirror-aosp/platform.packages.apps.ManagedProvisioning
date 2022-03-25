/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.managedprovisioning.provisioning;

import static java.util.Objects.requireNonNull;

import android.content.Context;

import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.SettingsFacade;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.task.AbstractProvisioningTask;
import com.android.managedprovisioning.task.AddWifiNetworkTask;
import com.android.managedprovisioning.task.ConnectMobileNetworkTask;
import com.android.managedprovisioning.task.DownloadPackageTask;
import com.android.managedprovisioning.task.InstallPackageTask;
import com.android.managedprovisioning.task.VerifyPackageTask;

/**
 * Controller which establishes network connection and downloads the device policy management
 * role holder.
 */
public class DownloadRoleHolderController extends AbstractProvisioningController {

    private final Utils mUtils;
    private final SettingsFacade mSettingsFacade;

    /**
     * Instantiates a new {@link DownloadRoleHolderController} instance and creates the
     * relevant tasks.
     *
     * @return the newly created instance
     */
    public static DownloadRoleHolderController createInstance(
            Context context,
            ProvisioningParams params,
            int userId,
            ProvisioningControllerCallback callback,
            Utils utils,
            SettingsFacade settingsFacade) {
        DownloadRoleHolderController controller =
                new DownloadRoleHolderController(context, params, userId, callback,
                        utils, settingsFacade);
        controller.setUpTasks();
        return controller;
    }

    private DownloadRoleHolderController(Context context,
            ProvisioningParams params, int userId,
            ProvisioningControllerCallback callback,
            Utils utils,
            SettingsFacade settingsFacade) {
        super(context, params, userId, callback);
        mUtils = requireNonNull(utils);
        mSettingsFacade = requireNonNull(settingsFacade);
    }

    @Override
    protected void setUpTasks() {
        if (mParams.wifiInfo != null) {
            addTasks(new AddWifiNetworkTask(mContext, mParams, this));
        } else if (mParams.useMobileData) {
            addTasks(new ConnectMobileNetworkTask(mContext, mParams, this));
        }

        addDownloadAndInstallRoleHolderPackageTasks();
    }

    private void addDownloadAndInstallRoleHolderPackageTasks() {
        if (mParams.roleHolderDownloadInfo == null) {
            return;
        }

        DownloadPackageTask downloadTask = new DownloadPackageTask(
                mContext, mParams, mParams.roleHolderDownloadInfo, this);
        addTasks(downloadTask,
                new VerifyPackageTask(
                        downloadTask, mContext, mParams, mParams.roleHolderDownloadInfo, this),
                new InstallPackageTask(downloadTask, mContext, mParams, this));
    }

    @Override
    protected int getErrorTitle() {
        return R.string.cant_set_up_device;
    }

    @Override
    protected int getErrorMsgId(AbstractProvisioningTask task, int errorCode) {
        // TODO(b/220175163): update strings for the DMRH case
        if (task instanceof AddWifiNetworkTask) {
            return R.string.error_wifi;
        } else if (task instanceof DownloadPackageTask) {
            switch (errorCode) {
                case DownloadPackageTask.ERROR_DOWNLOAD_FAILED:
                    return R.string.error_download_failed;
                case DownloadPackageTask.ERROR_OTHER:
                    return R.string.cant_set_up_device;
            }
        } else if (task instanceof VerifyPackageTask) {
            switch (errorCode) {
                case VerifyPackageTask.ERROR_HASH_MISMATCH:
                    return R.string.error_hash_mismatch;
                case VerifyPackageTask.ERROR_DEVICE_ADMIN_MISSING:
                    return R.string.error_package_invalid;
            }
        } else if (task instanceof InstallPackageTask) {
            switch (errorCode) {
                case InstallPackageTask.ERROR_PACKAGE_INVALID:
                    return R.string.error_package_invalid;
                case InstallPackageTask.ERROR_INSTALLATION_FAILED:
                    return R.string.error_installation_failed;
            }
        }

        return R.string.cant_set_up_device;
    }

    @Override
    protected boolean getRequireFactoryReset(AbstractProvisioningTask task, int errorCode) {
        return !mSettingsFacade.isDeviceProvisioned(mContext)
                && mUtils.isOrganizationOwnedAllowed(mParams)
                && !(task instanceof AddWifiNetworkTask);
    }
}
