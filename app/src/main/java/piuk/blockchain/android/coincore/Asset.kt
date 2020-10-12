package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan

enum class AssetFilter {
    All,
    NonCustodial,
    Custodial,
    Interest
}

enum class AssetAction {
    ViewActivity,
    NewSend,
    Withdraw,
    Receive,
    Swap,
    Sell,
    Summary,
    Deposit
}

typealias AvailableActions = Set<AssetAction>

interface Asset {
    fun init(): Completable
    val isEnabled: Boolean

    fun accountGroup(filter: AssetFilter = AssetFilter.All): Maybe<AccountGroup>

    fun transactionTargets(account: SingleAccount): Single<SingleAccountList>

    fun parseAddress(address: String): Maybe<ReceiveAddress>
}

interface CryptoAsset : Asset {
    val asset: CryptoCurrency

    fun defaultAccount(): Single<SingleAccount>
    fun interestRate(): Single<Double>

    // Fetch exchange rate to user's selected/display fiat
    fun exchangeRate(): Single<ExchangeRate>
    fun historicRate(epochWhen: Long): Single<ExchangeRate>
    fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries>
}

// TODO Address Parsing, from various places. Split and move to the appropriate token classes
// fun handlePredefinedInput(
//    untrimmedscanData: String,
//    defaultCurrency: CryptoCurrency,
//    isDeepLinked: Boolean
// ) {
//    val address: String
//
//    if (untrimmedscanData.isValidXlmQr()) {
//        onCurrencySelected(CryptoCurrency.XLM)
//        address = untrimmedscanData
//    } else {
//
//        var scanData = untrimmedscanData.trim { it <= ' ' }
//            .replace("ethereum:", "")
//
//        scanData = FormatsUtil.getURIFromPoorlyFormedBIP21(scanData)
//
//        when {
//            FormatsUtil.isValidBitcoinCashAddress(envSettings.bitcoinCashNetworkParameters, scanData) -> {
//                onCurrencySelected(CryptoCurrency.BCH)
//                address = scanData
//            }
//            FormatsUtil.isBitcoinUri(envSettings.bitcoinNetworkParameters, scanData) -> {
//                onCurrencySelected(CryptoCurrency.BTC)
//                address = FormatsUtil.getBitcoinAddress(scanData)
//
//                val amount: String = FormatsUtil.getBitcoinAmount(scanData)
//                val paymentRequestUrl = FormatsUtil.getPaymentRequestUrl(scanData)
//
//                if (address.isEmpty() && scanData.isBitpayAddress()) {
//                    // get payment protocol request data from bitpay
//                    val invoiceId = paymentRequestUrl.replace(bitpayInvoiceUrl, "")
//                    if (isDeepLinked) {
//                        analytics.logEvent(
//                            BitPayEvent.InputEvent(
//                                AnalyticsEvents.BitpayUrlDeeplink.event,
//                                CryptoCurrency.BTC))
//                    } else {
//                        analytics.logEvent(
//                            BitPayEvent.InputEvent(
//                                AnalyticsEvents.BitpayAdrressScanned.event,
//                                CryptoCurrency.BTC))
//                    }
//                    handleBitPayInvoice(invoiceId)
//                } else {
//                    // Convert to correct units
//                    try {
//                        val cryptoValue = CryptoValue(selectedCrypto, amount.toBigInteger())
//                        val fiatValue = cryptoValue.toFiat(exchangeRates, prefs.selectedFiatCurrency)
//                        view?.updateCryptoAmount(cryptoValue)
//                        view?.updateFiatAmount(fiatValue)
//                    } catch (e: Exception) {
//                        // ignore
//                    }
//                }
//            }
//            FormatsUtil.isValidBitcoinAddress(envSettings.bitcoinNetworkParameters, scanData) -> {
//                address = if (selectedCrypto == CryptoCurrency.BTC) {
//                    onCurrencySelected(CryptoCurrency.BTC)
//                    scanData
//                } else {
//                    onCurrencySelected(CryptoCurrency.BCH)
//                    scanData
//                }
//            }
//            FormatsUtil.isValidEthereumAddress(scanData) -> {
//                when (selectedCrypto) {
//                    CryptoCurrency.ETHER -> onCurrencySelected(CryptoCurrency.ETHER)
//                    CryptoCurrency.PAX -> onCurrencySelected(CryptoCurrency.PAX)
//                    else -> {
//                        if (defaultCurrency in listOf(CryptoCurrency.ETHER, CryptoCurrency.PAX)) {
//                            onCurrencySelected(defaultCurrency)
//                        } else {
//                            onCurrencySelected(CryptoCurrency.ETHER) // Default to ETH
//                        }
//                    }
//                }
//
//                address = scanData
//                view?.updateCryptoAmount(CryptoValue.zero(selectedCrypto))
//            }
//            else -> {
//                onCurrencySelected(CryptoCurrency.BTC)
//                view?.showSnackbar(R.string.invalid_address, Snackbar.LENGTH_LONG)
//                return
//            }
//        }
//    }
//
//    if (address != "") {
//        delegate.processURIScanAddress(address)
//    }
// }
//
// private fun handleBitPayInvoice(invoiceId: String) {
//    compositeDisposable += bitpayDataManager.getRawPaymentRequest(invoiceId = invoiceId)
//        .doOnSuccess {
//            val cryptoValue = CryptoValue(selectedCrypto, it.instructions[0].outputs[0].amount)
//            val merchant = it.memo.split(merchantPattern)[1]
//            val bitpayProtocol: BitPayProtocol? = delegate as? BitPayProtocol ?: return@doOnSuccess
//
//            bitpayProtocol?.setbitpayReceivingAddress(it.instructions[0].outputs[0].address)
//            bitpayProtocol?.setbitpayMerchant(merchant)
//            bitpayProtocol?.setInvoiceId(invoiceId)
//            bitpayProtocol?.setIsBitpayPaymentRequest(true)
//            view?.let { view ->
//                view.disableInput()
//                view.showBitPayTimerAndMerchantInfo(it.expires, merchant)
//                view.updateCryptoAmount(cryptoValue)
//                view.updateReceivingAddress("bitcoin:?r=" + it.paymentUrl)
//                view.setFeePrioritySelection(1)
//                view.disableFeeDropdown()
//                view.onBitPayAddressScanned()
//            }
//        }.doOnError {
//            Timber.e(it)
//        }.subscribe()
// }
//
// private fun String.isBitpayAddress(): Boolean {
//
//    val amount = FormatsUtil.getBitcoinAmount(this)
//    val paymentRequestUrl = FormatsUtil.getPaymentRequestUrl(this)
//    return amount == "0.0000" &&
//        paymentRequestUrl.contains(bitpayInvoiceUrl)
// }
//
// private fun requestScan() {
//
//   val fragment = currentFragment::class.simpleName ?: "unknown"
//
//    analytics.logEvent(object : AnalyticsEvent {
//        override val event = "qr_scan_requested"
//        override val params = mapOf("fragment" to fragment)
//    })
//
//    val deniedPermissionListener = SnackbarOnDeniedPermissionListener.Builder
//        .with(coordinator_layout, R.string.request_camera_permission)
//        .withButton(android.R.string.ok) { requestScan() }
//        .build()
//
//    val grantedPermissionListener = CameraPermissionListener(analytics, {
//        startScanActivity()
//    })
//
//    val compositePermissionListener =
//        CompositePermissionListener(deniedPermissionListener, grantedPermissionListener)
//
//    Dexter.withActivity(this)
//        .withPermission(Manifest.permission.CAMERA)
//        .withListener(compositePermissionListener)
//        .withErrorListener { error -> Timber.wtf("Dexter permissions error $error") }
//        .check()
// }
