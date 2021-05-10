package piuk.blockchain.android.ui.login

import io.reactivex.Completable
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.auth.AuthService
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs

class LoginInteractor(
    private val authService: AuthService,
    private val payloadDataManager: PayloadDataManager,
    private val prefs: PersistentPrefs,
    private val appUtil: AppUtil
) {

    fun loginWithQrCode(qrString: String): Completable {

        return payloadDataManager.handleQrCode(qrString)
            .doOnComplete {
                payloadDataManager.wallet?.let { wallet ->
                    prefs.sharedKey = wallet.sharedKey
                    prefs.walletGuid = wallet.guid
                    prefs.setValue(PersistentPrefs.KEY_EMAIL_VERIFIED, true)
                    prefs.setValue(PersistentPrefs.KEY_ONBOARDING_COMPLETE, true)
                }
            }
            .doOnError { appUtil.clearCredentials() }
    }

    fun sendEmailForVerification(email: String): Completable {
        return authService.sendEmailForDeviceVerification(email)
            .ignoreElement()
    }
}