package piuk.blockchain.android.ui.transfer.send.activity

import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.amshove.kluent.any
import org.amshove.kluent.itReturns
import org.amshove.kluent.mock
import org.bitcoinj.params.BitcoinCashMainNetParams
import org.bitcoinj.params.BitcoinMainNetParams
import org.junit.Test
import piuk.blockchain.android.coincore.btc.BtcCryptoWalletAccount
import piuk.blockchain.android.data.api.bitpay.models.BitPayPaymentRequestOutput
import piuk.blockchain.android.data.api.bitpay.models.BitPaymentInstructions
import piuk.blockchain.android.data.api.bitpay.models.RawPaymentRequest
import piuk.blockchain.android.data.api.bitpay.models.events.BitPayEvent
import piuk.blockchain.android.ui.transfer.send.activity.strategy.BitcoinSendStrategy
import piuk.blockchain.android.ui.transfer.send.activity.strategy.SendStrategy
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

class SendPresenterTest {

    private val analytics: Analytics = mock()

    @Test
    fun `handles btc address scan, delegates to btc strategy`() {
        val view: SendView = mock()
        val btcStrategy: SendStrategy<SendView> = mock()
        val exchangeRateFactory = mock<ExchangeRateDataManager> {
            on { updateTickers() } `it returns` Completable.complete()
        }
        val envSettings = mock<EnvironmentConfig> {
            on { bitcoinCashNetworkParameters } `it returns` BitcoinCashMainNetParams()
            on { bitcoinNetworkParameters } `it returns` BitcoinMainNetParams()
        }

        SendPresenter(
            btcStrategy = btcStrategy,
            bchStrategy = mock(),
            xlmStrategy = mock(),
            prefs = mock(),
            exchangeRates = mock(),
            stringUtils = mock(),
            envSettings = envSettings,
            exchangeRateFactory = exchangeRateFactory,
            pitLinkingFeatureFlag = mock(),
            bitpayDataManager = mock(),
            analytics = analytics
        ).apply {
            attachView(view)
            handlePredefinedInput("1FBPzxps6kGyk2exqLvz7cRMi2odtLEVQ", CryptoCurrency.BTC, false)
        }

        verify(btcStrategy).processURIScanAddress("1FBPzxps6kGyk2exqLvz7cRMi2odtLEVQ")
    }

    @Test
    fun `handles bitpay paypro scan`() {
        val view: SendView = mock()
        val btcStrategy: BitcoinSendStrategy = mock()
        val exchangeRateFactory = mock<ExchangeRateDataManager> {
            on { updateTickers() } `it returns` Completable.complete()
        }
        val envSettings = mock<EnvironmentConfig> {
            on { bitcoinCashNetworkParameters } `it returns` BitcoinCashMainNetParams()
            on { bitcoinNetworkParameters } `it returns` BitcoinMainNetParams()
        }

        val invoiceId = "Mjo9YLKe2pK31uH7vzD1p7"

        val bitpayBitcoinURI = "bitcoin:?r=https://bitpay.com/i/Mjo9YLKe2pK31uH7vzD1p7"

        val memo = "Payment request for BitPay invoice Mjo9YLKe2pK31uH7vzD1p7 for merchant Satoshi"

        val paymentUrl = "https://bitpay.com/i/Mjo9YLKe2pK31uH7vzD1p7"

        val output = BitPayPaymentRequestOutput(2.toBigInteger(), "1HLoD9E4SDFFPDiYfNYnkBLQ85Y51J3Zb1")

        val outputs = mutableListOf(output)

        val instructions = listOf(BitPaymentInstructions(outputs))

        val cryptoValue = CryptoValue(CryptoCurrency.BTC, output.amount)

        val paymentRequest = RawPaymentRequest(instructions, memo, "2019-08-31T04:49:19Z", paymentUrl)

        SendPresenter(
            btcStrategy = btcStrategy,
            bchStrategy = mock(),
            xlmStrategy = mock(),
            prefs = mock(),
            exchangeRates = mock(),
            stringUtils = mock(),
            envSettings = envSettings,
            exchangeRateFactory = exchangeRateFactory,
            pitLinkingFeatureFlag = mock(),
            bitpayDataManager = mock {
                on { getRawPaymentRequest(invoiceId) } `it returns` Single.just(paymentRequest)
            },
            analytics = analytics
        ).apply {
            attachView(view)
            handlePredefinedInput(bitpayBitcoinURI, CryptoCurrency.BTC, false)
        }

        verify(view).disableInput()
        verify(view).onBitPayAddressScanned()
        verify(view).showBitPayTimerAndMerchantInfo(paymentRequest.expires, "Satoshi")
        verify(view).updateCryptoAmount(cryptoValue)
        verify(view).updateReceivingAddress(bitpayBitcoinURI)
        verify(view).setFeePrioritySelection(1)
        verify(view).disableFeeDropdown()
        verify(analytics).logEvent(BitPayEvent.InputEvent(AnalyticsEvents.BitpayAdrressScanned.event,
            CryptoCurrency.BTC))
    }

    @Test
    fun `handles bitpay paypro deeplinked`() {
        val view: SendView = mock()
        val btcStrategy: BitcoinSendStrategy = mock()
        val exchangeRateFactory = mock<ExchangeRateDataManager> {
            on { updateTickers() } `it returns` Completable.complete()
        }
        val envSettings = mock<EnvironmentConfig> {
            on { bitcoinCashNetworkParameters } `it returns` BitcoinCashMainNetParams()
            on { bitcoinNetworkParameters } `it returns` BitcoinMainNetParams()
        }

        val invoiceId = "Mjo9YLKe2pK31uH7vzD1p7"

        val bitpayBitcoinURI = "bitcoin:?r=https://bitpay.com/i/Mjo9YLKe2pK31uH7vzD1p7"

        val memo = "Payment request for BitPay invoice Mjo9YLKe2pK31uH7vzD1p7 for merchant Satoshi"

        val paymentUrl = "https://bitpay.com/i/Mjo9YLKe2pK31uH7vzD1p7"

        val output = BitPayPaymentRequestOutput(2.toBigInteger(), "1HLoD9E4SDFFPDiYfNYnkBLQ85Y51J3Zb1")

        val outputs = mutableListOf(output)

        val instructions = listOf(BitPaymentInstructions(outputs))

        val cryptoValue = CryptoValue(CryptoCurrency.BTC, output.amount)

        val paymentRequest = RawPaymentRequest(instructions, memo, "2019-08-31T04:49:19Z", paymentUrl)

        SendPresenter(
            btcStrategy = btcStrategy,
            bchStrategy = mock(),
            xlmStrategy = mock(),
            prefs = mock(),
            exchangeRates = mock(),
            stringUtils = mock(),
            envSettings = envSettings,
            exchangeRateFactory = exchangeRateFactory,
            pitLinkingFeatureFlag = mock(),
            bitpayDataManager = mock {
                on { getRawPaymentRequest(invoiceId) } `it returns` Single.just(paymentRequest)
            },
            analytics = analytics
        ).apply {
            attachView(view)
            handlePredefinedInput(bitpayBitcoinURI, CryptoCurrency.BTC, true)
        }

        verify(view).disableInput()
        verify(view).onBitPayAddressScanned()
        verify(view).showBitPayTimerAndMerchantInfo(paymentRequest.expires, "Satoshi")
        verify(view).updateCryptoAmount(cryptoValue)
        verify(view).updateReceivingAddress(bitpayBitcoinURI)
        verify(view).setFeePrioritySelection(1)
        verify(view).disableFeeDropdown()
        verify(analytics).logEvent(BitPayEvent.InputEvent(AnalyticsEvents.BitpayUrlDeeplink.event,
            CryptoCurrency.BTC))
    }

    @Test
    fun `handles bitpay address entered`() {
        val view: SendView = mock()
        val btcStrategy: BitcoinSendStrategy = mock()

        val btcAccount: BtcCryptoWalletAccount = mock {
            on { asset } itReturns(CryptoCurrency.BTC)
        }

        val exchangeRateFactory = mock<ExchangeRateDataManager> {
            on { updateTickers() } `it returns` Completable.complete()
        }
        val envSettings = mock<EnvironmentConfig> {
            on { bitcoinCashNetworkParameters } `it returns` BitcoinCashMainNetParams()
            on { bitcoinNetworkParameters } `it returns` BitcoinMainNetParams()
        }

        val invoiceId = "Mjo9YLKe2pK31uH7vzD1p7"

        val bitpayBitcoinURI = "bitcoin:?r=https://bitpay.com/i/Mjo9YLKe2pK31uH7vzD1p7"

        val memo = "Payment request for BitPay invoice Mjo9YLKe2pK31uH7vzD1p7 for merchant Satoshi"

        val paymentUrl = "https://bitpay.com/i/Mjo9YLKe2pK31uH7vzD1p7"

        val output = BitPayPaymentRequestOutput(2.toBigInteger(), "1HLoD9E4SDFFPDiYfNYnkBLQ85Y51J3Zb1")

        val outputs = mutableListOf(output)

        val instructions = listOf(BitPaymentInstructions(outputs))

        val cryptoValue = CryptoValue(CryptoCurrency.BTC, output.amount)

        val paymentRequest = RawPaymentRequest(instructions, memo, "2019-08-31T04:49:19Z", paymentUrl)

        SendPresenter(
            btcStrategy = btcStrategy,
            bchStrategy = mock(),
            xlmStrategy = mock(),
            prefs = mock(),
            exchangeRates = mock(),
            stringUtils = mock(),
            envSettings = envSettings,
            exchangeRateFactory = exchangeRateFactory,
            pitLinkingFeatureFlag = mock(),
            bitpayDataManager = mock {
                on { getRawPaymentRequest(invoiceId) } `it returns` Single.just(paymentRequest)
            },
            analytics = analytics
        ).apply {
            attachView(view)
            setSourceAccount(btcAccount)
            onAddressTextChange(bitpayBitcoinURI)
        }

        verify(view).disableInput()
        verify(view).showBitPayTimerAndMerchantInfo(paymentRequest.expires, "Satoshi")
        verify(view).updateCryptoAmount(cryptoValue)
        verify(view).updateReceivingAddress(bitpayBitcoinURI)
        verify(view).setFeePrioritySelection(1)
        verify(view).disableFeeDropdown()
        verify(analytics).logEvent(BitPayEvent.InputEvent(AnalyticsEvents.BitpayUrlPasted.event,
            CryptoCurrency.BTC))
    }

    @Test
    fun `handles broken address scan, doesn't delegate, defaults to BTC`() {
        val view: SendView = mock()
        val btcStrategy: SendStrategy<SendView> = mock()
        val bchStrategy: SendStrategy<SendView> = mock()
        val xlmStrategy: SendStrategy<SendView> = mock()

        val exchangeRateFactory = mock<ExchangeRateDataManager> {
            on { updateTickers() } `it returns` Completable.complete()
        }

        val envSettings = mock<EnvironmentConfig> {
            on { bitcoinCashNetworkParameters } `it returns` BitcoinCashMainNetParams()
            on { bitcoinNetworkParameters } `it returns` BitcoinMainNetParams()
        }

        SendPresenter(
            btcStrategy = btcStrategy,
            bchStrategy = bchStrategy,
            xlmStrategy = xlmStrategy,
            prefs = mock(),
            exchangeRates = mock(),
            stringUtils = mock(),
            envSettings = envSettings,
            exchangeRateFactory = exchangeRateFactory,
            pitLinkingFeatureFlag = mock(),
            bitpayDataManager = mock(),
            analytics = analytics
        ).apply {
            attachView(view)
            handlePredefinedInput("nope_nope_nope", CryptoCurrency.BTC, false)
        }

        verify(btcStrategy, never()).processURIScanAddress(any())
        verify(bchStrategy, never()).processURIScanAddress(any())
        verify(xlmStrategy, never()).processURIScanAddress(any())
        verify(analytics, never()).logEvent(any())
    }

    @Test
    fun `memo required should start with false and then get the strategy exposed value`() {
        val btcStrategy: SendStrategy<SendView> = mock()
        whenever(btcStrategy.memoRequired()).thenReturn(Observable.just(true))
        val view: SendView = mock()

        SendPresenter(
            btcStrategy = btcStrategy,
            bchStrategy = mock(),
            xlmStrategy = mock(),
            prefs = mock(),
            exchangeRates = mock(),
            stringUtils = mock(),
            envSettings = mock(),
            exchangeRateFactory = mock {
                on { updateTickers() } `it returns` Completable.complete()
            },
            pitLinkingFeatureFlag = mock {
                on { enabled } `it returns` Single.just(true)
            },
            bitpayDataManager = mock(),
            analytics = analytics
        ).apply {
            attachView(view)
            onViewReady()
        }
        verify(view).updateRequiredLabelVisibility(false)
        verify(view).updateRequiredLabelVisibility(true)
    }

    @Test
    fun `when pit is enabled the correct value should propagated to the view`() {
        val btcStrategy: SendStrategy<SendView> = mock()
        whenever(btcStrategy.memoRequired()).thenReturn(Observable.just(true))
        val view: SendView = mock()

        SendPresenter(
            btcStrategy = btcStrategy,
            bchStrategy = mock(),
            xlmStrategy = mock(),
            prefs = mock(),
            exchangeRates = mock(),
            stringUtils = mock(),
            envSettings = mock(),
            exchangeRateFactory = mock {
                on { updateTickers() } `it returns` Completable.complete()
            },
            pitLinkingFeatureFlag = mock {
                on { enabled } `it returns` Single.just(true)
            },
            bitpayDataManager = mock(),
            analytics = analytics
        ).apply {
            attachView(view)
            onViewReady()
        }
        verify(view).isPitEnabled(true)
    }

    @Test
    fun `when pit is disabled the correct value should propagated to the view`() {
        val btcStrategy: SendStrategy<SendView> = mock()
        whenever(btcStrategy.memoRequired()).thenReturn(Observable.just(true))
        val view: SendView = mock()

        SendPresenter(
            btcStrategy = btcStrategy,
            bchStrategy = mock(),
            xlmStrategy = mock(),
            prefs = mock(),
            exchangeRates = mock(),
            stringUtils = mock(),
            envSettings = mock(),
            exchangeRateFactory = mock {
                on { updateTickers() } `it returns` Completable.complete()
            },
            pitLinkingFeatureFlag = mock {
                on { enabled } `it returns` Single.just(false)
            },
            bitpayDataManager = mock(),
            analytics = analytics
        ).apply {
            attachView(view)
            onViewReady()
        }
        verify(view).isPitEnabled(false)
    }
}
