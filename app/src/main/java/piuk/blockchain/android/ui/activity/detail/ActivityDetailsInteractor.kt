package piuk.blockchain.android.ui.activity.detail

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.CustodialActivitySummaryItem
import piuk.blockchain.android.coincore.FiatActivitySummaryItem
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.android.coincore.btc.BtcActivitySummaryItem
import piuk.blockchain.android.coincore.erc20.Erc20ActivitySummaryItem
import piuk.blockchain.android.coincore.eth.EthActivitySummaryItem
import piuk.blockchain.android.repositories.AssetActivityRepository
import java.text.ParseException
import java.util.Date

class ActivityDetailsInteractor(
    private val currencyPrefs: CurrencyPrefs,
    private val transactionInputOutputMapper: TransactionInOutMapper,
    private val assetActivityRepository: AssetActivityRepository,
    private val custodialWalletManager: CustodialWalletManager
) {

    fun loadCustodialItems(
        custodialActivitySummaryItem: CustodialActivitySummaryItem
    ): Single<List<ActivityDetailsType>> {
        val list = mutableListOf(
            BuyTransactionId(custodialActivitySummaryItem.txId),
            Created(Date(custodialActivitySummaryItem.timeStampMs)),
            BuyPurchaseAmount(custodialActivitySummaryItem.fundedFiat),
            BuyCryptoWallet(custodialActivitySummaryItem.cryptoCurrency),
            BuyFee(custodialActivitySummaryItem.fee)
        )

        return if (custodialActivitySummaryItem.paymentMethodId != PaymentMethod.BANK_PAYMENT_ID) {
            custodialWalletManager.getCardDetails(custodialActivitySummaryItem.paymentMethodId)
                .map { paymentMethod ->
                    addPaymentDetailsToList(list, paymentMethod, custodialActivitySummaryItem)

                    list.toList()
                }.onErrorReturn {
                    addPaymentDetailsToList(list, null, custodialActivitySummaryItem)

                    list.toList()
                }
        } else {
            list.add(BuyPaymentMethod(
                PaymentDetails(custodialActivitySummaryItem.paymentMethodId, label = null,
                    endDigits = null
                )))

            if (custodialActivitySummaryItem.status == OrderState.AWAITING_FUNDS ||
                custodialActivitySummaryItem.status == OrderState.PENDING_EXECUTION
            ) {
                list.add(CancelAction())
            }
            Single.just(list.toList())
        }
    }

    private fun addPaymentDetailsToList(
        list: MutableList<ActivityDetailsType>,
        paymentMethod: PaymentMethod.Card?,
        custodialActivitySummaryItem: CustodialActivitySummaryItem
    ) {
        paymentMethod?.let {
            list.add(BuyPaymentMethod(PaymentDetails(
                it.cardId, it.uiLabel(), it.endDigits
            )))
        } ?: list.add(BuyPaymentMethod(
            PaymentDetails(custodialActivitySummaryItem.paymentMethodId,
                label = null, endDigits = null)
        ))

        if (custodialActivitySummaryItem.status == OrderState.PENDING_CONFIRMATION) {
            list.add(CancelAction())
        }
    }

    fun getCustodialActivityDetails(
        cryptoCurrency: CryptoCurrency,
        txHash: String
    ): CustodialActivitySummaryItem? =
        assetActivityRepository.findCachedItem(cryptoCurrency, txHash) as? CustodialActivitySummaryItem

    fun getFiatActivityDetails(
        currency: String,
        txHash: String
    ): FiatActivitySummaryItem? =
        assetActivityRepository.findCachedItem(currency, txHash) as? FiatActivitySummaryItem

    fun getNonCustodialActivityDetails(
        cryptoCurrency: CryptoCurrency,
        txHash: String
    ): NonCustodialActivitySummaryItem? =
        assetActivityRepository.findCachedItem(cryptoCurrency, txHash) as? NonCustodialActivitySummaryItem

    fun loadCreationDate(
        activitySummaryItem: ActivitySummaryItem
    ): Date? = try {
        Date(activitySummaryItem.timeStampMs)
    } catch (e: ParseException) {
        null
    }

    fun loadFeeItems(
        item: NonCustodialActivitySummaryItem
    ) = item.totalFiatWhenExecuted(currencyPrefs.selectedFiatCurrency)
        .flatMap { fiatValue ->
            getTransactionsMapForFeeItems(item, fiatValue)
        }.onErrorResumeNext {
            getTransactionsMapForFeeItems(item, null)
        }

    private fun getTransactionsMapForFeeItems(
        item: NonCustodialActivitySummaryItem,
        fiatValue: Money?
    ) = transactionInputOutputMapper.transformInputAndOutputs(item).map {
        getListOfItemsForFees(item, fiatValue, it)
    }.onErrorReturn {
        getListOfItemsForFees(item, fiatValue, null)
    }

    private fun getListOfItemsForFees(
        item: NonCustodialActivitySummaryItem,
        fiatValue: Money?,
        transactionInOutDetails: TransactionInOutDetails?
    ) = listOfNotNull(
        Amount(item.value),
        Value(item.fiatValue(currencyPrefs.selectedFiatCurrency)),
        HistoricValue(fiatValue, item.direction),
        addSingleOrMultipleFromAddresses(transactionInOutDetails),
        addFeeForTransaction(item),
        checkIfShouldAddDescription(item),
        Action()
    )

    fun loadReceivedItems(
        item: NonCustodialActivitySummaryItem
    ) = item.totalFiatWhenExecuted(currencyPrefs.selectedFiatCurrency)
        .flatMap { fiatValue ->
            getTransactionsMapForReceivedItems(item, fiatValue)
        }.onErrorResumeNext {
            getTransactionsMapForReceivedItems(item, null)
        }

    private fun getTransactionsMapForReceivedItems(
        item: NonCustodialActivitySummaryItem,
        fiatValue: Money?
    ) = transactionInputOutputMapper.transformInputAndOutputs(item).map {
        getListOfItemsForReceives(item, fiatValue, it)
    }.onErrorReturn {
        getListOfItemsForReceives(item, fiatValue, null)
    }

    private fun getListOfItemsForReceives(
        item: NonCustodialActivitySummaryItem,
        fiatValue: Money?,
        transactionInOutDetails: TransactionInOutDetails?
    ) = listOfNotNull(
        Amount(item.value),
        Value(item.fiatValue(currencyPrefs.selectedFiatCurrency)),
        HistoricValue(fiatValue, item.direction),
        addSingleOrMultipleFromAddresses(transactionInOutDetails),
        addSingleOrMultipleToAddresses(transactionInOutDetails),
        checkIfShouldAddDescription(item),
        Action()
    )

    fun loadTransferItems(
        item: NonCustodialActivitySummaryItem
    ) = item.totalFiatWhenExecuted(currencyPrefs.selectedFiatCurrency)
        .flatMap { fiatValue ->
            getTransactionsMapForTransferItems(item, fiatValue)
        }.onErrorResumeNext {
            getTransactionsMapForTransferItems(item, null)
        }

    private fun getTransactionsMapForTransferItems(
        item: NonCustodialActivitySummaryItem,
        fiatValue: Money?
    ) = transactionInputOutputMapper.transformInputAndOutputs(item).map {
        getListOfItemsForTransfers(item, fiatValue, it)
    }.onErrorReturn {
        getListOfItemsForTransfers(item, fiatValue, null)
    }

    private fun getListOfItemsForTransfers(
        item: NonCustodialActivitySummaryItem,
        fiatValue: Money?,
        transactionInOutDetails: TransactionInOutDetails?
    ) = listOfNotNull(
        Amount(item.value),
        Value(item.fiatValue(currencyPrefs.selectedFiatCurrency)),
        HistoricValue(fiatValue, item.direction),
        addSingleOrMultipleFromAddresses(transactionInOutDetails),
        addSingleOrMultipleToAddresses(transactionInOutDetails),
        checkIfShouldAddDescription(item),
        Action()
    )

    fun loadConfirmedSentItems(
        item: NonCustodialActivitySummaryItem
    ) = item.fee.single(item.value as? CryptoValue).flatMap { cryptoValue ->
        getTotalFiat(item, cryptoValue, currencyPrefs.selectedFiatCurrency)
    }.onErrorResumeNext {
        getTotalFiat(item, null, currencyPrefs.selectedFiatCurrency)
    }

    private fun getTotalFiat(
        item: NonCustodialActivitySummaryItem,
        value: Money?,
        selectedFiatCurrency: String
    ) = item.totalFiatWhenExecuted(selectedFiatCurrency).flatMap { fiatValue ->
        getTransactionsMapForConfirmedSentItems(value, fiatValue, item)
    }.onErrorResumeNext {
        getTransactionsMapForConfirmedSentItems(value, null, item)
    }

    private fun getTransactionsMapForConfirmedSentItems(
        cryptoValue: Money?,
        fiatValue: Money?,
        item: NonCustodialActivitySummaryItem
    ) = transactionInputOutputMapper.transformInputAndOutputs(item)
        .map {
            getListOfItemsForConfirmedSends(cryptoValue, fiatValue, item, it)
        }.onErrorReturn {
            getListOfItemsForConfirmedSends(cryptoValue, fiatValue, item, null)
        }

    private fun getListOfItemsForConfirmedSends(
        cryptoValue: Money?,
        fiatValue: Money?,
        item: NonCustodialActivitySummaryItem,
        transactionInOutDetails: TransactionInOutDetails?
    ) = listOfNotNull(
        Amount(item.value),
        Fee(cryptoValue),
        Value(item.fiatValue(currencyPrefs.selectedFiatCurrency)),
        HistoricValue(fiatValue, item.direction),
        addSingleOrMultipleFromAddresses(transactionInOutDetails),
        addSingleOrMultipleToAddresses(transactionInOutDetails),
        checkIfShouldAddDescription(item),
        Action()
    )

    fun loadUnconfirmedSentItems(
        item: NonCustodialActivitySummaryItem
    ) = item.fee.singleOrError().flatMap { cryptoValue ->
        getTransactionsMapForUnconfirmedSentItems(item, cryptoValue)
    }.onErrorResumeNext {
        getTransactionsMapForUnconfirmedSentItems(item, null)
    }

    private fun getTransactionsMapForUnconfirmedSentItems(
        item: NonCustodialActivitySummaryItem,
        cryptoValue: CryptoValue?
    ) = transactionInputOutputMapper.transformInputAndOutputs(item).map {
        getListOfItemsForUnconfirmedSends(item, cryptoValue, it)
    }.onErrorReturn {
        getListOfItemsForUnconfirmedSends(item, cryptoValue, null)
    }

    private fun getListOfItemsForUnconfirmedSends(
        item: NonCustodialActivitySummaryItem,
        cryptoValue: CryptoValue?,
        transactionInOutDetails: TransactionInOutDetails?
    ) = listOfNotNull(
        Amount(item.value),
        Fee(cryptoValue),
        addSingleOrMultipleFromAddresses(transactionInOutDetails),
        addSingleOrMultipleToAddresses(transactionInOutDetails),
        checkIfShouldAddDescription(item),
        Action()
    )

    fun updateItemDescription(
        txId: String,
        cryptoCurrency: CryptoCurrency,
        description: String
    ): Completable {
        return when (val activityItem = assetActivityRepository.findCachedItem(cryptoCurrency, txId)) {
            is BtcActivitySummaryItem -> activityItem.updateDescription(description)
            is EthActivitySummaryItem -> activityItem.updateDescription(description)
            is Erc20ActivitySummaryItem -> activityItem.updateDescription(description)
            else -> {
                Completable.error(UnsupportedOperationException(
                    "This type of currency doesn't support descriptions"))
            }
        }
    }

    private fun addFeeForTransaction(item: NonCustodialActivitySummaryItem): FeeForTransaction? {
        return when (item) {
            is EthActivitySummaryItem -> {
                val relatedItem = assetActivityRepository.findCachedItemById(item.ethTransaction.hash)
                relatedItem?.let {
                    FeeForTransaction(
                        item.direction,
                        it.value
                    )
                }
            }
            else -> null
        }
    }

    private fun addSingleOrMultipleFromAddresses(
        it: TransactionInOutDetails?
    ) = when {
        it == null -> {
            From(null)
        }
        it.inputs.size == 1 -> {
            From(it.inputs[0].address)
        }
        else -> {
            From(it.inputs.joinToString("\n"))
        }
    }

    private fun addSingleOrMultipleToAddresses(
        it: TransactionInOutDetails?
    ) = when {
        it == null -> {
            To(null)
        }
        it.outputs.size == 1 -> {
            To(it.outputs[0].address)
        }
        else -> {
            To(it.outputs.joinToString("\n"))
        }
    }

    private fun checkIfShouldAddDescription(
        item: NonCustodialActivitySummaryItem
    ): Description? = when (item) {
        is BtcActivitySummaryItem,
        is EthActivitySummaryItem,
        is Erc20ActivitySummaryItem -> Description(item.description)
        else -> null
    }
}