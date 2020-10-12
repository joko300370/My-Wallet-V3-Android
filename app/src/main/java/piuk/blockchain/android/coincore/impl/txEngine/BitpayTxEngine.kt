package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.preferences.WalletStatus
import com.blockchain.swap.nabu.extensions.fromIso8601ToUtc
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.impl.BitPayInvoiceTarget
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.android.data.api.bitpay.BitPayDataManager
import piuk.blockchain.android.data.api.bitpay.models.BitPayTransaction
import piuk.blockchain.android.data.api.bitpay.models.BitPaymentRequest
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.lang.IllegalStateException
import java.util.Calendar
import java.util.concurrent.TimeUnit

interface EngineTransaction {
    val encodedMsg: String
    val msgSize: Int
    val txHash: String
}

interface BitPayClientEngine {
    fun doPrepareTransaction(pendingTx: PendingTx, secondPassword: String): Single<EngineTransaction>
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
                )
            }

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        assetEngine.doUpdateAmount(bitpayInvoice.amount, pendingTx)
            .flatMap { assetEngine.doBuildConfirmations(it) }
            .map { pTx ->
                pTx.addOrReplaceOption(TxOptionValue.BitPayCountdown(getCountdownTimeoutMs()), true).run {
                    if (hasOption(TxOption.FEE_SELECTION)) {
                        addOrReplaceOption(makeFeeSelectionOption(pTx))
                    } else {
                        this
                    }
                }
            }

    // BitPay invoices _always_ require priority fees, so replace the option as defined by the
    // underlying asset engine.
    private fun makeFeeSelectionOption(pendingTx: PendingTx): TxOptionValue.FeeSelection =
        TxOptionValue.FeeSelection(
            absoluteFee = pendingTx.fees,
            exchange = pendingTx.fees.toFiat(exchangeRates, userFiat),
            selectedLevel = pendingTx.feeLevel,
            availableLevels = setOf(FeeLevel.Priority)
        )

    // Don't set the amount here, it is fixed so we can do it in the confirmation building step
    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> =
        Single.just(pendingTx)

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        assetEngine.doValidateAmount(pendingTx)

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        doValidateTimeout(pendingTx)
            .flatMap { assetEngine.doValidateAll(pendingTx) }
            .updateTxValidity(pendingTx)

    private fun doValidateTimeout(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(pendingTx)
            .map { pTx ->
                val remaining = (getCountdownTimeoutMs() - System.currentTimeMillis())
                if (remaining <= 0) {
                    throw TxValidationFailure(ValidationState.INVOICE_EXPIRED)
                }
                pTx
            }

    private fun getCountdownTimeoutMs(): Long =
        bitpayInvoice.expires.fromIso8601ToUtc()?.let {
            val calendar = Calendar.getInstance()
            val timeZone = calendar.timeZone
            val offset = timeZone.getOffset(it.time).toLong()
            return it.time + offset
        } ?: throw IllegalStateException("Unknown countdown time")

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        executionClient.doPrepareTransaction(pendingTx, secondPassword)
            .flatMap { preparedTx ->
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
            }.map {
                TxResult.HashedTxResult(it, pendingTx.amount)
            }

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
