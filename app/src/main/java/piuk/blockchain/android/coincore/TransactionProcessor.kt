package piuk.blockchain.android.coincore

import androidx.annotation.CallSuper
import com.blockchain.extensions.replace
import com.blockchain.koin.payloadScope
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import org.koin.core.KoinComponent
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber

open class TransferError(msg: String) : Exception(msg)

enum class ValidationState {
    CAN_EXECUTE,
    UNINITIALISED,
    HAS_TX_IN_FLIGHT,
    INVALID_AMOUNT,
    INSUFFICIENT_FUNDS,
    INSUFFICIENT_GAS,
    INVALID_ADDRESS,
    ADDRESS_IS_CONTRACT,
    OPTION_INVALID,
    UNDER_MIN_LIMIT,
    OVER_MAX_LIMIT,
    INVOICE_EXPIRED,
    UNKNOWN_ERROR
}

class TxValidationFailure(val state: ValidationState) : TransferError("Invalid Send Tx: $state")

enum class FeeLevel {
    None,
    Regular,
    Priority,
    Custom
}

data class PendingTx(
    val amount: Money,
    val available: Money,
    val fees: Money,
    val selectedFiat: String,
    val feeLevel: FeeLevel = FeeLevel.Regular,
    val options: List<TxOptionValue> = emptyList(),
    val minLimit: Money? = null,
    val maxLimit: Money? = null,
    val validationState: ValidationState = ValidationState.UNINITIALISED,
    val engineState: Map<String, Any> = emptyMap()
) {
    fun hasOption(option: TxOption): Boolean =
        options.find { it.option == option } != null

    inline fun <reified T : TxOptionValue> getOption(option: TxOption): T? =
        options.find { it.option == option } as? T

    // Internal, coincore only helper methods for managing option lists. If you're using these in
    // UI are client code, you're doing something wrong!
    internal fun removeOption(option: TxOption): PendingTx =
        this.copy(
            options = options.filter { it.option != option }
        )

    internal fun addOrReplaceOption(newOption: TxOptionValue, prepend: Boolean = false): PendingTx =
        copy(
            options = if (hasOption(newOption.option)) {
                val old = options.find { it.option == newOption.option }
                options.replace(old, newOption).filterNotNull()
            } else {
                val opts = options.toMutableList()
                if (prepend) {
                    opts.add(0, newOption)
                } else {
                    opts.add(newOption)
                }
                opts.toList()
            }
        )
}

enum class TxOption {
    DESCRIPTION,
    AGREEMENT_INTEREST_T_AND_C,
    AGREEMENT_INTEREST_TRANSFER,
    READ_ONLY,
    MEMO,
    LARGE_TRANSACTION_WARNING,
    FEE_SELECTION,
    ERROR_NOTICE,
    INVOICE_COUNTDOWN
}

sealed class TxOptionValue(open val option: TxOption) {

    data class ExchangePriceOption(val money: Money, val asset: CryptoCurrency) :
        TxOptionValue(TxOption.READ_ONLY)

    data class FeedTotal(
        val amount: Money,
        val fee: Money,
        val exchangeAmount: Money? = null,
        val exchangeFee: Money? = null
    ) : TxOptionValue(TxOption.READ_ONLY)

    data class From(val from: String) : TxOptionValue(TxOption.READ_ONLY)

    data class To(val to: String) : TxOptionValue(TxOption.READ_ONLY)

    data class Total(val total: Money, val exchange: Money? = null) : TxOptionValue(TxOption.READ_ONLY)

    @Deprecated("Replace with FeeSelection")
    data class Fee(val fee: Money, val exchange: Money? = null) : TxOptionValue(TxOption.READ_ONLY)

    data class FeeSelection(
        val absoluteFee: Money? = null,
        val exchange: Money? = null,
        val selectedLevel: FeeLevel,
        val availableLevels: Set<FeeLevel> = emptySet()
    ) : TxOptionValue(TxOption.FEE_SELECTION)

    data class BitPayCountdown(
        val expireTime: Long = 0,
        val isExpired: Boolean = false
    ) : TxOptionValue(TxOption.INVOICE_COUNTDOWN)

    data class ErrorNotice(val status: ValidationState) : TxOptionValue(TxOption.ERROR_NOTICE)

    data class Description(val text: String = "") : TxOptionValue(TxOption.DESCRIPTION)

    data class Memo(val text: String?, val isRequired: Boolean, val id: Long?) : TxOptionValue(TxOption.MEMO)

    data class TxBooleanOption<T>(
        override val option: TxOption,
        val data: T? = null,
        val value: Boolean = false
    ) : TxOptionValue(option)
}

abstract class TxEngine : KoinComponent {

    private lateinit var _sourceAccount: CryptoAccount
    private lateinit var _txTarget: TransactionTarget
    private lateinit var _exchangeRates: ExchangeRateDataManager

    protected val sourceAccount: CryptoAccount
        get() = _sourceAccount

    protected val txTarget: TransactionTarget
        get() = _txTarget

    protected val exchangeRates: ExchangeRateDataManager
        get() = _exchangeRates

    @CallSuper
    open fun start(
        sourceAccount: CryptoAccount,
        txTarget: TransactionTarget,
        exchangeRates: ExchangeRateDataManager
    ) {
        this._sourceAccount = sourceAccount
        this._txTarget = txTarget
        this._exchangeRates = exchangeRates
    }

    // Optionally assert, via require() etc, that sourceAccounts and txTarget
    // are valid for this engine.
    open fun assertInputsValid() {}

    open val userFiat: String by unsafeLazy {
        payloadScope.get<CurrencyPrefs>().selectedFiatCurrency
    }

    protected val asset: CryptoCurrency
        get() = sourceAccount.asset

    open val requireSecondPassword: Boolean = false

    // Does this engine accept fiat input amounts
    open val canTransactFiat: Boolean = false

    // Return a stream of the exchange rate between the source asset and the user's selected
    // fiat currency. This should always return at least once, but can safely either complete
    // or keep sending updated rates, depending on what is useful for Transaction context
    open fun userExchangeRate(): Observable<ExchangeRate> =
        Observable.just(
            exchangeRates.getLastPrice(sourceAccount.asset, userFiat)
        ).map { rate ->
            ExchangeRate.CryptoToFiat(
                sourceAccount.asset,
                userFiat,
                rate.toBigDecimal()
            )
        }

    abstract fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx>

    // If the source and target assets are not the same this MAY return a stream of the exchange rates
    // between them. Or it may simply complete. This is not used yet in the UI, but it may be when
    // sell and or swap are fully integrated into this flow
    open fun targetExchangeRate(): Observable<ExchangeRate> =
        Observable.empty()

    // Implementation interface:
    // Call this first to initialise the processor. Construct and initialise a pendingTx object.
    abstract fun doInitialiseTx(): Single<PendingTx>

    // Update the transaction with a new amount. This method should check balances, calculate fees and
    // Return a new PendingTx with the state updated for the UI to update. The pending Tx will
    // be passed to validate after this call.
    abstract fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx>

    // Process any TxOption updates, if required. The default just replaces the option and returns
    // the updated pendingTx. Subclasses may want to, eg, update amounts on fee changes etc
    open fun doOptionUpdateRequest(pendingTx: PendingTx, newOption: TxOptionValue): Single<PendingTx> =
        Single.just(pendingTx.addOrReplaceOption(newOption))

    // Check the tx is complete, well formed and possible. If it is, set pendingTx to CAN_EXECUTE
    // Else set it to the appropriate error, and then return the updated PendingTx
    abstract fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx>

    // Check the tx is complete, well formed and possible. If it is, set pendingTx to CAN_EXECUTE
    // Else set it to the appropriate error, and then return the updated PendingTx
    abstract fun doValidateAll(pendingTx: PendingTx): Single<PendingTx>

    // Execute the transaction, it will have been validated before this is called, so the expectation
    // is that it will succeed.
    abstract fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult>

    // Action to be executed once the transaction has been executed, it will have been validated before this is called, so the expectation
    // is that it will succeed.
    open fun doPostExecute(txResult: TxResult): Completable = Completable.complete()
}

class TransactionProcessor(
    sourceAccount: CryptoAccount,
    txTarget: TransactionTarget,
    exchangeRates: ExchangeRateDataManager,
    private val engine: TxEngine
) {
    init {
        engine.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )
        engine.assertInputsValid()
    }

    val requireSecondPassword: Boolean
        get() = engine.requireSecondPassword

    val canTransactFiat: Boolean
        get() = engine.canTransactFiat

    private val txObservable: BehaviorSubject<PendingTx> = BehaviorSubject.create()

    private fun updatePendingTx(pendingTx: PendingTx) =
        txObservable.onNext(pendingTx)

    private fun getPendingTx(): PendingTx =
        txObservable.value ?: throw IllegalStateException("TransactionProcessor not initialised")

    // Initialise the tx as required.
    // This will start propagating the pendingTx to the client code.
    fun initialiseTx(): Observable<PendingTx> =
        engine.doInitialiseTx()
            .doOnSuccess {
                updatePendingTx(it)
            }.flatMapObservable {
                txObservable
            }

    // Set the option to the passed option value. If the option is not supported, it will not be
    // in the original list when the pendingTx is created. And if it is not supported, then trying to
    // update it will cause an error.
    fun setOption(newOption: TxOptionValue): Completable {

        val pendingTx = getPendingTx()
        if (!pendingTx.hasOption(newOption.option)) {
            throw IllegalArgumentException("Unsupported TxOption: ${newOption.option}")
        }

        return engine.doOptionUpdateRequest(pendingTx, newOption)
            .flatMap { pTx ->
                engine.doValidateAll(pTx)
            }.doOnSuccess { pTx ->
                updatePendingTx(pTx)
            }.ignoreElement()
    }

    fun updateAmount(amount: Money): Completable {
        Timber.d("!TRANSACTION!> in UpdateAmount")
        val pendingTx = getPendingTx()
        if (!canTransactFiat && amount is FiatValue)
            throw IllegalArgumentException("The processor does not support fiat values")

        return engine.doUpdateAmount(amount, pendingTx)
            .flatMap {
                val isFreshTx = it.validationState == ValidationState.UNINITIALISED
                engine.doValidateAmount(it)
                    .map { pendingTx ->
                        // Remove initial "insufficient funds' warning
                        if (amount.isZero && isFreshTx) {
                            pendingTx.copy(validationState = ValidationState.UNINITIALISED)
                        } else {
                            pendingTx
                        }
                    }
            }
            .doOnSuccess {
                updatePendingTx(it)
            }
            .ignoreElement()
    }

    // Return a stream of the exchange rate between the source asset and the user's selected
    // fiat currency. This should always return at least once, but can safely either complete
    // or keep sending updated rates, depending on what is useful for Transaction context
    fun userExchangeRate(): Observable<ExchangeRate> =
        engine.userExchangeRate()

    // Check the validity of a pending transactions.
    fun validateAll(): Completable {
        val pendingTx = getPendingTx()
        return engine.doBuildConfirmations(pendingTx).flatMap {
            engine.doValidateAll(it)
        }.doOnSuccess { updatePendingTx(it) }.ignoreElement()
    }

    // Execute the transaction.
    // Ideally, I'd like to return the Tx id/hash. But we get nothing back from the
    // custodial APIs (and are not likely to, since the tx is batched and not executed immediately)
    fun execute(secondPassword: String = ""): Completable {
        if (requireSecondPassword && secondPassword.isEmpty())
            throw IllegalArgumentException("Second password not supplied")

        val pendingTx = getPendingTx()
        return engine.doValidateAll(pendingTx)
            .doOnSuccess {
                if (it.validationState != ValidationState.CAN_EXECUTE)
                    throw IllegalStateException("PendingTx is not executable")
            }.flatMapCompletable {
                engine.doExecute(it, secondPassword).flatMapCompletable { result ->
                    engine.doPostExecute(result)
                }
            }
    }

    // If the source and target assets are not the same this MAY return a stream of the exchange rates
    // between them. Or it may simply complete. This is not used yet in the UI, but it may be when
    // sell and or swap are fully integrated into this flow
    fun targetExchangeRate(): Observable<ExchangeRate> =
        engine.targetExchangeRate()
}

fun Completable.updateTxValidity(pendingTx: PendingTx): Single<PendingTx> =
    this.toSingle {
        pendingTx.copy(validationState = ValidationState.CAN_EXECUTE)
    }.updateTxValidity(pendingTx)

fun Single<PendingTx>.updateTxValidity(pendingTx: PendingTx): Single<PendingTx> =
    this.onErrorReturn {
        if (it is TxValidationFailure) {
            pendingTx.copy(validationState = it.state)
        } else {
            throw it
        }
    }.map { pTx ->
        if (pTx.options.isNotEmpty())
            updateOptionsWithValidityWarning(pTx)
        else
            pTx
    }

private fun updateOptionsWithValidityWarning(pendingTx: PendingTx): PendingTx =
    if (pendingTx.validationState !in setOf(ValidationState.CAN_EXECUTE, ValidationState.UNINITIALISED)) {
        pendingTx.addOrReplaceOption(TxOptionValue.ErrorNotice(status = pendingTx.validationState), true)
    } else {
        pendingTx.removeOption(TxOption.ERROR_NOTICE)
    }

sealed class TxResult(val amount: Money) {
    class HashedTxResult(val txHash: String, amount: Money) : TxResult(amount)
    class UnHashedTxResult(amount: Money) : TxResult(amount)
}