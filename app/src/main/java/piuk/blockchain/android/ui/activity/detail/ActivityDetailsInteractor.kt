package piuk.blockchain.android.ui.activity.detail

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.OrderType
import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CustodialInterestActivitySummaryItem
import piuk.blockchain.android.coincore.CustodialTradingActivitySummaryItem
import piuk.blockchain.android.coincore.FiatActivitySummaryItem
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.android.coincore.NullCryptoAccount
import piuk.blockchain.android.coincore.btc.BtcActivitySummaryItem
import piuk.blockchain.android.coincore.erc20.Erc20ActivitySummaryItem
import piuk.blockchain.android.coincore.eth.EthActivitySummaryItem
import piuk.blockchain.android.repositories.AssetActivityRepository
import piuk.blockchain.android.util.StringUtils
import java.text.ParseException
import java.util.Date

class ActivityDetailsInteractor(
    private val currencyPrefs: CurrencyPrefs,
    private val transactionInputOutputMapper: TransactionInOutMapper,
    private val assetActivityRepository: AssetActivityRepository,
    private val custodialWalletManager: CustodialWalletManager,
    private val stringUtils: StringUtils,
    private val coincore: Coincore
) {

    fun loadCustodialTradingItems(
        summaryItem: CustodialTradingActivitySummaryItem
    ): Single<List<ActivityDetailsType>> {
        val list = mutableListOf(
            TransactionId(summaryItem.txId),
            Created(Date(summaryItem.timeStampMs)),
            if (summaryItem.type == OrderType.BUY)
                BuyPurchaseAmount(summaryItem.fundedFiat)
            else
                SellPurchaseAmount(summaryItem.fundedFiat),
            if (summaryItem.type == OrderType.BUY)
                BuyCryptoWallet(summaryItem.cryptoCurrency)
            else
                SellCryptoWallet(summaryItem.fundedFiat.currencyCode),
            BuyFee(summaryItem.fee)
        )

        return if (summaryItem.paymentMethodType == PaymentMethodType.PAYMENT_CARD) {
            custodialWalletManager.getCardDetails(
                summaryItem.paymentMethodId
            )
                .map { paymentMethod ->
                    addPaymentDetailsToList(list, paymentMethod, summaryItem)

                    list.toList()
                }.onErrorReturn {
                    addPaymentDetailsToList(list, null, summaryItem)

                    list.toList()
                }
        } else {
            list.add(BuyPaymentMethod(
                PaymentDetails(summaryItem.paymentMethodId, label = null,
                    endDigits = null
                )))

            if (summaryItem.status == OrderState.AWAITING_FUNDS ||
                summaryItem.status == OrderState.PENDING_EXECUTION
            ) {
                list.add(CancelAction())
            }
            Single.just(list.toList())
        }
    }

    fun loadCustodialInterestItems(
        summaryItem: CustodialInterestActivitySummaryItem
    ): Single<List<ActivityDetailsType>> {
        val list = mutableListOf(
            TransactionId(summaryItem.txId),
            Created(Date(summaryItem.timeStampMs))
        )
        when (summaryItem.type) {
            TransactionSummary.TransactionType.DEPOSIT -> {
                list.add(To(summaryItem.account.label))
            }
            TransactionSummary.TransactionType.WITHDRAW -> {
                list.add(From(stringUtils.getString(R.string.common_company_name)))
            }
            TransactionSummary.TransactionType.INTEREST_EARNED -> {
                list.add(From(stringUtils.getString(R.string.common_company_name)))
                list.add(To(summaryItem.account.label))
            }
            else -> {
                // do nothing
            }
        }
        return if (summaryItem.type == TransactionSummary.TransactionType.WITHDRAW) {
            coincore.findAccountByAddress(summaryItem.account.asset,
                summaryItem.accountRef).map {
                list.add(To(if (it !is NullCryptoAccount) {
                    it.label
                } else {
                    summaryItem.accountRef
                }))
                list.toList()
            }.toSingle()
        } else {
            Single.just(list.toList())
        }
    }

    private fun addPaymentDetailsToList(
        list: MutableList<ActivityDetailsType>,
        paymentMethod: PaymentMethod.Card?,
        summaryItem: CustodialTradingActivitySummaryItem
    ) {
        paymentMethod?.let {
            list.add(BuyPaymentMethod(PaymentDetails(
                it.cardId, it.uiLabel(), it.endDigits
            )))
        } ?: list.add(BuyPaymentMethod(
            PaymentDetails(summaryItem.paymentMethodId,
                label = null, endDigits = null)
        ))

        if (summaryItem.status == OrderState.PENDING_CONFIRMATION) {
            list.add(CancelAction())
        }
    }

    fun getCustodialTradingActivityDetails(
        cryptoCurrency: CryptoCurrency,
        txHash: String
    ): CustodialTradingActivitySummaryItem? =
        assetActivityRepository.findCachedItem(cryptoCurrency,
            txHash) as? CustodialTradingActivitySummaryItem

    fun getCustodialInterestActivityDetails(
        cryptoCurrency: CryptoCurrency,
        txHash: String
    ): CustodialInterestActivitySummaryItem? =
        assetActivityRepository.findCachedItem(cryptoCurrency,
            txHash) as? CustodialInterestActivitySummaryItem

    fun getFiatActivityDetails(
        currency: String,
        txHash: String
    ): FiatActivitySummaryItem? =
        assetActivityRepository.findCachedItem(currency, txHash) as? FiatActivitySummaryItem

    fun getNonCustodialActivityDetails(
        cryptoCurrency: CryptoCurrency,
        txHash: String
    ): NonCustodialActivitySummaryItem? =
        assetActivityRepository.findCachedItem(cryptoCurrency,
            txHash) as? NonCustodialActivitySummaryItem

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
        HistoricValue(fiatValue, item.transactionType),
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
        HistoricValue(fiatValue, item.transactionType),
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
        HistoricValue(fiatValue, item.transactionType),
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
        HistoricValue(fiatValue, item.transactionType),
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
        return when (val activityItem =
            assetActivityRepository.findCachedItem(cryptoCurrency, txId)) {
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
                val relatedItem =
                    assetActivityRepository.findCachedItemById(item.ethTransaction.hash)
                relatedItem?.let {
                    FeeForTransaction(
                        item.transactionType,
                        it.value
                    )
                }
            }
            else -> null
        }
    }

    private fun addSingleOrMultipleFromAddresses(
        it: TransactionInOutDetails?
    ) = From(
        when {
            it == null -> null
            it.inputs.size == 1 -> it.inputs[0].address
            else -> it.inputs.toJoinedString()
        }
    )

    private fun addSingleOrMultipleToAddresses(
        it: TransactionInOutDetails?
    ) = To(
        when {
            it == null -> null
            it.outputs.size == 1 -> it.outputs[0].address
            else -> it.outputs.toJoinedString()
        }
    )

    private fun List<TransactionDetailModel>.toJoinedString(): String =
        this.map { o -> o.address }.joinToString("\n")

    private fun checkIfShouldAddDescription(
        item: NonCustodialActivitySummaryItem
    ): Description? = when (item) {
        is BtcActivitySummaryItem,
        is EthActivitySummaryItem,
        is Erc20ActivitySummaryItem -> Description(item.description)
        else -> null
    }
}