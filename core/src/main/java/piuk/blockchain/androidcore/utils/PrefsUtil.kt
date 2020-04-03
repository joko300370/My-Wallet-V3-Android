package piuk.blockchain.androidcore.utils

import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.data.Settings.UNIT_FIAT
import java.lang.IllegalStateException
import java.util.Currency
import java.util.Locale

interface UUIDGenerator {
    fun generateUUID(): String
}

class PrefsUtil(
    private val store: SharedPreferences,
    private val backupStore: SharedPreferences,
    private val idGenerator: DeviceIdGenerator,
    private val uuidGenerator: UUIDGenerator
) : PersistentPrefs {

    private var isUnderAutomationTesting = false // Don't persist!

    override val isUnderTest: Boolean
        get() = isUnderAutomationTesting

    override fun setIsUnderTest() {
        isUnderAutomationTesting = true
    }

    override val deviceId: String
        get() {
            return if (qaRandomiseDeviceId) {
                uuidGenerator.generateUUID()
            } else {
                var deviceId = getValue(KEY_PRE_IDV_DEVICE_ID, "")
                if (deviceId.isEmpty()) {
                    deviceId = idGenerator.generateId()
                    setValue(KEY_PRE_IDV_DEVICE_ID, deviceId)
                }
                deviceId
            }
        }

    override var devicePreIDVCheckFailed: Boolean
        get() = getValue(KEY_PRE_IDV_FAILED, false)
        set(value) = setValue(KEY_PRE_IDV_FAILED, value)

    override var isOnboardingComplete: Boolean
        get() = getValue(PersistentPrefs.KEY_ONBOARDING_COMPLETE, false)
        set(completed) = setValue(PersistentPrefs.KEY_ONBOARDING_COMPLETE, completed)

    override var isCustodialIntroSeen: Boolean
        get() = getValue(KEY_CUSTODIAL_INTRO_SEEN, false)
        set(seen) = setValue(KEY_CUSTODIAL_INTRO_SEEN, seen)

    override val isLoggedOut: Boolean
        get() = getValue(KEY_LOGGED_OUT, true)

    override var qaRandomiseDeviceId: Boolean
        get() = getValue(KEY_IS_DEVICE_ID_RANDOMISED, false)
        set(value) = setValue(KEY_IS_DEVICE_ID_RANDOMISED, value)

    // SecurityPrefs
    override var disableRootedWarning: Boolean
        get() = getValue(PersistentPrefs.KEY_ROOT_WARNING_DISABLED, false)
        set(v) = setValue(PersistentPrefs.KEY_ROOT_WARNING_DISABLED, v)

    override var trustScreenOverlay: Boolean
        get() = getValue(PersistentPrefs.KEY_OVERLAY_TRUSTED, false)
        set(v) = setValue(PersistentPrefs.KEY_OVERLAY_TRUSTED, v)

    override val areScreenshotsEnabled: Boolean
        get() = getValue(PersistentPrefs.KEY_SCREENSHOTS_ENABLED, false)

    override fun setScreenshotsEnabled(enable: Boolean) =
        setValue(PersistentPrefs.KEY_SCREENSHOTS_ENABLED, enable)

    // From CurrencyPrefs
    override var selectedFiatCurrency: String
        get() = getValue(KEY_SELECTED_FIAT, defaultCurrency())
        set(fiat) = setValue(KEY_SELECTED_FIAT, fiat)

    private fun defaultCurrency(): String =
        // Android has been know to return non-ISO3166 locale codes. Esp on cheap Chinese phones (Meizu m5u!)
        // Fallback to the default DEFAULT_FIAT_CURRENCY in these cases
        try {
            val localeFiat = Currency.getInstance(Locale.getDefault()).currencyCode
            if (UNIT_FIAT.contains(localeFiat)) localeFiat else DEFAULT_FIAT_CURRENCY
        } catch (e: IllegalStateException) {
            DEFAULT_FIAT_CURRENCY
        }

    override var selectedCryptoCurrency: CryptoCurrency
        get() =
            try {
                CryptoCurrency.valueOf(getValue(KEY_SELECTED_CRYPTO, DEFAULT_CRYPTO_CURRENCY.name))
            } catch (e: IllegalArgumentException) {
                removeValue(KEY_SELECTED_CRYPTO)
                DEFAULT_CRYPTO_CURRENCY
            }
        set(crypto) = setValue(KEY_SELECTED_CRYPTO, crypto.name)

    // From ThePitLinkingPrefs
    override var pitToWalletLinkId: String
        get() = getValue(KEY_PIT_LINKING_LINK_ID, "")
        set(v) = setValue(KEY_PIT_LINKING_LINK_ID, v)

    override fun clearPitToWalletLinkId() {
        removeValue(KEY_PIT_LINKING_LINK_ID)
    }

    override fun simpleBuyState(): String? {
        return getValue(KEY_SIMPLE_BUY_STATE, "").takeIf { it != "" }
    }

    override fun updateSimpleBuyState(simpleBuyState: String) {
        setValue(KEY_SIMPLE_BUY_STATE, simpleBuyState)
    }

    override fun clearState() {
        removeValue(KEY_SIMPLE_BUY_STATE)
    }

    override fun setFlowStartedAtLeastOnce() {
        setValue(KEY_SIMPLE_BUY_FLOW_STARTED, true)
    }

    override fun flowStartedAtLeastOnce(): Boolean =
        getValue(KEY_SIMPLE_BUY_FLOW_STARTED, false)

    // From Onboarding
    override var swapIntroCompleted: Boolean
        get() = getValue(KEY_SWAP_INTRO_COMPLETED, false)
        set(v) = setValue(KEY_SWAP_INTRO_COMPLETED, v)

    override val isTourComplete: Boolean
        get() = getValue(KEY_INTRO_TOUR_COMPLETED, false)

    override val tourStage: String
        get() = getValue(KEY_INTRO_TOUR_CURRENT_STAGE, "")

    override fun setTourComplete() {
        setValue(KEY_INTRO_TOUR_COMPLETED, true)
        removeValue(KEY_INTRO_TOUR_CURRENT_STAGE)
    }

    override fun setTourStage(stageName: String) =
        setValue(KEY_INTRO_TOUR_CURRENT_STAGE, stageName)

    override fun resetTour() {
        removeValue(KEY_INTRO_TOUR_COMPLETED)
        removeValue(KEY_INTRO_TOUR_CURRENT_STAGE)
    }

    // Wallet Status
    override var lastBackupTime: Long
        get() = getValue(BACKUP_DATE_KEY, 0L)
        set(v) = setValue(BACKUP_DATE_KEY, v)

    override val isWalletBackedUp: Boolean
        get() = lastBackupTime != 0L

    override val isWalletFunded: Boolean
        get() = getValue(WALLET_FUNDED_KEY, false)

    override fun setWalletFunded() = setValue(WALLET_FUNDED_KEY, true)

    override var lastSwapTime: Long
        get() = getValue(SWAP_DATE_KEY, 0L)
        set(v) = setValue(SWAP_DATE_KEY, v)

    override val hasSwapped: Boolean
        get() = lastSwapTime != 0L

    override val hasMadeBitPayTransaction: Boolean
        get() = getValue(BITPAY_TRANSACTION_SUCCEEDED, false)

    override fun setBitPaySuccess() = setValue(BITPAY_TRANSACTION_SUCCEEDED, true)

    // Notification prefs
    override var arePushNotificationsEnabled: Boolean
        get() = getValue(KEY_PUSH_NOTIFICATION_ENABLED, true)
        set(v) = setValue(KEY_PUSH_NOTIFICATION_ENABLED, v)

    override var firebaseToken: String
        get() = getValue(KEY_FIREBASE_TOKEN, "")
        set(v) = setValue(KEY_FIREBASE_TOKEN, v)

    // Encrypted prefs from backup
    override var backupEncryptedPassword: String?
        get() = backupStore.getString(KEY_ENCRYPTED_PASSWORD, null)
        set(value) { backupStore.edit().putString(KEY_ENCRYPTED_PASSWORD, value).apply() }
    override var backupEncryptedSharedKey: String?
        get() = backupStore.getString(KEY_ENCRYPTED_SHARED_KEY, null)
        set(value) { backupStore.edit().putString(KEY_ENCRYPTED_SHARED_KEY, value).apply() }
    override var backupEncryptedGuid: String?
        get() = backupStore.getString(KEY_ENCRYPTED_GUID, null)
        set(value) { backupStore.edit().putString(KEY_ENCRYPTED_GUID, value).apply() }
    override var backupPinIdentifier: String?
        get() = backupStore.getString(KEY_ENCRYPTED_PIN_KEY, null)
        set(value) { backupStore.edit().putString(KEY_ENCRYPTED_PIN_KEY, value).apply() }
    override var backupEnabled: Boolean
        get() = backupStore.getBoolean(KEY_CLOUD_BACKUP_ENABLED, true)
        set(value) { backupStore.edit().putBoolean(KEY_CLOUD_BACKUP_ENABLED, value).apply() }
    override val hasBackup: Boolean
        get() = backupPinIdentifier != null && backupEncryptedPassword != null &&
                backupEncryptedGuid != null && backupEncryptedSharedKey != null && backupEnabled

    override fun clearBackup() {
        backupStore.edit().clear().apply()
    }

    // Raw accessors
    override fun getValue(name: String): String? =
        store.getString(name, null)

    override fun getValue(name: String, defaultValue: String): String =
        store.getString(name, defaultValue) ?: ""

    override fun getValue(name: String, defaultValue: Int): Int =
        store.getInt(name, defaultValue)

    override fun getValue(name: String, defaultValue: Long): Long =
        try {
            store.getLong(name, defaultValue)
        } catch (e: Exception) {
            store.getInt(name, defaultValue.toInt()).toLong()
        }

    override fun getValue(name: String, defaultValue: Boolean): Boolean =
        store.getBoolean(name, defaultValue)

    override fun setValue(name: String, value: String) {
        store.edit().putString(name, value).apply()
    }

    override fun setValue(name: String, value: Int) {
        store.edit().putInt(name, if (value < 0) 0 else value).apply()
    }

    override fun setValue(name: String, value: Long) {
        store.edit().putLong(name, if (value < 0L) 0L else value).apply()
    }

    override fun setValue(name: String, value: Boolean) {
        store.edit().putBoolean(name, value).apply()
    }

    override fun has(name: String): Boolean = store.contains(name)

    override fun removeValue(name: String) {
        store.edit().remove(name).apply()
    }

    override fun clear() {
        store.edit().clear().apply()
        // TODO should this clear the backup or not? Seems like there are quite a few entry points
        clearBackup()
    }

    /**
     * Clears everything but the GUID for logging back in and the deviceId - for pre-IDV checking
     */
    override fun logOut() {
        val guid = getValue(PersistentPrefs.KEY_WALLET_GUID, "")
        val deviceId = getValue(KEY_PRE_IDV_DEVICE_ID, "")

        clear()

        setValue(KEY_LOGGED_OUT, true)
        setValue(PersistentPrefs.KEY_WALLET_GUID, guid)
        setValue(KEY_PRE_IDV_DEVICE_ID, deviceId)
    }

    /**
     * Reset value once user logged in
     */
    override fun logIn() {
        setValue(KEY_LOGGED_OUT, false)
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DEFAULT_FIAT_CURRENCY = "USD"
        val DEFAULT_CRYPTO_CURRENCY = CryptoCurrency.BTC

        const val KEY_PRE_IDV_FAILED = "pre_idv_check_failed"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val KEY_SELECTED_FIAT = "ccurrency" // Historical misspelling, don't update
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val KEY_PRE_IDV_DEVICE_ID = "pre_idv_device_id"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val KEY_LOGGED_OUT = "logged_out"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val KEY_SELECTED_CRYPTO = "KEY_CURRENCY_CRYPTO_STATE"

        private const val KEY_PIT_LINKING_LINK_ID = "pit_wallet_link_id"
        private const val KEY_SIMPLE_BUY_STATE = "key_simple_buy_state"
        private const val KEY_SIMPLE_BUY_FLOW_STARTED = "key_simple_buy_flow"
        private const val KEY_SWAP_INTRO_COMPLETED = "key_swap_intro_completed"
        private const val KEY_INTRO_TOUR_COMPLETED = "key_intro_tour_complete"
        private const val KEY_INTRO_TOUR_CURRENT_STAGE = "key_intro_tour_current_stage"
        private const val KEY_CUSTODIAL_INTRO_SEEN = "key_custodial_balance_intro_seen"

        private const val BACKUP_DATE_KEY = "BACKUP_DATE_KEY"
        private const val SWAP_DATE_KEY = "SWAP_DATE_KEY"
        private const val WALLET_FUNDED_KEY = "WALLET_FUNDED_KEY"
        private const val BITPAY_TRANSACTION_SUCCEEDED = "BITPAY_TRANSACTION_SUCCEEDED"
        // For QA:
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val KEY_IS_DEVICE_ID_RANDOMISED = "random_device_id"

        private const val KEY_FIREBASE_TOKEN = "firebase_token"
        private const val KEY_PUSH_NOTIFICATION_ENABLED = "push_notification_enabled"

        private const val KEY_ENCRYPTED_PIN_KEY = "backup_pin_key"
        private const val KEY_ENCRYPTED_GUID = "backup_encrypted_guid"
        private const val KEY_ENCRYPTED_SHARED_KEY = "backup_encrypted_shared_key"
        private const val KEY_ENCRYPTED_PASSWORD = "backup_encrypted_password"
        private const val KEY_CLOUD_BACKUP_ENABLED = "backup_enabled"
    }
}
