package piuk.blockchain.android.coincore

import com.blockchain.swap.nabu.datamanagers.OrderState
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.helperfunctions.JavaHashCode
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import kotlin.math.sign

abstract class ActivitySummaryItem : Comparable<ActivitySummaryItem> {
    protected abstract val exchangeRates: ExchangeRateDataManager

    abstract val cryptoCurrency: CryptoCurrency
    abstract val txId: String
    abstract val timeStampMs: Long

    abstract val cryptoValue: CryptoValue

    fun fiatValue(selectedFiat: String): FiatValue =
        cryptoValue.toFiat(exchangeRates, selectedFiat)

    fun totalFiatWhenExecuted(selectedFiat: String): Single<FiatValue> =
        exchangeRates.getHistoricPrice(
            value = cryptoValue,
            fiat = selectedFiat,
            timeInSeconds = timeStampMs / 1000 // API uses seconds
        )

    override operator fun compareTo(
        other: ActivitySummaryItem
    ) = (other.timeStampMs - timeStampMs).sign

    abstract val account: CryptoAccount
}

data class CustodialActivitySummaryItem(
    override val exchangeRates: ExchangeRateDataManager,
    override val cryptoCurrency: CryptoCurrency,
    override val txId: String,
    override val timeStampMs: Long,
    override val cryptoValue: CryptoValue,
    override val account: CryptoAccount,
    val fundedFiat: FiatValue,
    val status: OrderState,
    val fee: FiatValue,
    val paymentMethodId: String
) : ActivitySummaryItem()

abstract class NonCustodialActivitySummaryItem : ActivitySummaryItem() {

    abstract val direction: TransactionSummary.Direction
    abstract val fee: Observable<CryptoValue>

    abstract val inputsMap: Map<String, CryptoValue>

    abstract val outputsMap: Map<String, CryptoValue>

    abstract val description: String?

    open val confirmations = 0
    open val watchOnly: Boolean = false
    open val doubleSpend: Boolean = false
    open val isFeeTransaction = false
    open val isPending: Boolean = false
    open var note: String? = null

    override fun toString(): String = "cryptoCurrency = $cryptoCurrency" +
        "direction  = $direction " +
        "timeStamp  = $timeStampMs " +
        "total  = ${cryptoValue.toStringWithSymbol()} " +
        "txId (hash)  = $txId " +
        "inputsMap  = $inputsMap " +
        "outputsMap  = $outputsMap " +
        "confirmations  = $confirmations " +
        "watchOnly  = $watchOnly " +
        "doubleSpend  = $doubleSpend " +
        "isPending  = $isPending " +
        "note = $note"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as NonCustodialActivitySummaryItem?

        return this.cryptoCurrency == that?.cryptoCurrency &&
            this.direction == that.direction &&
            this.timeStampMs == that.timeStampMs &&
            this.cryptoValue == that.cryptoValue &&
            this.txId == that.txId &&
            this.inputsMap == that.inputsMap &&
            this.outputsMap == that.outputsMap &&
            this.confirmations == that.confirmations &&
            this.watchOnly == that.watchOnly &&
            this.doubleSpend == that.doubleSpend &&
            this.isFeeTransaction == that.isFeeTransaction &&
            this.isPending == that.isPending &&
            this.note == that.note
    }

    override fun hashCode(): Int {
        var result = 17
        result = 31 * result + cryptoCurrency.hashCode()
        result = 31 * result + direction.hashCode()
        result = 31 * result + JavaHashCode.hashCode(timeStampMs)
        result = 31 * result + cryptoValue.hashCode()
        result = 31 * result + txId.hashCode()
        result = 31 * result + inputsMap.hashCode()
        result = 31 * result + outputsMap.hashCode()
        result = 31 * result + JavaHashCode.hashCode(confirmations)
        result = 31 * result + JavaHashCode.hashCode(isFeeTransaction)
        result = 31 * result + JavaHashCode.hashCode(watchOnly)
        result = 31 * result + JavaHashCode.hashCode(doubleSpend)
        result = 31 * result + (note?.hashCode() ?: 0)
        return result
    }

    open fun updateDescription(description: String): Completable =
        Completable.error(IllegalStateException("Update description not supported"))

    val isConfirmed: Boolean by unsafeLazy {
        confirmations >= cryptoCurrency.requiredConfirmations
    }
}

typealias ActivitySummaryList = List<ActivitySummaryItem>
