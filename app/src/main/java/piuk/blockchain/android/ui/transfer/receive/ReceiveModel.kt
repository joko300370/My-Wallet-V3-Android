package piuk.blockchain.android.ui.transfer.receive

import android.graphics.Bitmap
import com.blockchain.logging.CrashLogger
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.NullCryptoAccount
import piuk.blockchain.android.coincore.NullCryptoAddress
import piuk.blockchain.android.scan.QrCodeDataManager
import piuk.blockchain.android.ui.base.mvi.MviIntent
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import timber.log.Timber

internal data class ReceiveState(
    val account: CryptoAccount = NullCryptoAccount(),
    val address: CryptoAddress = NullCryptoAddress,
    val qrBitmap: Bitmap? = null,
    val qrDimension: Int = 0,
    val shareList: List<SendPaymentCodeData> = emptyList()
) : MviState

internal sealed class ReceiveIntent : MviIntent<ReceiveState>
internal class InitWithAccount(
    val cryptoAccount: CryptoAccount,
    private val qrSize: Int
) : ReceiveIntent() {
    override fun reduce(oldState: ReceiveState): ReceiveState {
        return oldState.copy(
            account = cryptoAccount,
            qrDimension = qrSize,
            address = NullCryptoAddress,
            qrBitmap = null,
            shareList = emptyList()
        )
    }
}

internal class UpdateAddress(private val address: CryptoAddress) : ReceiveIntent() {
    override fun reduce(oldState: ReceiveState): ReceiveState =
        oldState.copy(
            address = address,
            shareList = emptyList()
        )
}

internal object GenerateQrCode : ReceiveIntent() {
    override fun reduce(oldState: ReceiveState): ReceiveState = oldState
}

internal class UpdateQrCode(private val bmp: Bitmap) : ReceiveIntent() {
    override fun reduce(oldState: ReceiveState): ReceiveState =
        oldState.copy(
            qrBitmap = bmp,
            shareList = emptyList()
        )
}

internal object ShowShare : ReceiveIntent() {
    override fun isValidFor(oldState: ReceiveState): Boolean = oldState.qrBitmap != null

    override fun reduce(oldState: ReceiveState): ReceiveState = oldState
}

internal class UpdateShareList(val list: List<SendPaymentCodeData>) : ReceiveIntent() {
    override fun reduce(oldState: ReceiveState): ReceiveState =
        oldState.copy(shareList = list)
}

internal object AddressError : ReceiveIntent() {
    override fun reduce(oldState: ReceiveState): ReceiveState = oldState
}

internal class ReceiveModel(
    private val qrCodeDataManager: QrCodeDataManager,
    private val receiveIntentHelper: ReceiveIntentHelper,
    initialState: ReceiveState,
    observeScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<ReceiveState, ReceiveIntent>(
    initialState,
    observeScheduler,
    environmentConfig,
    crashLogger
) {

    override fun performAction(previousState: ReceiveState, intent: ReceiveIntent): Disposable? =
        when (intent) {
            is InitWithAccount -> handleInit(intent.cryptoAccount)
            is GenerateQrCode -> handleGenerateQrCode(previousState.address, previousState.qrDimension)
            is ShowShare -> handleGetShareList(previousState)
            is UpdateShareList,
            is UpdateAddress,
            is UpdateQrCode,
            is AddressError -> null
        }

    private fun handleInit(account: CryptoAccount): Disposable =
        account.receiveAddress
            .map { it as CryptoAddress }
            .subscribeBy(
                onSuccess = {
                    process(UpdateAddress(it))
                    process(GenerateQrCode)
                },
                onError = {
                    Timber.e("Unable to fetch ${account.asset} address from account")
                    process(AddressError)
                }
            )

    private fun handleGenerateQrCode(address: CryptoAddress, qrDimension: Int) =
        qrCodeDataManager.generateQrCode(address.toUrl(), qrDimension)
            .subscribeBy(
                onSuccess = { process(UpdateQrCode(it)) },
                onError = { process(AddressError) }
            )

    private fun handleGetShareList(state: ReceiveState) =
        state.qrBitmap?.let {
            receiveIntentHelper.getIntentDataList(state.address.toUrl(), it, state.account.asset)
                .subscribeBy(
                    onSuccess = { process(UpdateShareList(it)) },
                    onError = { process(AddressError) }
                )
        }
}
