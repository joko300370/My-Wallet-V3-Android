package piuk.blockchain.android.ui.swapold.exchange

import androidx.lifecycle.ViewModel
import com.blockchain.accounts.AsyncAllAccountList
import com.blockchain.datamanagers.MaximumSpendableCalculator
import com.blockchain.datamanagers.TransactionExecutorWithoutFees
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.common.exchange.mvi.EnoughFeesLimit
import com.blockchain.swap.common.exchange.mvi.ExchangeDialog
import com.blockchain.swap.common.exchange.mvi.ExchangeIntent
import com.blockchain.swap.common.exchange.mvi.ExchangeRateIntent
import com.blockchain.swap.common.exchange.mvi.ExchangeViewState
import com.blockchain.swap.common.exchange.mvi.FiatExchangeRateIntent
import com.blockchain.swap.common.exchange.mvi.IsUserEligiableForFreeEthIntent
import com.blockchain.swap.common.exchange.mvi.SetEthTransactionInFlight
import com.blockchain.swap.common.exchange.mvi.SetFixIntent
import com.blockchain.swap.common.exchange.mvi.SetTierLimit
import com.blockchain.swap.common.exchange.mvi.SetTradeLimits
import com.blockchain.swap.common.exchange.mvi.SetUserTier
import com.blockchain.swap.common.exchange.mvi.SpendableValueIntent
import com.blockchain.swap.common.exchange.mvi.allQuoteClearingConditions
import com.blockchain.swap.common.exchange.mvi.initial
import com.blockchain.swap.common.exchange.mvi.toIntent
import com.blockchain.swap.common.exchange.service.QuoteService
import com.blockchain.swap.common.exchange.service.QuoteServiceFactory
import com.blockchain.swap.common.quote.ExchangeQuoteRequest
import com.blockchain.swap.nabu.CurrentTier
import com.blockchain.swap.nabu.EthEligibility
import com.blockchain.swap.nabu.service.Fix
import com.blockchain.swap.nabu.service.Quote
import com.blockchain.swap.nabu.service.TradeLimitService
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.ReplaySubject
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.datastore.ExchangeRateDataStore
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class ExchangeModel(
    quoteServiceFactory: QuoteServiceFactory,
    private val allAccountList: AsyncAllAccountList,
    private val tradeLimitService: TradeLimitService,
    private val currentTier: CurrentTier,
    private val ethEligibility: EthEligibility,
    private val exchangeRateDataStore: ExchangeRateDataStore,
    private val transactionExecutor: TransactionExecutorWithoutFees,
    private val maximumSpendableCalculator: MaximumSpendableCalculator,
    private val ethDataManager: EthDataManager,
    private val currencyPrefs: CurrencyPrefs
) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    private val dialogDisposable = CompositeDisposable()

    private val maxSpendableDisposable = CompositeDisposable()

    private var preselectedToCryptoCurrency = CryptoCurrency.ETHER
    private var preselectedFromCryptoCurrency = CryptoCurrency.BTC

    val quoteService: QuoteService by lazy {
        quoteServiceFactory.createQuoteService()
            .also { initDialog(it) }
    }

    private val exchangeViewModelsSubject = BehaviorSubject.create<ExchangeViewState>()

    val inputEventSink = ReplaySubject.create<ExchangeIntent>()

    val exchangeViewStates: Observable<ExchangeViewState> = exchangeViewModelsSubject

    private var accountThatHasCalculatedSpendable = AtomicReference<AccountReference?>()

    fun initWithPreselectedToCurrency(cryptoCurrency: CryptoCurrency) {
        preselectedToCryptoCurrency = cryptoCurrency
    }

    fun initWithPreselectedFromCurrency(cryptoCurrency: CryptoCurrency) {
        preselectedFromCryptoCurrency = cryptoCurrency
    }

    override fun onCleared() {
        super.onCleared()
        dialogDisposable.clear()
        compositeDisposable.clear()
        Timber.d("ExchangeViewModel cleared")
    }

    private fun initDialog(quoteService: QuoteService) {
        val fiatCurrency = currencyPrefs.selectedFiatCurrency

        compositeDisposable += Singles.zip(
            allAccountList[preselectedFromCryptoCurrency].flatMap { it.defaultAccount() },
            allAccountList[preselectedToCryptoCurrency].flatMap { it.defaultAccount() }
        ).subscribeBy(
            onSuccess = { (fromAccount, toAccount) ->
                newDialog(
                    fiatCurrency,
                    quoteService,
                    ExchangeDialog(
                        Observable.merge(
                            inputEventSink,
                            quoteService.quotes.map(Quote::toIntent)
                        ),
                        initial(
                            fiatCurrency,
                            fromAccount,
                            toAccount
                        )
                    )
                )
            },
            onError = { }
        )
    }

    private fun newDialog(
        fiatCurrency: String,
        quoteService: QuoteService,
        exchangeDialog: ExchangeDialog
    ) {
        dialogDisposable.clear()
        dialogDisposable += quoteService.rates.subscribeBy {
            Timber.d("RawExchangeRate: $it")
            when (it) {
                is ExchangeRate.CryptoToFiat -> inputEventSink.onNext(
                    FiatExchangeRateIntent(
                        it
                    )
                )
            }
        }
        dialogDisposable += tradeLimitService.getTradesLimits(fiatCurrency)
            .subscribeBy {
                inputEventSink.onNext(
                    SetTradeLimits(
                        it.minOrder,
                        it.maxOrder
                    )
                )
            }
        dialogDisposable += Observable.interval(1, TimeUnit.MINUTES)
            .startWith(0L)
            .flatMapSingle {
                tradeLimitService.getTradesLimits(fiatCurrency)
            }
            .subscribeBy {
                inputEventSink.onNext(SetTierLimit(it.minAvailable()))
            }
        dialogDisposable += Observable.interval(1, TimeUnit.MINUTES)
            .startWith(0L)
            .flatMapSingle {
                currentTier.usersCurrentTier()
            }
            .subscribeBy {
                inputEventSink.onNext(SetUserTier(it))
            }

        dialogDisposable += Observable.interval(1, TimeUnit.MINUTES)
            .startWith(0L)
            .flatMapSingle {
                ethDataManager.isLastTxPending()
            }
            .subscribeBy {
                inputEventSink.onNext(
                    SetEthTransactionInFlight(
                        it
                    )
                )
            }

        dialogDisposable += ethEligibility.isEligible().subscribeBy {
            inputEventSink.onNext(
                IsUserEligiableForFreeEthIntent(
                    it
                )
            )
        }

        dialogDisposable += exchangeRateDataStore.updateExchangeRates().subscribeBy {
                inputEventSink.onNext(
                    ExchangeRateIntent(
                        CryptoCurrency.activeCurrencies().map {
                            ExchangeRate.CryptoToFiat(
                                it,
                                currencyPrefs.selectedFiatCurrency,
                                exchangeRateDataStore.getLastPrice(
                                    it,
                                    currencyPrefs.selectedFiatCurrency
                                ).toBigDecimal()
                            )
                        }
                    )
                )
            }

        dialogDisposable += exchangeDialog.viewStates.distinctUntilChanged()
            .doOnError { Timber.e(it) }
            .subscribeBy {
                newViewModel(it)
            }

        dialogDisposable += exchangeViewStates
            .subscribeBy {
                quoteService.updateQuoteRequest(it.toExchangeQuoteRequest(it.fromFiat.currencyCode))

                updateMaxSpendable(it.fromAccount)
            }

        val currency =
            exchangeViewStates.map {
                it.fromCrypto.currency
            }.distinctUntilChanged()

        dialogDisposable += currency.withLatestFrom(
            exchangeViewStates.map {
                it.fromCrypto to it.fromAccount
            }
        ).flatMapSingle { (_, cryptoAccountPair) ->
            transactionExecutor.hasEnoughEthFeesForTheTransaction(cryptoAccountPair.first, cryptoAccountPair.second)
        }.subscribeBy {
            inputEventSink.onNext(EnoughFeesLimit(it))
        }

        dialogDisposable += exchangeViewStates.allQuoteClearingConditions()
            .subscribeBy {
                inputEventSink.onNext(it)
            }
    }

    private fun updateMaxSpendable(account: AccountReference) {
        if (accountThatHasCalculatedSpendable.getAndSet(account) == account) return
        Timber.d("Updating max spendable for $account")
        maxSpendableDisposable.clear()
        maxSpendableDisposable += maximumSpendableCalculator.getMaximumSpendable(account)
            .subscribeBy {
                Timber.d("Max spendable is $it")
                inputEventSink.onNext(
                    SpendableValueIntent(
                        it
                    )
                )
            }
    }

    private fun newViewModel(exchangeViewModel: ExchangeViewState) {
        exchangeViewModelsSubject.onNext(exchangeViewModel)
    }

    fun fixAsFiat() {
        inputEventSink.onNext(SetFixIntent(Fix.BASE_FIAT))
    }

    fun fixAsCrypto() {
        inputEventSink.onNext(SetFixIntent(Fix.BASE_CRYPTO))
    }
}

private fun ExchangeViewState.toExchangeQuoteRequest(
    currency: String
): ExchangeQuoteRequest = when (fix) {
    Fix.COUNTER_FIAT ->
        ExchangeQuoteRequest.BuyingFiatLinked(
            offering = fromCrypto.currency,
            wanted = toCrypto.currency,
            wantedFiatValue = lastUserValue as FiatValue
        )
    Fix.BASE_FIAT ->
        ExchangeQuoteRequest.SellingFiatLinked(
            offering = fromCrypto.currency,
            wanted = toCrypto.currency,
            offeringFiatValue = lastUserValue as FiatValue
        )
    Fix.COUNTER_CRYPTO ->
        ExchangeQuoteRequest.Buying(
            offering = fromCrypto.currency,
            wanted = lastUserValue as CryptoValue,
            indicativeFiatSymbol = currency
        )
    Fix.BASE_CRYPTO ->
        ExchangeQuoteRequest.Selling(
            offering = lastUserValue as CryptoValue,
            wanted = toCrypto.currency,
            indicativeFiatSymbol = currency
        )
}

interface ExchangeViewModelProvider {
    val exchangeViewModel: ExchangeModel
}

interface ExchangeLimitState {

    fun setOverTierLimit(overLimit: Boolean)
}

interface ExchangeMenuState {
    sealed class ExchangeMenu {
        data class Error(val error: ExchangeMenuError) : ExchangeMenu()

        object Help : ExchangeMenu()
    }

    data class ExchangeMenuError(
        val fromCrypto: CryptoCurrency,
        val tier: Int,
        val title: CharSequence,
        val message: CharSequence,
        val errorType: ErrorType
    )

    enum class ErrorType {
        TRADE, TIER, BALANCE, TRANSACTION_STATE
    }

    fun setMenuState(state: ExchangeMenu)
}
