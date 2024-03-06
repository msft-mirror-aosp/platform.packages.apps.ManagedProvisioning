package com.android.managedprovisioning.contracts

import android.content.Context
import android.content.Intent
import com.android.managedprovisioning.ManagedProvisioningScreens.DOWNLOAD_ROLE_HOLDER
import com.android.managedprovisioning.ScreenManager
import com.android.managedprovisioning.annotations.LegacyApi
import com.android.managedprovisioning.contracts.Checksum.SignatureChecksum
import com.android.managedprovisioning.model.PackageDownloadInfo
import com.android.managedprovisioning.model.ProvisioningParams
import com.android.onboarding.common.MANAGED_PROVISIONING
import com.android.onboarding.contracts.IntentScope
import com.android.onboarding.contracts.ScopedIntentSerializer
import com.android.onboarding.contracts.VoidOnboardingActivityApiContract
import com.android.onboarding.contracts.annotations.OnboardingNode
import com.android.onboarding.contracts.provisioning.EXTRAS
import com.android.onboarding.contracts.require
import com.android.onboarding.contracts.setupwizard.SuwArguments
import com.android.onboarding.contracts.setupwizard.SuwArgumentsSerializer
import com.android.onboarding.contracts.setupwizard.WithSuwArguments
import javax.inject.Inject

/**
 * @property location Url where the package (.apk) can be downloaded from. {@code null} if there is no download location specified.
 * @property cookieHeader Cookie header for http request.
 * @property checksum SHA-256 hash of the signature in the .apk file
 * @property minVersion Minimum supported version code of the downloaded package.
 */
data class DownloadRoleHolderArguments(
    override val suwArguments: SuwArguments,
    override val provisioningParams: ProvisioningParams,
    val location: String,
    val checksum: SignatureChecksum,
    val cookieHeader: String? = null,
    val minVersion: Int = Int.MAX_VALUE,
) : WithProvisioningParams, WithSuwArguments {
    @LegacyApi
    constructor(
        suwArguments: SuwArguments,
        provisioningParams: ProvisioningParams,
    ) : this(
        suwArguments = suwArguments,
        provisioningParams = provisioningParams,
        location = provisioningParams.roleHolderDownloadInfo?.location
                ?.require("Download location must not be empty.", String::isNotBlank)
            ?: error("Missing download location."),
        checksum = provisioningParams.roleHolderDownloadInfo.signatureChecksum
                .require("Checksum is missing", ByteArray::isNotEmpty)
                .let(Checksum::SignatureChecksum),
        cookieHeader = provisioningParams.roleHolderDownloadInfo.cookieHeader,
        minVersion = provisioningParams.roleHolderDownloadInfo.minVersion
    )
}

@OnboardingNode(
    component = MANAGED_PROVISIONING,
    name = "DownloadRoleHolder",
    uiType = OnboardingNode.UiType.LOADING)
class DownloadRoleHolderContract
@Inject
constructor(
    private val screenManager: ScreenManager,
    val suwArgumentsSerializer: SuwArgumentsSerializer
) : VoidOnboardingActivityApiContract<DownloadRoleHolderArguments>(),
    ScopedIntentSerializer<DownloadRoleHolderArguments> {

    override fun performCreateIntent(context: Context, arg: DownloadRoleHolderArguments): Intent =
        Intent(context, screenManager.getActivityClassForScreen(DOWNLOAD_ROLE_HOLDER))
                .also { write(it, arg) }

    override fun performExtractArgument(intent: Intent): DownloadRoleHolderArguments = read(intent)
    override fun IntentScope.write(value: DownloadRoleHolderArguments) {
        write(suwArgumentsSerializer, value.suwArguments)
        this[EXTRAS.EXTRA_PROVISIONING_PARAMS] =
            value.provisioningParams.toBuilder()
                    .setRoleHolderDownloadInfo(
                        PackageDownloadInfo.Builder()
                                .setLocation(value.location)
                                .setCookieHeader(value.cookieHeader)
                                .setMinVersion(value.minVersion)
                                .setSignatureChecksum(value.checksum.bytes)
                                .build())
                    .build()
    }

    override fun IntentScope.read(): DownloadRoleHolderArguments {
        val provisioningParams =
            parcelableExtra<ProvisioningParams>(EXTRAS.EXTRA_PROVISIONING_PARAMS)
        val params = provisioningParams.roleHolderDownloadInfo
            ?: error("Missing role holder extras")
        return DownloadRoleHolderArguments(
            suwArguments = read(suwArgumentsSerializer),
            provisioningParams = provisioningParams,
            location = params.location,
            checksum = SignatureChecksum(params.signatureChecksum),
            cookieHeader = params.cookieHeader,
            minVersion = params.minVersion,
        )
    }
}