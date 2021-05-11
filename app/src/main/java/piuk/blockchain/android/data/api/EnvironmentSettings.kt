package piuk.blockchain.android.data.api

import info.blockchain.wallet.api.Environment
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.data.api.bitpay.BITPAY_LIVE_BASE
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class EnvironmentSettings : EnvironmentConfig {

    override fun isRunningInDebugMode(): Boolean = BuildConfig.DEBUG

    override val environment: Environment = Environment.fromString(BuildConfig.ENVIRONMENT)

    override val apiUrl: String = BuildConfig.API_URL
    override val everypayHostUrl: String = BuildConfig.EVERYPAY_HOST_URL
    override val statusUrl: String = BuildConfig.STATUS_API_URL

    override val bitpayUrl: String = BITPAY_LIVE_BASE
}
