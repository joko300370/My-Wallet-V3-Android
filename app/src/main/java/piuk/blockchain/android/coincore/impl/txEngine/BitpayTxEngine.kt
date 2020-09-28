package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.preferences.WalletStatus
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.impl.BitPayInvoiceTarget
import piuk.blockchain.android.data.api.bitpay.BitPayDataManager
import piuk.blockchain.android.data.api.bitpay.models.BitPayTransaction
import piuk.blockchain.android.data.api.bitpay.models.BitPaymentRequest
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.util.concurrent.TimeUnit

interface EngineTransaction {
    val encodedMsg: String
    val msgSize: Int
    val txHash: String
}

interface BitPayClientEngine {
    fun doPrepareTransaction(PendingTx: PendingTx, secondPassword: String): Single<EngineTransaction>
    fun doOnTransactionSuccess(pendingTx: PendingTx)
    fun doOnTransactionFailed(pendingTx: PendingTx, e: Throwable)
}

class BtcBitpayTxEngine(
    private val bitPayDataManager: BitPayDataManager,
    private val assetEngine: OnChainTxEngineBase,
    private val walletPrefs: WalletStatus
) : TxEngine() {

    override fun assertInputsValid() {
        // Only support BTC bitpay at this time
        require(asset == CryptoCurrency.BTC)
        require(txTarget is BitPayInvoiceTarget)
        require(assetEngine is BitPayClientEngine)
    }

    private val executionClient: BitPayClientEngine by unsafeLazy {
        assetEngine as BitPayClientEngine
    }

    private val bitpayInvoice: BitPayInvoiceTarget by unsafeLazy {
        txTarget as BitPayInvoiceTarget
    }

    override fun start(
        sourceAccount: CryptoAccount,
        txTarget: TransactionTarget,
        exchangeRates: ExchangeRateDataManager
    ) {
        super.start(sourceAccount, txTarget, exchangeRates)
        assetEngine.start(sourceAccount, txTarget, exchangeRates)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        assetEngine.doInitialiseTx()
            .map { tx ->
                tx.copy(
                    amount = bitpayInvoice.amount,
                    feeLevel = FeeLevel.Priority
//                    engineState = BitPayEngineState(
                )
            }
//                view.showBitPayTimerAndMerchantInfo(expires, merchant)
//                view.updateReceivingAddress("bitcoin:?r=" + it.paymentUrl)
//            }

    override val feeOptions: Set<FeeLevel>
        get() = setOf(FeeLevel.Priority)

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(pendingTx)

    // Bitpay invoices have a fixed amount, so attempting to update the amount is an error
    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        require(amount.isZero)
        return assetEngine.doUpdateAmount(bitpayInvoice.amount, pendingTx)
    }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        assetEngine.doValidateAmount(pendingTx)

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(pendingTx)

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Completable =
        executionClient.doPrepareTransaction(pendingTx, secondPassword)
            .map { preparedTx ->
                doExecuteTransaction(bitpayInvoice.invoiceId, preparedTx)
            }.doOnSuccess {
                walletPrefs.setBitPaySuccess()
//                analytics.logEvent(BitPayEvent.SuccessEvent(pendingTx.amount, CryptoCurrency.BTC))
                executionClient.doOnTransactionSuccess(pendingTx)
            }.doOnError { e ->
                executionClient.doOnTransactionFailed(pendingTx, e)
//                (it as? BitPayApiException)?.let { bitpayException ->
//                    analytics.logEvent(BitPayEvent.FailureEvent(bitpayException.message ?: ""))
//                }
            }.ignoreElement()

    private fun doExecuteTransaction(
        invoiceId: String,
        tx: EngineTransaction
    ): Single<String> =
        bitPayDataManager.paymentVerificationRequest(
            invoiceId,
            BitPaymentRequest(
                CryptoCurrency.BTC.networkTicker,
                listOf(
                    BitPayTransaction(
                        tx.encodedMsg,
                        tx.msgSize
                    )
                )
            )
        ).flatMap {
            Single.timer(3, TimeUnit.SECONDS)
        }.flatMap {
            bitPayDataManager.paymentSubmitRequest(
                invoiceId,
                BitPaymentRequest(
                    CryptoCurrency.BTC.networkTicker,
                    listOf(
                        BitPayTransaction(
                            tx.encodedMsg,
                            tx.msgSize
                        )
                    )
                )
            )
        }.map {
            tx.txHash
        }
}

// if (address.isEmpty() && scanData.isBitpayAddress()) {
//    // get payment protocol request data from bitpay
//    val invoiceId = paymentRequestUrl.replace(bitpayInvoiceUrl, "")
//    if (isDeepLinked) {
//        analytics.logEvent(BitPayEvent.InputEvent(AnalyticsEvents.BitpayUrlDeeplink.event, CryptoCurrency.BTC))
//    } else {
//        analytics.logEvent(BitPayEvent.InputEvent(AnalyticsEvents.BitpayAdrressScanned.event,CryptoCurrency.BTC))
//    }
//    handleBitPayInvoice(invoiceId)
//
// private fun handleBitPayInvoice(invoiceId: String) {
//    compositeDisposable += bitpayDataManager.getRawPaymentRequest(invoiceId = invoiceId)
//        .doOnSuccess {
//            val cryptoValue = CryptoValue(selectedCrypto, it.instructions[0].outputs[0].amount)
//            val merchant = it.memo.split(merchantPattern)[1]
//            val bitpayProtocol: BitPayProtocol? = delegate as? BitPayProtocol ?: return@doOnSuccess
//
//            bitpayProtocol?.setBitpayReceivingAddress(it.instructions[0].outputs[0].address)
//            bitpayProtocol?.setBitpayMerchant(merchant)
//            bitpayProtocol?.setInvoiceId(invoiceId)
//            bitpayProtocol?.setIsBitpayPaymentRequest(true)
//            view?.let { view ->
//                view.disableInput()
//                view.showBitPayTimerAndMerchantInfo(it.expires, merchant)
//                view.updateCryptoAmount(cryptoValue)
//                view.updateReceivingAddress("bitcoin:?r=" + it.paymentUrl)
//                view.setFeePrioritySelection(1)
//                view.disableFeeDropdown()
//                view.onBitPayAddressScanned()
//            }
//        }.doOnError {
//            Timber.e(it)
//        }.subscribeBy(
//            onError = {
//                view?.finishPage()
//            }
//        )
// }
