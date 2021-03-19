package piuk.blockchain.android.ui.pairingcode

import android.graphics.Bitmap
import androidx.annotation.StringRes
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
import okhttp3.ResponseBody
import piuk.blockchain.android.R
import piuk.blockchain.android.scan.QrCodeDataManager
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.utils.logging.Logging
import piuk.blockchain.androidcoreui.utils.logging.pairingEvent
import piuk.blockchain.androidcoreui.utils.logging.PairingMethod

interface PairingCodeView : View {
    fun onQrLoaded(bitmap: Bitmap)
    fun showError(@StringRes message: Int)
    fun showProgressSpinner()
    fun hideProgressSpinner()
}

class PairingCodePresenter(
    private val qrCodeDataManager: QrCodeDataManager,
    private val payloadDataManager: PayloadDataManager,
    private val authDataManager: AuthDataManager
) : BasePresenter<PairingCodeView>() {

    override fun onViewReady() {
        // No op
    }

    internal fun generatePairingQr() {
        compositeDisposable += pairingEncryptionPasswordObservable
            .doOnSubscribe { view.showProgressSpinner() }
            .doAfterTerminate { view.hideProgressSpinner() }
            .flatMap { encryptionPassword -> generatePairingCodeObservable(encryptionPassword.string()) }
            .subscribe(
                { bitmap ->
                    view.onQrLoaded(bitmap)
                    Logging.logEvent(pairingEvent(PairingMethod.REVERSE))
                },
                { view.showError(R.string.unexpected_error) })
    }

    private val pairingEncryptionPasswordObservable: Observable<ResponseBody>
        get() {
            val guid = payloadDataManager.wallet!!.guid
            return authDataManager.getPairingEncryptionPassword(guid)
        }

    private fun generatePairingCodeObservable(encryptionPhrase: String): Observable<Bitmap> {
        val guid = payloadDataManager.wallet!!.guid
        val sharedKey = payloadDataManager.wallet!!.sharedKey
        val password = payloadDataManager.tempPassword

        return qrCodeDataManager.generatePairingCode(
            guid,
            password,
            sharedKey,
            encryptionPhrase,
            600
        )
    }
}
