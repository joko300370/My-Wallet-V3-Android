package piuk.blockchain.android.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import com.blockchain.ui.ActivityIndicator
import info.blockchain.wallet.payload.PayloadManagerWiper
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.utils.PersistentPrefs

class AppUtil(
    private val context: Context,
    private var payloadManager: PayloadManagerWiper,
    private var accessState: AccessState,
    private val prefs: PersistentPrefs
) {
    val isSane: Boolean
        get() {
            val guid = prefs.getValue(PersistentPrefs.KEY_WALLET_GUID, "")

            if (!guid.matches(REGEX_UUID.toRegex())) {
                return false
            }

            val encryptedPassword = prefs.getValue(PersistentPrefs.KEY_ENCRYPTED_PASSWORD, "")
            val pinID = prefs.pin

            return !(encryptedPassword.isEmpty() || pinID.isEmpty())
        }

    var sharedKey: String
        get() = prefs.getValue(PersistentPrefs.KEY_SHARED_KEY, "")
        set(sharedKey) = prefs.setValue(PersistentPrefs.KEY_SHARED_KEY, sharedKey)

    val packageManager: PackageManager
        get() = context.packageManager

    val isCameraOpen: Boolean
        get() {
            var camera: Camera? = null

            try {
                camera = Camera.open()
            } catch (e: RuntimeException) {
                return true
            } finally {
                camera?.release()
            }

            return false
        }

    var activityIndicator: ActivityIndicator? = null

    fun clearCredentials() {
        payloadManager.wipe()
        prefs.clear()
        accessState.forgetWallet()
    }

    fun clearCredentialsAndRestart(launcherActivity: Class<*>) {
        clearCredentials()
        restartApp(launcherActivity)
    }

    fun restartApp(launcherActivity: Class<*>) {
        context.startActivity(
            Intent(context, launcherActivity).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun restartAppWithVerifiedPin(launcherActivity: Class<*>, isAfterWalletCreation: Boolean = false) {
        context.startActivity(
            Intent(context, launcherActivity).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(INTENT_EXTRA_VERIFIED, true)
                putExtra(INTENT_EXTRA_IS_AFTER_WALLET_CREATION, isAfterWalletCreation)
            }
        )
        accessState.logIn()
    }

    companion object {
        private const val REGEX_UUID =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"

        const val INTENT_EXTRA_VERIFIED = "verified"
        const val INTENT_EXTRA_IS_AFTER_WALLET_CREATION = "is_after_wallet_creation"
    }
}
