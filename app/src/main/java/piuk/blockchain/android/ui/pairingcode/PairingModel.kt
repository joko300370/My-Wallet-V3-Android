package piuk.blockchain.android.ui.pairingcode

import android.graphics.Bitmap
import com.blockchain.logging.CrashLogger
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import okhttp3.ResponseBody
import piuk.blockchain.android.scan.QrCodeDataManager
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcoreui.utils.logging.Logging
import piuk.blockchain.androidcoreui.utils.logging.PairingMethod
import piuk.blockchain.androidcoreui.utils.logging.pairingEvent

class PairingModel(
    initialState: PairingState,
    mainScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger,
    private val qrCodeDataManager: QrCodeDataManager,
    private val payloadDataManager: PayloadDataManager,
    private val authDataManager: AuthDataManager
) : MviModel<PairingState, PairingIntents>(initialState, mainScheduler, environmentConfig, crashLogger) {

    override fun performAction(previousState: PairingState, intent: PairingIntents): Disposable? {
        return when (intent) {
            is PairingIntents.LoadQrImage -> loadQrCode()
            is PairingIntents.ShowQrImage -> showQrCode(previousState.imageStatus)
            is PairingIntents.ShowQrError,
            is PairingIntents.CompleteQrImageLoading,
            is PairingIntents.HideQrImage -> null
        }
    }

    private fun showQrCode(qrCodeImageStatus: QrCodeImageStatus): Disposable? {
        if (qrCodeImageStatus !is QrCodeImageStatus.Ready &&
            qrCodeImageStatus !is QrCodeImageStatus.Hidden) {
            process(PairingIntents.LoadQrImage)
        }
        return null
    }

    private fun loadQrCode(): Disposable =
        pairingEncryptionPasswordObservable
            .flatMap { encryptionPassword -> generatePairingCodeObservable(encryptionPassword.string()) }
            .subscribe(
                { bitmap ->
                    process(PairingIntents.CompleteQrImageLoading(bitmap))
                    Logging.logEvent(pairingEvent(PairingMethod.REVERSE))
                },
                { process(PairingIntents.ShowQrError) })

    private val pairingEncryptionPasswordObservable: Single<ResponseBody>
        get() = payloadDataManager.wallet?.let { wallet ->
            Single.fromObservable(authDataManager.getPairingEncryptionPassword(wallet.guid))
        } ?: Single.error(IllegalStateException("Wallet cannot be null"))

    private fun generatePairingCodeObservable(encryptionPhrase: String): Single<Bitmap> {
        return payloadDataManager.wallet?.let { wallet ->
            qrCodeDataManager.generatePairingCode(
                wallet.guid,
                payloadDataManager.tempPassword,
                wallet.sharedKey,
                encryptionPhrase,
                600
            )
        } ?: Single.error(IllegalStateException("Wallet cannot be null"))
    }
}