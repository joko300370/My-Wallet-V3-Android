package piuk.blockchain.android.ui.send.external

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.bitcoinj.params.BitcoinCashMainNetParams
import org.bitcoinj.params.BitcoinMainNetParams
import org.junit.Test
import piuk.blockchain.android.ui.send.SendView
import piuk.blockchain.android.ui.send.strategy.SendStrategy
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

class PerCurrencySendPresenterTest {

    @Test
    fun `handles xlm address scan, delegates to xlm strategy`() {
        val view: SendView = mock()
        val xlmStrategy: SendStrategy<SendView> = mock()

        val exchangeRateFactory = mock<ExchangeRateDataManager> {
            on { updateTickers() } `it returns` Completable.complete()
        }

        PerCurrencySendPresenter(
            btcStrategy = mock(),
            bchStrategy = mock(),
            etherStrategy = mock(),
            xlmStrategy = xlmStrategy,
            paxStrategy = mock(),
            prefs = mock(),
            exchangeRates = mock(),
            stringUtils = mock(),
            envSettings = mock(),
            exchangeRateFactory = exchangeRateFactory,
            pitLinkingFeatureFlag = mock()
        ).apply {
            initView(view)
            handleURIScan("GDYULVJK2T6G7HFUC76LIBKZEMXPKGINSG6566EPWJKCLXTYVWJ7XPY4", CryptoCurrency.BTC)
        }

        verify(xlmStrategy).processURIScanAddress("GDYULVJK2T6G7HFUC76LIBKZEMXPKGINSG6566EPWJKCLXTYVWJ7XPY4")
        verify(xlmStrategy).onCurrencySelected()
        verify(view).setSelectedCurrency(CryptoCurrency.XLM)
    }

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

        PerCurrencySendPresenter(
            btcStrategy = btcStrategy,
            bchStrategy = mock(),
            etherStrategy = mock(),
            xlmStrategy = mock(),
            paxStrategy = mock(),
            prefs = mock(),
            exchangeRates = mock(),
            stringUtils = mock(),
            envSettings = envSettings,
            exchangeRateFactory = exchangeRateFactory,
            pitLinkingFeatureFlag = mock()
        ).apply {
            initView(view)
            handleURIScan("1FBPzxps6kGyk2exqLvz7cRMi2odtLEVQ", CryptoCurrency.BTC)
        }

        verify(btcStrategy).processURIScanAddress("1FBPzxps6kGyk2exqLvz7cRMi2odtLEVQ")
    }

    @Test
    fun `handles broken address scan, doesn't delegate, defaults to BTC`() {
        val view: SendView = mock()
        val btcStrategy: SendStrategy<SendView> = mock()
        val bchStrategy: SendStrategy<SendView> = mock()
        val etherStrategy: SendStrategy<SendView> = mock()
        val xlmStrategy: SendStrategy<SendView> = mock()
        val erc20Strategy: SendStrategy<SendView> = mock()

        val exchangeRateFactory = mock<ExchangeRateDataManager> {
            on { updateTickers() } `it returns` Completable.complete()
        }

        val envSettings = mock<EnvironmentConfig> {
            on { bitcoinCashNetworkParameters } `it returns` BitcoinCashMainNetParams()
            on { bitcoinNetworkParameters } `it returns` BitcoinMainNetParams()
        }

        PerCurrencySendPresenter(
            btcStrategy = btcStrategy,
            bchStrategy = bchStrategy,
            etherStrategy = etherStrategy,
            xlmStrategy = xlmStrategy,
            paxStrategy = mock(),
            prefs = mock(),
            exchangeRates = mock(),
            stringUtils = mock(),
            envSettings = envSettings,
            exchangeRateFactory = exchangeRateFactory,
            pitLinkingFeatureFlag = mock()
        ).apply {
            initView(view)
            handleURIScan("nope_nope_nope", CryptoCurrency.BTC)
        }

        verify(btcStrategy, never()).processURIScanAddress(any())
        verify(bchStrategy, never()).processURIScanAddress(any())
        verify(etherStrategy, never()).processURIScanAddress(any())
        verify(xlmStrategy, never()).processURIScanAddress(any())
        verify(erc20Strategy, never()).processURIScanAddress(any())
    }

    @Test
    fun `memo required should start with false and then get the strategy exposed value`() {
        val btcStrategy: SendStrategy<SendView> = mock()
        whenever(btcStrategy.memoRequired()).thenReturn(Observable.just(true))
        val view: SendView = mock()

        PerCurrencySendPresenter(
            btcStrategy = btcStrategy,
            bchStrategy = mock(),
            etherStrategy = mock(),
            xlmStrategy = mock(),
            paxStrategy = mock(),
            prefs = mock(),
            exchangeRates = mock(),
            stringUtils = mock(),
            envSettings = mock(),
            exchangeRateFactory = mock {
                on { updateTickers() } `it returns` Completable.complete()
            },
            pitLinkingFeatureFlag = mock {
                on { enabled } `it returns` Single.just(true)
            }
        ).apply {
            initView(view)
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

        PerCurrencySendPresenter(
            btcStrategy = btcStrategy,
            bchStrategy = mock(),
            etherStrategy = mock(),
            xlmStrategy = mock(),
            paxStrategy = mock(),
            prefs = mock(),
            exchangeRates = mock(),
            stringUtils = mock(),
            envSettings = mock(),
            exchangeRateFactory = mock {
                on { updateTickers() } `it returns` Completable.complete()
            },
            pitLinkingFeatureFlag = mock {
                on { enabled } `it returns` Single.just(true)
            }
        ).apply {
            initView(view)
            onViewReady()
        }
        verify(view).isPitEnabled(true)
    }

    @Test
    fun `when pit is disabled the correct value should propagated to the view`() {
        val btcStrategy: SendStrategy<SendView> = mock()
        whenever(btcStrategy.memoRequired()).thenReturn(Observable.just(true))
        val view: SendView = mock()

        PerCurrencySendPresenter(
            btcStrategy = btcStrategy,
            bchStrategy = mock(),
            etherStrategy = mock(),
            xlmStrategy = mock(),
            paxStrategy = mock(),
            prefs = mock(),
            exchangeRates = mock(),
            stringUtils = mock(),
            envSettings = mock(),
            exchangeRateFactory = mock {
                on { updateTickers() } `it returns` Completable.complete()
            },
            pitLinkingFeatureFlag = mock {
                on { enabled } `it returns` Single.just(false)
            }
        ).apply {
            initView(view)
            onViewReady()
        }
        verify(view).isPitEnabled(false)
    }
}
