package piuk.blockchain.android.ui.send.external

import android.annotation.SuppressLint
import android.support.design.widget.Snackbar
import android.text.Editable
import android.widget.EditText
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.serialization.JsonSerializableAccount
import com.blockchain.sunriver.isValidXlmQr
import com.blockchain.transactions.Memo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.withMajorValueOrZero
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.data.api.bitpay.BitPayDataManager
import piuk.blockchain.android.data.api.bitpay.PATH_BITPAY_INVOICE
import piuk.blockchain.android.data.api.bitpay.BITPAY_LIVE_BASE
import piuk.blockchain.android.ui.send.DisplayFeeOptions
import piuk.blockchain.android.ui.send.SendView
import piuk.blockchain.android.ui.send.strategy.BitPayProtocol
import piuk.blockchain.android.ui.send.strategy.SendStrategy
import piuk.blockchain.android.util.EditTextFormatUtil
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.FiatExchangeRates
import piuk.blockchain.androidcore.data.exchangerate.toCrypto
import piuk.blockchain.androidcore.data.exchangerate.toFiat
import piuk.blockchain.androidcore.utils.PersistentPrefs
import timber.log.Timber
import java.util.regex.Pattern

/**
 * Does some of the basic work, using the [BaseSendView] interface.
 * Delegates the rest of the work to one of the [SendPresenterStrategy] supplied to it depending on currency.
 */
internal class PerCurrencySendPresenter<View : SendView>(
    private val btcStrategy: SendStrategy<View>,
    private val bchStrategy: SendStrategy<View>,
    private val etherStrategy: SendStrategy<View>,
    private val xlmStrategy: SendStrategy<View>,
    private val paxStrategy: SendStrategy<View>,
    private val exchangeRates: FiatExchangeRates,
    private val envSettings: EnvironmentConfig,
    private val stringUtils: StringUtils,
    private val exchangeRateFactory: ExchangeRateDataManager,
    private val prefs: PersistentPrefs,
    private val pitLinkingFeatureFlag: FeatureFlag,
    private val bitpayDataManager: BitPayDataManager
) : SendPresenter<View>() {

    override fun onPitAddressSelected() {
        delegate.onPitAddressSelected()
    }

    override fun onPitAddressCleared() {
        delegate.onPitAddressCleared()
    }

    private var selectedMemoType: Int = MEMO_TEXT_NONE
    private var selectedCrypto: CryptoCurrency = CryptoCurrency.BTC
    private val merchantPattern: Pattern = Pattern.compile("for merchant ")

    override fun getFeeOptionsForDropDown(): List<DisplayFeeOptions> {
        val regular = DisplayFeeOptions(
            stringUtils.getString(R.string.fee_options_regular),
            stringUtils.getString(R.string.fee_options_regular_time)
        )
        val priority = DisplayFeeOptions(
            stringUtils.getString(R.string.fee_options_priority),
            stringUtils.getString(R.string.fee_options_priority_time)
        )
        val custom = DisplayFeeOptions(
            stringUtils.getString(R.string.fee_options_custom),
            stringUtils.getString(R.string.fee_options_custom_warning)
        )
        return listOf(regular, priority, custom)
    }

    private var delegate: SendStrategy<View> = btcStrategy
        set(value) {
            field.reset()
            field = value
            field.initView(view)
            field.onViewReady()
        }

    override fun onContinueClicked() = delegate.onContinueClicked()

    override fun onSpendMaxClicked() = delegate.onSpendMaxClicked()

    override fun onMemoTypeChanged(memo: Int) {
        this.selectedMemoType = memo
    }

    override fun onBroadcastReceived() {
        updateTicker()
        delegate.onBroadcastReceived()
    }

    @SuppressLint("CheckResult")
    private fun updateTicker() {
        exchangeRateFactory.updateTickers()
            .addToCompositeDisposable(this)
            .subscribe(
                { /* No-op */ },
                { Timber.e(it) }
            )
    }

    override fun onResume() {
        delegate.onResume()
    }

    override fun onCurrencySelected(currency: CryptoCurrency) {
        delegate = when (currency) {
            CryptoCurrency.BTC -> btcStrategy
            CryptoCurrency.ETHER -> etherStrategy
            CryptoCurrency.BCH -> bchStrategy
            CryptoCurrency.XLM -> xlmStrategy
            CryptoCurrency.PAX -> paxStrategy
        }

        selectedCrypto = currency
        updateTicker()
        view?.setSelectedCurrency(currency)

        delegate.onCurrencySelected()
    }

    override fun handleURIScan(untrimmedscanData: String, defaultCurrency: CryptoCurrency) {
        if (untrimmedscanData == null)
            return

        val address: String

        if (untrimmedscanData.isValidXlmQr()) {
            onCurrencySelected(CryptoCurrency.XLM)
            address = untrimmedscanData
        } else {

            var scanData = untrimmedscanData.trim { it <= ' ' }
                .replace("ethereum:", "")

            scanData = FormatsUtil.getURIFromPoorlyFormedBIP21(scanData)

            when {
                FormatsUtil.isValidBitcoinCashAddress(envSettings.bitcoinCashNetworkParameters, scanData) -> {
                    onCurrencySelected(CryptoCurrency.BCH)
                    address = scanData
                }
                FormatsUtil.isBitcoinUri(envSettings.bitcoinNetworkParameters, scanData) -> {
                    onCurrencySelected(CryptoCurrency.BTC)
                    address = FormatsUtil.getBitcoinAddress(scanData)

                    val amount: String = FormatsUtil.getBitcoinAmount(scanData)
                    val paymentRequestUrl = FormatsUtil.getPaymentRequestUrl(scanData)
                    val bitpayInvoiceUrl = BITPAY_LIVE_BASE + PATH_BITPAY_INVOICE + "/"

                    if (address.isEmpty() && amount == "0.0000" &&
                        paymentRequestUrl.contains(bitpayInvoiceUrl)) {
                        // get payment protocol request data from bitpay
                        val invoiceId = paymentRequestUrl.replace(bitpayInvoiceUrl, "")
                        bitpayDataManager.getRawPaymentRequest(invoiceId = invoiceId)
                            .doOnSuccess {
                                val cryptoValue = CryptoValue(selectedCrypto, it.outputs[0].amount)
                                val merchant = it.memo.split(merchantPattern)[1]
                                val bitpayProtocol: BitPayProtocol? = delegate as? BitPayProtocol

                                bitpayProtocol?.setbitpayReceivingAddress(it.outputs[0].address)
                                bitpayProtocol?.setbitpayMerchant(merchant)
                                bitpayProtocol?.setIsBitpayPaymentRequest(true)
                                view?.let { view ->
                                    view.disableInput()
                                    view.showBitPayTimerAndMerchantInfo(it.expires, merchant)
                                    view.updateCryptoAmount(cryptoValue)
                                    view.updateReceivingAddress("bitcoin:?r=" + it.paymentUrl)
                                    view.setFeePrioritySelection(1)
                                    view.disableFeeDropdown()
                                }
                            }.doOnError {
                                Timber.e(it)
                            }.subscribe()
                    } else {
                        // Convert to correct units
                        try {
                            val cryptoValue = CryptoValue(selectedCrypto, amount.toBigInteger())
                            val fiatValue = cryptoValue.toFiat(exchangeRates)
                            view?.updateCryptoAmount(cryptoValue)
                            view?.updateFiatAmount(fiatValue)
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                }
                FormatsUtil.isValidBitcoinAddress(envSettings.bitcoinNetworkParameters, scanData) -> {
                    address = if (selectedCrypto == CryptoCurrency.BTC) {
                        onCurrencySelected(CryptoCurrency.BTC)
                        scanData
                    } else {
                        onCurrencySelected(CryptoCurrency.BCH)
                        scanData
                    }
                }
                FormatsUtil.isValidEthereumAddress(scanData) -> {
                    when (selectedCrypto) {
                        CryptoCurrency.ETHER -> onCurrencySelected(CryptoCurrency.ETHER)
                        CryptoCurrency.PAX -> onCurrencySelected(CryptoCurrency.PAX)
                        else -> {
                            if (defaultCurrency in listOf(CryptoCurrency.ETHER, CryptoCurrency.PAX)) {
                                onCurrencySelected(defaultCurrency)
                            } else {
                                onCurrencySelected(CryptoCurrency.ETHER) // Default to ETH
                            }
                        }
                    }

                    address = scanData
                    view?.updateCryptoAmount(CryptoValue.zero(selectedCrypto))
                }
                else -> {
                    onCurrencySelected(CryptoCurrency.BTC)
                    view.showSnackbar(R.string.invalid_address, Snackbar.LENGTH_LONG)
                    return
                }
            }
        }

        if (address != "") {
            delegate.processURIScanAddress(address)
        }
    }

    override fun handlePrivxScan(scanData: String?) = delegate.handlePrivxScan(scanData)

    override fun clearReceivingObject() = delegate.clearReceivingObject()

    override fun selectSendingAccount(account: JsonSerializableAccount?) =
        delegate.selectSendingAccount(account)

    override fun selectReceivingAccount(account: JsonSerializableAccount?) =
        delegate.selectReceivingAccount(account)

    override fun updateCryptoTextField(editable: Editable, amountFiat: EditText) {
        val maxLength = 2
        val fiat = EditTextFormatUtil.formatEditable(
            editable,
            maxLength,
            amountFiat,
            getDefaultDecimalSeparator()
        ).toString()

        val fiatValue = FiatValue.fromMajorOrZero(prefs.selectedFiatCurrency, fiat)
        val cryptoValue = fiatValue.toCrypto(exchangeRates, selectedCrypto)

        view.updateCryptoAmount(cryptoValue, true)
    }

    override fun updateFiatTextField(editable: Editable, amountCrypto: EditText) {
        val crypto = EditTextFormatUtil.formatEditable(
            editable,
            selectedCrypto.dp,
            amountCrypto,
            getDefaultDecimalSeparator()
        ).toString()

        val cryptoValue = selectedCrypto.withMajorValueOrZero(crypto)
        val fiatValue = cryptoValue.toFiat(exchangeRates)

        view.updateFiatAmount(fiatValue, true)
    }

    override fun selectDefaultOrFirstFundedSendingAccount() = delegate.selectDefaultOrFirstFundedSendingAccount()

    override fun submitPayment() = delegate.submitPayment()

    override fun onCryptoTextChange(cryptoText: String) = delegate.onCryptoTextChange(cryptoText)

    override fun onAddressTextChange(address: String) = delegate.onAddressTextChange(address)

    override fun onMemoChange(memoText: String) =
        delegate.onMemoChange(Memo(memoText, getMemoTypeRawValue(selectedMemoType)))

    private fun getMemoTypeRawValue(selectedMemoType: Int): String? =
        when (selectedMemoType) {
            MEMO_TEXT_TYPE -> "text"
            MEMO_ID_TYPE -> "id"
            else -> null
        }

    override fun spendFromWatchOnlyBIP38(pw: String, scanData: String) =
        delegate.spendFromWatchOnlyBIP38(pw, scanData)

    override fun onNoSecondPassword() = delegate.onNoSecondPassword()

    override fun onSecondPasswordValidated(validateSecondPassword: String) =
        delegate.onSecondPasswordValidated(validateSecondPassword)

    override fun getBitcoinFeeOptions() = delegate.getFeeOptions()

    override fun onViewReady() {
        updateTicker()

        if (envSettings.environment == Environment.TESTNET) {
            selectedCrypto = CryptoCurrency.BTC
            view.hideCurrencyHeader()
        }
        compositeDisposable += delegate.memoRequired().startWith(false).subscribe {
            view?.updateRequiredLabelVisibility(it)
        }

        compositeDisposable += pitLinkingFeatureFlag.enabled.subscribeBy {
            view.isPitEnabled(it)
        }
    }

    override fun disableAdvancedFeeWarning() {
        prefs.setValue(PersistentPrefs.KEY_WARN_ADVANCED_FEE, false)
    }

    override fun shouldShowAdvancedFeeWarning(): Boolean =
        prefs.getValue(PersistentPrefs.KEY_WARN_ADVANCED_FEE, true)

    override fun setWarnWatchOnlySpend(warn: Boolean) {
        prefs.setValue(PersistentPrefs.KEY_WARN_WATCH_ONLY_SPEND, warn)
    }
}
