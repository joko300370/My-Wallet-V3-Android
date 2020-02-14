package piuk.blockchain.android.simplebuy

import com.blockchain.preferences.SimpleBuyPrefs
import com.google.gson.Gson
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.ui.base.mvi.MviModel

class SimpleBuyModel(
    private val prefs: SimpleBuyPrefs,
    initialState: SimpleBuyState,
    scheduler: Scheduler,
    private val gson: Gson,
    private val interactor: SimpleBuyInteractor
) : MviModel<SimpleBuyState, SimpleBuyIntent>(
    gson.fromJson(prefs.simpleBuyState(), SimpleBuyState::class.java) ?: initialState,
    scheduler) {

    override fun performAction(previousState: SimpleBuyState, intent: SimpleBuyIntent): Disposable? =
        when (intent) {
            is SimpleBuyIntent.FetchBuyLimits -> interactor.fetchBuyLimitsAndSupportedCryptoCurrencies(intent.currency)
                .subscribeBy(
                    onSuccess = { process(it) },
                    onError = { process(SimpleBuyIntent.ErrorIntent()) }
                )
            is SimpleBuyIntent.FetchPredefinedAmounts -> interactor.fetchPredefinedAmounts(intent.currency).subscribeBy(
                onSuccess = { process(it) },
                onError = { process(SimpleBuyIntent.ErrorIntent()) }
            )
            is SimpleBuyIntent.CancelOrder -> interactor.cancelOrder().subscribeBy(
                onSuccess = { process(it) },
                onError = { process(SimpleBuyIntent.ErrorIntent()) }
            )
            is SimpleBuyIntent.ConfirmOrder -> interactor.createOrder(
                previousState.selectedCryptoCurrency,
                previousState.order.amount
            ).subscribeBy(
                onSuccess = { process(it) },
                onError = { process(SimpleBuyIntent.ErrorIntent()) }
            )
            is SimpleBuyIntent.FetchBankAccount -> {
                if (previousState.bankAccount != null) {
                    process(SimpleBuyIntent.BankAccountUpdated(previousState.bankAccount))
                    null
                } else {
                    interactor.fetchBankAccount(previousState.currency).subscribeBy(
                        onSuccess = { process(it) },
                        onError = { process(SimpleBuyIntent.ErrorIntent()) })
                }
            }
            is SimpleBuyIntent.FetchKycState -> interactor.pollForKycState().subscribeBy(
                onSuccess = { process(it) },
                onError = { /*never fails. will return SimpleBuyIntent.KycStateUpdated(KycState.FAILED)*/ }
            )
            is SimpleBuyIntent.FetchQuote -> interactor.fetchQuote(
                previousState.selectedCryptoCurrency,
                previousState.order.amount
            ).subscribeBy(
                onSuccess = { process(it) },
                onError = { process(SimpleBuyIntent.ErrorIntent()) }
            )
            is SimpleBuyIntent.BuyButtonClicked -> interactor.checkTierLevel().subscribeBy(
                onSuccess = { process(it) },
                onError = { process(SimpleBuyIntent.ErrorIntent()) }
            )
            is SimpleBuyIntent.KycStateUpdated -> null
            is SimpleBuyIntent.QuoteUpdated -> null
            is SimpleBuyIntent.FlowCurrentScreen -> null
            is SimpleBuyIntent.ClearState -> null
            is SimpleBuyIntent.UpdatedPredefinedAmounts -> null
            is SimpleBuyIntent.ConfirmationHandled -> null
            is SimpleBuyIntent.BankAccountUpdated -> null
            is SimpleBuyIntent.KycCompleted -> null
            is SimpleBuyIntent.KycStarted -> null
            is SimpleBuyIntent.ErrorIntent -> null
            is SimpleBuyIntent.ClearError -> null
            is SimpleBuyIntent.UpdatedBuyLimitsAndSupportedCryptoCurrencies -> null
            is SimpleBuyIntent.NewCryptoCurrencySelected -> null
            is SimpleBuyIntent.EnteredAmount -> null
            is SimpleBuyIntent.OrderCreated -> null
            is SimpleBuyIntent.OrderCanceled -> null
        }

    override fun onStateUpdate(s: SimpleBuyState) {
        prefs.updateSimpleBuyState(gson.toJson(s))
    }
}