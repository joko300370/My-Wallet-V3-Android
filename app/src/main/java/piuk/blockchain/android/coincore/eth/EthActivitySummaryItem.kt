package piuk.blockchain.android.coincore.eth

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.ethereum.data.EthTransaction
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Completable
import io.reactivex.Observable
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

internal class EthActivitySummaryItem(
    private val ethDataManager: EthDataManager,
    val ethTransaction: EthTransaction,
    override val isFeeTransaction: Boolean,
    private val blockHeight: Long,
    override val exchangeRates: ExchangeRateDataManager,
    override val account: EthCryptoWalletAccount
) : NonCustodialActivitySummaryItem() {

    override val cryptoCurrency: CryptoCurrency = CryptoCurrency.ETHER

    override val direction: TransactionSummary.Direction by unsafeLazy {
        val ethAddress = account.address.toLowerCase()
        when {
            ethAddress == ethTransaction.to && ethAddress == ethTransaction.from ->
                TransactionSummary.Direction.TRANSFERRED
            ethAddress == ethTransaction.from ->
                TransactionSummary.Direction.SENT
            else ->
                TransactionSummary.Direction.RECEIVED
        }
    }

    override val timeStampMs: Long = ethTransaction.timestamp * 1000

    override val cryptoValue: CryptoValue by unsafeLazy {
        CryptoValue.fromMinor(CryptoCurrency.ETHER,
            when (direction) {
                TransactionSummary.Direction.RECEIVED -> ethTransaction.value
                else -> ethTransaction.value.plus(ethTransaction.gasUsed.multiply(ethTransaction.gasPrice))
            }
        )
    }

    override val description: String?
        get() = ethDataManager.getTransactionNotes(txId)

    override val fee: Observable<CryptoValue>
        get() = Observable.just(
            CryptoValue.fromMinor(
                CryptoCurrency.ETHER,
                ethTransaction.gasUsed.multiply(ethTransaction.gasPrice)
            )
        )

    override val txId: String
        get() = ethTransaction.hash

    override val inputsMap: Map<String, CryptoValue>
        get() = mapOf(ethTransaction.from to CryptoValue.fromMinor(CryptoCurrency.ETHER, ethTransaction.value))

    override val outputsMap: Map<String, CryptoValue>
        get() = mapOf(ethTransaction.to to CryptoValue.fromMinor(CryptoCurrency.ETHER, ethTransaction.value))

    override val confirmations: Int
        get() {
            val blockNumber = ethTransaction.blockNumber ?: return 0
            val blockHash = ethTransaction.blockHash ?: return 0

            return if (blockNumber == 0L || blockHash == "0x") 0 else (blockHeight - blockNumber).toInt()
        }

    override fun updateDescription(description: String): Completable =
        ethDataManager.updateTransactionNotes(txId, description)
}
