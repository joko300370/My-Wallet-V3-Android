package piuk.blockchain.android.ui.receive

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import com.blockchain.extensions.exhaustive
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.isValidXlmQr
import com.blockchain.sunriver.toUri
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.withMajorValueOrZero
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.uri.BitcoinURI
import piuk.blockchain.android.R
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager
import piuk.blockchain.android.ui.base.MvpPresenter
import piuk.blockchain.android.ui.base.MvpView
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.ethereum.datastores.EthDataStore
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.toCrypto
import piuk.blockchain.androidcore.data.exchangerate.toFiat
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.extensions.toSafeLong
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import timber.log.Timber
import java.math.BigInteger
import java.util.Locale

interface ReceiveView : MvpView {
    fun getQrBitmap(): Bitmap
    fun getBtcAmount(): String
    fun showQrLoading()
    fun showQrCode(bitmap: Bitmap?)
    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)
    fun updateFiatTextField(text: String)
    fun updateBtcTextField(text: String)
    fun updateReceiveAddress(address: String)
    fun showWatchOnlyWarning()
    fun updateReceiveLabel(label: String)
    fun showShareBottomSheet(uri: String)
    fun setSelectedCurrency(cryptoCurrency: CryptoCurrency)
    fun finishPage()
    fun disableCurrencyHeader()
}

class ReceivePresenter(
    private val prefs: PersistentPrefs,
    private val qrCodeDataManager: QrCodeDataManager,
    private val walletAccountHelper: WalletAccountHelper,
    private val payloadDataManager: PayloadDataManager,
    private val ethDataStore: EthDataStore,
    private val bchDataManager: BchDataManager,
    private val xlmDataManager: XlmDataManager,
    private val environmentSettings: EnvironmentConfig,
    private val currencyState: CurrencyState,
    private val exchangeRates: ExchangeRateDataManager
) : MvpPresenter<ReceiveView>() {

    override val alwaysDisableScreenshots = false
    override val enableLogoutTimer = true

    @VisibleForTesting
    internal var selectedAddress: String? = null
    @VisibleForTesting
    internal var selectedAccount: Account? = null
    @VisibleForTesting
    internal var selectedBchAccount: GenericMetadataAccount? = null

    fun getMaxCryptoDecimalLength() = currencyState.cryptoCurrency.dp

    val cryptoUnit
        get() = currencyState.cryptoCurrency.displayTicker

    val fiatUnit
        get() = prefs.selectedFiatCurrency

    override fun onViewReady() {
        super.onViewReady()
        if (environmentSettings.environment == Environment.TESTNET) {
            currencyState.cryptoCurrency = CryptoCurrency.BTC
            view?.disableCurrencyHeader()
        }
    }

    override fun onViewAttached() {}
    override fun onViewDetached() {}

    internal fun onResume(defaultAccountPosition: Int) {
        when (currencyState.cryptoCurrency) {
            CryptoCurrency.BTC -> onSelectDefault(defaultAccountPosition)
            CryptoCurrency.ETHER -> onEthSelected()
            CryptoCurrency.BCH -> onSelectBchDefault()
            CryptoCurrency.XLM -> onXlmSelected()
            CryptoCurrency.PAX -> onPaxSelected()
            CryptoCurrency.STX -> TODO("STX is not yet fully supported")
            CryptoCurrency.ALGO -> TODO("ALG is not yet fully supported")
        }.exhaustive
    }

    internal fun isValidAmount(btcAmount: String) = btcAmount.toSafeLong(Locale.getDefault()) > 0

    internal fun shouldShowAccountDropdown() =
        walletAccountHelper.hasMultipleEntries(currencyState.cryptoCurrency)

    internal fun onLegacyAddressSelected(legacyAddress: LegacyAddress) {
        if (legacyAddress.isWatchOnly && shouldWarnWatchOnly()) {
            view?.showWatchOnlyWarning()
        }

        selectedAccount = null
        selectedBchAccount = null
        view?.updateReceiveLabel(
            if (!legacyAddress.label.isNullOrEmpty()) {
                legacyAddress.label
            } else {
                legacyAddress.address
            }
        )

        legacyAddress.address.let {
            selectedAddress = it
            view?.apply {
                updateReceiveAddress(it)
                generateQrCode(getBitcoinUri(it, getBtcAmount()))
            }
        }
    }

    internal fun onLegacyBchAddressSelected(legacyAddress: LegacyAddress) {
        // Here we are assuming that the legacy address is in Base58. This may change in the future
        // if we decide to allow importing BECH32 paper wallets.
        val address = Address.fromBase58(
            environmentSettings.bitcoinCashNetworkParameters,
            legacyAddress.address
        )
        val bech32 = address.toCashAddress()
        val bech32Display = bech32.removeBchUri()

        if (legacyAddress.isWatchOnly && shouldWarnWatchOnly()) {
            view?.showWatchOnlyWarning()
        }

        selectedAccount = null
        selectedBchAccount = null
        view?.updateReceiveLabel(
            if (!legacyAddress.label.isNullOrEmpty()) {
                legacyAddress.label
            } else {
                bech32Display
            }
        )

        selectedAddress = bech32
        view?.updateReceiveAddress(bech32Display)
        generateQrCode(bech32)
    }

    internal fun onAccountBtcSelected(account: Account) {
        currencyState.cryptoCurrency = CryptoCurrency.BTC
        view?.setSelectedCurrency(currencyState.cryptoCurrency)
        selectedAccount = account
        selectedBchAccount = null
        view?.updateReceiveLabel(account.label)

        compositeDisposable += payloadDataManager.updateAllTransactions()
            .doOnSubscribe { view?.showQrLoading() }
            .onErrorComplete()
            .andThen(payloadDataManager.getNextReceiveAddress(account))
            .doOnNext {
                selectedAddress = it
                view?.apply {
                    updateReceiveAddress(it)
                    generateQrCode(getBitcoinUri(it, getBtcAmount()))
                }
            }
            .doOnError { Timber.e(it) }
            .subscribe(
                { /* No-op */ },
                { view?.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR) })
    }

    internal fun onEthSelected() {
        currencyState.cryptoCurrency = CryptoCurrency.ETHER
        compositeDisposable.clear()
        view?.setSelectedCurrency(CryptoCurrency.ETHER)
        selectedAccount = null
        selectedBchAccount = null

        lookupEthAccountAndUpdateView()
    }

    internal fun onPaxSelected() {
        currencyState.cryptoCurrency = CryptoCurrency.PAX
        compositeDisposable.clear()
        view?.setSelectedCurrency(CryptoCurrency.PAX)
        selectedAccount = null
        selectedBchAccount = null

        lookupEthAccountAndUpdateView()
    }

    private fun lookupEthAccountAndUpdateView() {
        // This can be null at this stage for some reason - TODO investigate thoroughly
        val account: String? = ethDataStore.ethAddressResponse?.getAddressResponse()?.account
        if (account != null) {
            account.let {
                selectedAddress = it
                view?.updateReceiveAddress(it)
                generateQrCode(it)
            }
        } else {
            view?.finishPage()
        }
    }

    internal fun onXlmSelected() {
        currencyState.cryptoCurrency = CryptoCurrency.XLM
        compositeDisposable.clear()
        view?.setSelectedCurrency(CryptoCurrency.XLM)
        selectedAccount = null
        selectedBchAccount = null
        compositeDisposable += xlmDataManager.defaultAccount()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { account ->
                    account.let {
                        selectedAddress = it.toUri()
                        view?.updateReceiveAddress(it.accountId)
                        generateQrCode(it.toUri())
                    }
                },
                onError = {
                    view?.finishPage()
                })
    }

    internal fun onSelectBchDefault() {
        compositeDisposable.clear()
        onBchAccountSelected(bchDataManager.getDefaultGenericMetadataAccount()!!)
    }

    internal fun onBchAccountSelected(account: GenericMetadataAccount) {
        currencyState.cryptoCurrency = CryptoCurrency.BCH
        view?.setSelectedCurrency(CryptoCurrency.BCH)
        selectedAccount = null
        selectedBchAccount = account
        view?.updateReceiveLabel(account.label)
        val position =
            bchDataManager.getAccountMetadataList().indexOfFirst { it.xpub == account.xpub }

        compositeDisposable += bchDataManager.updateAllBalances()
            .doOnSubscribe { view?.showQrLoading() }
            .andThen(
                bchDataManager.getWalletTransactions(50, 0)
                    .onErrorReturn { emptyList() }
            )
            .flatMap { bchDataManager.getNextReceiveAddress(position) }
            .doOnNext {
                val address = Address.fromBase58(environmentSettings.bitcoinCashNetworkParameters, it)
                val bech32 = address.toCashAddress()
                selectedAddress = bech32
                view?.updateReceiveAddress(bech32.removeBchUri())
                generateQrCode(bech32)
            }
            .doOnError { Timber.e(it) }
            .subscribe(
                { /* No-op */ },
                { view?.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR) }
            )
    }

    internal fun onSelectDefault(defaultAccountPosition: Int) {
        compositeDisposable.clear()
        onAccountBtcSelected(
            if (defaultAccountPosition > -1) {
                payloadDataManager.getAccount(defaultAccountPosition)
            } else {
                payloadDataManager.defaultAccount
            }
        )
    }

    internal fun onBitcoinAmountChanged(amount: String) {
        val amountBigInt = amount.toSafeLong(Locale.getDefault())

        if (isValidAmount(amountBigInt)) {
            view?.showToast(R.string.invalid_amount, ToastCustom.TYPE_ERROR)
        }

        generateQrCode(getBitcoinUri(selectedAddress!!, amount))
    }

    private fun isValidAmount(amount: Long): Boolean {
        return BigInteger.valueOf(amount).compareTo(BigInteger.valueOf(2_100_000_000_000_000L)) == 1
    }

    internal fun getSelectedAccountPosition(): Int {
        return if (currencyState.cryptoCurrency == CryptoCurrency.ETHER) {
            -1
        } else {
            val position = payloadDataManager.accounts.asIterable()
                .indexOfFirst { it.xpub == selectedAccount?.xpub }
            payloadDataManager.getPositionOfAccountInActiveList(
                if (position > -1) position else payloadDataManager.defaultAccountIndex
            )
        }
    }

    internal fun setWarnWatchOnlySpend(warn: Boolean) {
        prefs.setValue(KEY_WARN_WATCH_ONLY_SPEND, warn)
    }

    internal fun onShowBottomShareSheetSelected() {
        val v = view ?: return
        selectedAddress?.let {
            when {
                FormatsUtil.isValidBitcoinAddress(it) ->
                    v.showShareBottomSheet(getBitcoinUri(it, v.getBtcAmount()))
                FormatsUtil.isValidEthereumAddress(it) ||
                        FormatsUtil.isValidBitcoinCashAddress(environmentSettings.bitcoinCashNetworkParameters, it) ||
                        it.isValidXlmQr() -> v.showShareBottomSheet(it)
                else -> throw IllegalStateException("Unknown address format $selectedAddress")
            }
        }
    }

    internal fun updateFiatTextField(bitcoin: String) {
        view?.updateFiatTextField(
            currencyState.cryptoCurrency.withMajorValueOrZero(bitcoin)
                .toFiat(exchangeRates, currencyState.fiatUnit)
                .toStringWithoutSymbol()
        )
    }

    internal fun updateBtcTextField(fiat: String) {
        view?.updateBtcTextField(
            FiatValue.fromMajorOrZero(fiatUnit, fiat)
                .toCrypto(exchangeRates, currencyState.cryptoCurrency)
                .toStringWithoutSymbol()
        )
    }

    private fun getBitcoinUri(address: String, amount: String): String {
        require(FormatsUtil.isValidBitcoinAddress(address)) {
            "$address is not a valid Bitcoin address"
        }

        val amountLong = amount.toSafeLong(Locale.getDefault())

        return if (amountLong > 0L) {
            BitcoinURI.convertToBitcoinURI(
                Address.fromBase58(environmentSettings.bitcoinNetworkParameters, address),
                Coin.valueOf(amountLong),
                "",
                ""
            )
        } else {
            "bitcoin:$address"
        }
    }

    private fun generateQrCode(uri: String) {
        view?.showQrLoading()
        compositeDisposable.clear()
        compositeDisposable += qrCodeDataManager.generateQrCode(uri, DIMENSION_QR_CODE)
            .subscribeBy(
                onNext = { view?.showQrCode(it) },
                onError = { view?.showQrCode(null) }
            )
    }

    private fun shouldWarnWatchOnly() = prefs.getValue(KEY_WARN_WATCH_ONLY_SPEND, true)

    private fun String.removeBchUri(): String = this.replace("bitcoincash:", "")

    companion object {

        @VisibleForTesting
        const val KEY_WARN_WATCH_ONLY_SPEND = "warn_watch_only_spend"
        private const val DIMENSION_QR_CODE = 600
    }
}
