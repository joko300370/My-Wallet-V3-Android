package piuk.blockchain.android.ui.transfer.send.activity

import android.text.Editable
import android.widget.EditText
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.serialization.JsonSerializableAccount
import com.blockchain.sunriver.isValidXlmQr
import com.blockchain.transactions.Memo
import com.google.android.material.snackbar.Snackbar
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.withMajorValueOrZero
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.data.api.bitpay.BITPAY_LIVE_BASE
import piuk.blockchain.android.data.api.bitpay.BitPayDataManager
import piuk.blockchain.android.data.api.bitpay.PATH_BITPAY_INVOICE
import piuk.blockchain.android.data.api.bitpay.models.events.BitPayEvent
import piuk.blockchain.android.ui.base.MvpPresenter
import piuk.blockchain.android.ui.transfer.send.activity.strategy.BitPayProtocol
import piuk.blockchain.android.ui.transfer.send.activity.strategy.SendStrategy
import piuk.blockchain.android.util.EditTextFormatUtil
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.toCrypto
import piuk.blockchain.androidcore.utils.PersistentPrefs
import timber.log.Timber
import java.text.DecimalFormatSymbols
import java.util.regex.Pattern

class DisplayFeeOptions(val title: String, val description: String)

/**
 * Does some of the basic work, but mostly delegates to one of the [SendPresenterStrategy]
 * supplied to it depending on currency.
 */
class SendPresenter<View : SendView>(
    private val btcStrategy: SendStrategy<View>,
    private val bchStrategy: SendStrategy<View>,
    private val xlmStrategy: SendStrategy<View>,
    private val exchangeRates: ExchangeRateDataManager,
    private val envSettings: EnvironmentConfig,
    private val stringUtils: StringUtils,
    private val exchangeRateFactory: ExchangeRateDataManager,
    private val prefs: PersistentPrefs,
    private val pitLinkingFeatureFlag: FeatureFlag,
    private val bitpayDataManager: BitPayDataManager,
    private val analytics: Analytics
) : MvpPresenter<View>() {

    fun getDefaultDecimalSeparator() = DecimalFormatSymbols.getInstance().decimalSeparator.toString()

    override fun onViewAttached() { }
    override fun onViewDetached() {
        delegate.detachView(view!!)
    }

    override val alwaysDisableScreenshots: Boolean = false
    override val enableLogoutTimer: Boolean = true

    fun onPitAddressSelected() {
        delegate.onPitAddressSelected()
    }

    fun onPitAddressCleared() {
        delegate.onPitAddressCleared()
    }

    private val bitpayInvoiceUrl = "$BITPAY_LIVE_BASE$PATH_BITPAY_INVOICE/"
    private var selectedMemoType: Int =
        MEMO_TEXT_NONE
    private var selectedCrypto: CryptoCurrency = CryptoCurrency.BTC
    private val merchantPattern: Pattern = Pattern.compile("for merchant ")

    fun getFeeOptionsForDropDown(): List<DisplayFeeOptions> {
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
            field.attachView(view!!)
            field.onViewReady()
        }

    fun onContinueClicked() = delegate.onContinueClicked()

    fun onSpendMaxClicked() = delegate.onSpendMaxClicked()

    fun onMemoTypeChanged(memo: Int) {
        this.selectedMemoType = memo
    }

    fun onBroadcastReceived() {
        updateTicker()
        delegate.onBroadcastReceived()
    }

    private fun updateTicker() {
        compositeDisposable += exchangeRateFactory.updateTickers()
            .subscribe(
                { /* No-op */ },
                { Timber.e(it) }
            )
    }

    fun onResume() {
        delegate.onResume()
    }

    fun setSourceAccount(account: CryptoAccount) {
        onCurrencySelected(account.asset)
        delegate.setSendingAccount(account)
    }

    private fun onCurrencySelected(asset: CryptoCurrency) {
        delegate = when (asset) {
            CryptoCurrency.BTC -> btcStrategy
            CryptoCurrency.BCH -> bchStrategy
            CryptoCurrency.XLM -> xlmStrategy
            else -> throw IllegalArgumentException("Old send no longer supports: ${asset.networkTicker}")
        }

        selectedCrypto = asset
        updateTicker()

        delegate.onCurrencySelected()
    }

    fun handlePredefinedInput(
        untrimmedscanData: String,
        defaultCurrency: CryptoCurrency,
        isDeepLinked: Boolean
    ) {
        val address: String

        if (untrimmedscanData.isValidXlmQr()) {
            onCurrencySelected(CryptoCurrency.XLM)
            address = untrimmedscanData
        } else {

            var scanData = untrimmedscanData.trim { it <= ' ' }
                .replace("ethereum:", "")

            scanData = FormatsUtil.getURIFromPoorlyFormedBIP21(scanData)

            when {
                FormatsUtil.isValidBCHAddress(envSettings.bitcoinCashNetworkParameters, scanData) -> {
                    onCurrencySelected(CryptoCurrency.BCH)
                    address = scanData
                }
                FormatsUtil.isBitcoinUri(envSettings.bitcoinNetworkParameters, scanData) -> {
                    onCurrencySelected(CryptoCurrency.BTC)
                    address = FormatsUtil.getBitcoinAddress(scanData)

                    val amount: String = FormatsUtil.getBitcoinAmount(scanData)
                    val paymentRequestUrl = FormatsUtil.getPaymentRequestUrl(scanData)

                    if (address.isEmpty() && scanData.isBitpayAddress()) {
                        // get payment protocol request data from bitpay
                        val invoiceId = paymentRequestUrl.replace(bitpayInvoiceUrl, "")
                        if (isDeepLinked) {
                            analytics.logEvent(BitPayEvent.InputEvent(AnalyticsEvents.BitpayUrlDeeplink.event,
                                CryptoCurrency.BTC))
                        } else {
                            analytics.logEvent(BitPayEvent.InputEvent(AnalyticsEvents.BitpayAdrressScanned.event,
                                CryptoCurrency.BTC))
                        }
                        handleBitPayInvoice(invoiceId)
                    } else {
                        // Convert to correct units
                        try {
                            val cryptoValue = CryptoValue(selectedCrypto, amount.toBigInteger())
                            val fiatValue = cryptoValue.toFiat(exchangeRates, prefs.selectedFiatCurrency)
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
                    view?.showSnackbar(R.string.invalid_address, Snackbar.LENGTH_LONG)
                    return
                }
            }
        }

        if (address != "") {
            delegate.processURIScanAddress(address)
        }
    }

    private fun handleBitPayInvoice(invoiceId: String) {
        compositeDisposable += bitpayDataManager.getRawPaymentRequest(invoiceId = invoiceId)
            .doOnSuccess {
                val cryptoValue = CryptoValue(selectedCrypto, it.instructions[0].outputs[0].amount)
                val merchant = it.memo.split(merchantPattern)[1]
                val bitpayProtocol: BitPayProtocol? = delegate as? BitPayProtocol ?: return@doOnSuccess

                bitpayProtocol?.setbitpayReceivingAddress(it.instructions[0].outputs[0].address)
                bitpayProtocol?.setbitpayMerchant(merchant)
                bitpayProtocol?.setInvoiceId(invoiceId)
                bitpayProtocol?.setIsBitpayPaymentRequest(true)
                view?.let { view ->
                    view.disableInput()
                    view.showBitPayTimerAndMerchantInfo(it.expires, merchant)
                    view.updateCryptoAmount(cryptoValue)
                    view.updateReceivingAddress("bitcoin:?r=" + it.paymentUrl)
                    view.setFeePrioritySelection(1)
                    view.disableFeeDropdown()
                    view.onBitPayAddressScanned()
                }
            }.doOnError {
                Timber.e(it)
            }.subscribe()
    }

    private fun String.isBitpayAddress(): Boolean {

        val amount = FormatsUtil.getBitcoinAmount(this)
        val paymentRequestUrl = FormatsUtil.getPaymentRequestUrl(this)
        return amount == "0.0000" &&
                paymentRequestUrl.contains(bitpayInvoiceUrl)
    }

    fun handlePrivxScan(scanData: String?) = delegate.handlePrivxScan(scanData)

    fun clearReceivingObject() = delegate.clearReceivingObject()

    fun selectReceivingAccount(account: JsonSerializableAccount?) =
        delegate.selectReceivingAccount(account)

    fun updateCryptoTextField(editable: Editable, amountFiat: EditText) {
        val maxLength = 2
        val fiat = EditTextFormatUtil.formatEditable(
            editable,
            maxLength,
            amountFiat,
            getDefaultDecimalSeparator()
        ).toString()

        val fiatValue = FiatValue.fromMajorOrZero(prefs.selectedFiatCurrency, fiat)
        val cryptoValue = fiatValue.toCrypto(exchangeRates, selectedCrypto)

        view?.updateCryptoAmount(cryptoValue, true)
    }

    fun updateFiatTextField(editable: Editable, amountCrypto: EditText) {
        val crypto = EditTextFormatUtil.formatEditable(
            editable,
            selectedCrypto.dp,
            amountCrypto,
            getDefaultDecimalSeparator()
        ).toString()

        val cryptoValue = selectedCrypto.withMajorValueOrZero(crypto)
        val fiatValue = cryptoValue.toFiat(exchangeRates, prefs.selectedFiatCurrency)

        view?.updateFiatAmount(fiatValue, true)
    }

    fun submitPayment() = delegate.submitPayment()

    fun onCryptoTextChange(cryptoText: String) = delegate.onCryptoTextChange(cryptoText)

    fun onAddressTextChange(address: String) {
        delegate.onAddressTextChange(address)
        if (delegate.isAddressValid(address)) {
            view?.hidePitIconForValidAddress()
        } else {
            view?.showPitIconIfAvailable()
        }

        val bitPayProtocol = delegate as? BitPayProtocol ?: return
        if (!bitPayProtocol.isBitpayPaymentRequest && address.isBitpayAddress()) {
            val invoiceId = address
                .replace(bitpayInvoiceUrl, "")
                .replace("bitcoin:?r=", "")
            handleBitPayInvoice(invoiceId)
            analytics.logEvent(BitPayEvent.InputEvent(AnalyticsEvents.BitpayUrlPasted.event, CryptoCurrency.BTC))
        }
    }

    fun onMemoChange(memoText: String) =
        delegate.onMemoChange(Memo(memoText, getMemoTypeRawValue(selectedMemoType)))

    private fun getMemoTypeRawValue(selectedMemoType: Int): String? =
        when (selectedMemoType) {
            MEMO_TEXT_TYPE -> "text"
            MEMO_ID_TYPE -> "id"
            else -> null
        }

    fun spendFromWatchOnlyBIP38(pw: String, scanData: String) =
        delegate.spendFromWatchOnlyBIP38(pw, scanData)

    fun onNoSecondPassword() = delegate.onNoSecondPassword()

    fun onSecondPasswordValidated(validateSecondPassword: String) =
        delegate.onSecondPasswordValidated(validateSecondPassword)

    fun getBitcoinFeeOptions() = delegate.getFeeOptions()

    override fun onViewReady() {
        updateTicker()

        if (envSettings.environment == Environment.TESTNET) {
            selectedCrypto = CryptoCurrency.BTC
        }
        compositeDisposable += delegate.memoRequired().startWith(false).subscribe {
            view?.updateRequiredLabelVisibility(it)
        }

        compositeDisposable += pitLinkingFeatureFlag.enabled.subscribeBy {
            view?.isPitEnabled(it)
        }
    }

    fun disableAdvancedFeeWarning() {
        prefs.setValue(PersistentPrefs.KEY_WARN_ADVANCED_FEE, false)
    }

    fun shouldShowAdvancedFeeWarning(): Boolean =
        prefs.getValue(PersistentPrefs.KEY_WARN_ADVANCED_FEE, true)

    fun setWarnWatchOnlySpend(warn: Boolean) {
        prefs.setValue(PersistentPrefs.KEY_WARN_WATCH_ONLY_SPEND, warn)
    }

    companion object {
        const val MEMO_TEXT_NONE = -1
        const val MEMO_TEXT_TYPE = 0
        const val MEMO_ID_TYPE = 1
    }
}
