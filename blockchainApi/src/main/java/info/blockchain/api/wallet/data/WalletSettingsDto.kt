package info.blockchain.api.wallet.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class WalletSettingsDto internal constructor(
    @SerialName("btc_currency")
    val btcCurrency: String = "",
    @SerialName("notifications_type")
    val notificationsType: List<Int> = emptyList(),
    @SerialName("language")
    val language: String = "",
    @SerialName("notifications_on")
    val notificationsOn: Int = 0,
    @SerialName("ip_lock_on")
    val ipLockOn: Int = 0,
    @SerialName("dial_code")
    val dialCode: String = "",
    @SerialName("block_tor_ips")
    val blockTorIps: Int = 0,
    @SerialName("currency")
    val currency: String = "",
    @SerialName("notifications_confirmations")
    val notificationsConfirmations: Int = 0,
    @SerialName("auto_email_backup")
    val autoEmailBackup: Int = 0,
    @SerialName("never_save_auth_type")
    val neverSaveAuthType: Int = 0,
    @SerialName("email")
    val email: String = "",
    @SerialName("sms_number")
    val smsNumber: String? = null,
    @SerialName("sms_verified")
    val smsVerified: Int = 0,
    @SerialName("is_api_access_enabled")
    val isApiAccessEnabled: Int = 0,
    @SerialName("auth_type")
    val authType: Int = 0,
    @SerialName("my_ip")
    val myIp: String = "",
    @SerialName("email_verified")
    val emailVerified: Int = 0,
    @SerialName("password_hint1")
    val passwordHint1: String = "",
    @SerialName("country_code")
    val countryCode: String = "",
    @SerialName("state")
    private val state: String = "",
    @SerialName("logging_level")
    private val loggingLevel: Int = 0,
    @SerialName("guid")
    val guid: String = "",
    @SerialName("invited")
    val invited: Map<String, Boolean> = emptyMap()
)
