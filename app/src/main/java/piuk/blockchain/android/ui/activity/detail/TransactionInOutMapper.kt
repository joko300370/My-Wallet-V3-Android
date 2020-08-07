package piuk.blockchain.android.ui.activity.detail

import com.blockchain.sunriver.XlmDataManager
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRates
import info.blockchain.balance.Money
import info.blockchain.wallet.multiaddress.MultiAddressFactory
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SendProcessor
import piuk.blockchain.android.coincore.SendState
import piuk.blockchain.android.coincore.SendTarget
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class TransactionInOutMapper(
    private val transactionHelper: TransactionHelper,
    private val payloadDataManager: PayloadDataManager,
    private val stringUtils: StringUtils,
    private val ethDataManager: EthDataManager,
    private val bchDataManager: BchDataManager,
    private val xlmDataManager: XlmDataManager,
    private val environmentSettings: EnvironmentConfig,
    private val coincore: Coincore
) {

    fun transformInputAndOutputs(
        item: NonCustodialActivitySummaryItem
    ): Single<TransactionInOutDetails> =
        when (item.cryptoCurrency) {
            CryptoCurrency.BTC -> handleBtcToAndFrom(item)
            CryptoCurrency.BCH -> handleBchToAndFrom(item)
            CryptoCurrency.XLM -> handleXlmToAndFrom(item)
            CryptoCurrency.PAX,
            CryptoCurrency.USDT,
            CryptoCurrency.ETHER -> handleErc20ToAndFrom(item)
            else -> throw IllegalArgumentException(
                "${item.cryptoCurrency} is not currently supported")
        }

    private fun handleXlmToAndFrom(activitySummaryItem: NonCustodialActivitySummaryItem) =
        xlmDataManager.defaultAccount()
            .map { account ->
                var fromAddress = activitySummaryItem.inputsMap.keys.first()
                var toAddress = activitySummaryItem.outputsMap.keys.first()
                if (fromAddress == account.accountId) {
                    fromAddress = account.label
                }
                if (toAddress == account.accountId) {
                    toAddress = account.label
                }

                TransactionInOutDetails(
                    inputs = listOf(
                        TransactionDetailModel(
                            fromAddress
                        )
                    ),
                    outputs = listOf(
                        TransactionDetailModel(
                            toAddress
                        )
                    )
                )
            }

    private fun handleErc20ToAndFrom(
        activitySummaryItem: NonCustodialActivitySummaryItem
    ): Single<TransactionInOutDetails> {

        val fromAddress = activitySummaryItem.inputsMap.keys.first()
        val toAddress = activitySummaryItem.outputsMap.keys.first()

        return Singles.zip(
            coincore.findAccountByAddress(activitySummaryItem.cryptoCurrency, fromAddress)
                .toSingle(EmptyAccount(fromAddress)),
            coincore.findAccountByAddress(activitySummaryItem.cryptoCurrency, toAddress)
                .toSingle(EmptyAccount(toAddress))
        ) { a, b ->
            TransactionInOutDetails(
                inputs = listOf(
                    TransactionDetailModel(
                        a.label
                    )
                ),
                outputs = listOf(
                    TransactionDetailModel(
                        b.label
                    )
                )
            )
        }
    }

    private fun handleBtcToAndFrom(activitySummaryItem: NonCustodialActivitySummaryItem) =
        Single.fromCallable {
            val (inputs, outputs) = transactionHelper.filterNonChangeBtcAddresses(
                activitySummaryItem)
            setToAndFrom(CryptoCurrency.BTC, inputs, outputs)
        }

    private fun handleBchToAndFrom(activitySummaryItem: NonCustodialActivitySummaryItem) =
        Single.fromCallable {
            val (inputs, outputs) = transactionHelper.filterNonChangeBchAddresses(
                activitySummaryItem)
            setToAndFrom(CryptoCurrency.BCH, inputs, outputs)
        }

    private fun setToAndFrom(
        cryptoCurrency: CryptoCurrency,
        inputs: Map<String, Money>,
        outputs: Map<String, Money>
    ) = TransactionInOutDetails(
        inputs = getFromList(cryptoCurrency, inputs),
        outputs = getToList(cryptoCurrency, outputs)
    )

    private fun getFromList(
        currency: CryptoCurrency,
        inputMap: Map<String, Money>
    ): List<TransactionDetailModel> {
        val inputs = handleTransactionMap(inputMap, currency)
        // No inputs = coinbase transaction
        if (inputs.isEmpty()) {
            val coinbase =
                TransactionDetailModel(
                    address = stringUtils.getString(R.string.transaction_detail_coinbase),
                    displayUnits = currency.displayTicker
                )
            inputs.add(coinbase)
        }
        return inputs.toList()
    }

    private fun getToList(
        currency: CryptoCurrency,
        outputMap: Map<String, Money>
    ): List<TransactionDetailModel> = handleTransactionMap(outputMap, currency)

    private fun handleTransactionMap(
        inputMap: Map<String, Money>,
        currency: CryptoCurrency
    ): MutableList<TransactionDetailModel> {
        val inputs = mutableListOf<TransactionDetailModel>()
        for ((key, value) in inputMap) {
            val label = if (currency == CryptoCurrency.BTC) {
                payloadDataManager.addressToLabel(key)
            } else {
                bchDataManager.getLabelFromBchAddress(key)
                    ?: FormatsUtil.toShortCashAddress(
                        environmentSettings.bitcoinCashNetworkParameters, key)
            }

            val transactionDetailModel = buildTransactionDetailModel(label, value, currency)
            inputs.add(transactionDetailModel)
        }
        return inputs
    }

    private fun buildTransactionDetailModel(
        label: String,
        value: Money,
        cryptoCurrency: CryptoCurrency
    ): TransactionDetailModel =
        TransactionDetailModel(
            label,
            value.toStringWithoutSymbol(),
            cryptoCurrency.displayTicker
        ).apply {
            if (address == MultiAddressFactory.ADDRESS_DECODE_ERROR) {
                address = stringUtils.getString(R.string.tx_decode_error)
                addressDecodeError = true
            }
        }

    private class EmptyAccount(override val label: String) : SingleAccount {
        override val receiveAddress: Single<ReceiveAddress>
            get() = Single.error(NotImplementedError("Empty Account"))

        override val isDefault: Boolean
            get() = false

        override fun createSendProcessor(sendTo: SendTarget): Single<SendProcessor> =
            Single.error(NotImplementedError("Empty Account"))

        override val sendState: Single<SendState>
            get() = Single.error(NotImplementedError("Empty Account"))

        override val balance: Single<Money>
            get() = Single.error(NotImplementedError("Empty Account"))

        override val activity: Single<ActivitySummaryList>
            get() = Single.error(NotImplementedError("Empty Account"))

        override val actions: AvailableActions = emptySet()
        override val isFunded: Boolean = false
        override val hasTransactions: Boolean = false

        override fun fiatBalance(
            fiatCurrency: String,
            exchangeRates: ExchangeRates
        ): Single<Money> =
            Single.error(NotImplementedError("Empty Account"))
    }
}
