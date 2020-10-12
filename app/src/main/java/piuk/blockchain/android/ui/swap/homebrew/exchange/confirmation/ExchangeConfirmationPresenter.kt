package piuk.blockchain.android.ui.swap.homebrew.exchange.confirmation

import androidx.annotation.VisibleForTesting
import com.blockchain.datamanagers.TransactionExecutorWithoutFees
import com.blockchain.logging.SwapDiagnostics
import com.blockchain.morph.CoinPair
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.SwapAnalyticsEvents
import com.blockchain.payload.PayloadDecrypt
import com.blockchain.serialization.fromMoshiJson
import com.blockchain.sunriver.Memo
import com.blockchain.sunriver.SendException
import com.blockchain.swap.common.trade.MorphTrade
import com.blockchain.swap.nabu.service.Quote
import com.blockchain.swap.nabu.service.TradeExecutionService
import com.blockchain.swap.nabu.service.TradeTransaction
import com.blockchain.ui.urllinks.URL_BLOCKCHAIN_ORDER_ABOVE_MAX
import com.blockchain.ui.urllinks.URL_BLOCKCHAIN_ORDER_EXPIRED
import com.blockchain.ui.urllinks.URL_BLOCKCHAIN_ORDER_FAILED_BELOW_MIN
import com.blockchain.ui.urllinks.URL_BLOCKCHAIN_ORDER_LIMIT_EXCEED
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.exceptions.TransactionHashApiException
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.customviews.ErrorBottomDialog
import piuk.blockchain.android.ui.swap.homebrew.exchange.model.SwapErrorDialogContent
import piuk.blockchain.android.ui.swap.homebrew.exchange.model.SwapErrorResponse
import piuk.blockchain.android.ui.swap.homebrew.exchange.model.SwapErrorType
import piuk.blockchain.android.ui.swap.homebrew.exchange.model.Trade
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.ethereum.exceptions.TransactionInProgressException
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import retrofit2.HttpException
import timber.log.Timber

class ExchangeConfirmationPresenter internal constructor(
    private val transactionExecutor: TransactionExecutorWithoutFees,
    private val tradeExecutionService: TradeExecutionService,
    private val payloadDecrypt: PayloadDecrypt,
    private val stringUtils: StringUtils,
    private val analytics: Analytics,
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val diagnostics: SwapDiagnostics
) : BasePresenter<ExchangeConfirmationView>() {

    private var showPaxAirdropBottomDialog: Boolean = false
    private var minSpendableFiatValue: FiatValue? = null
    private var maxSpendableFiatValue: FiatValue? = null
    private var maxSpendable: CryptoValue? = null
    private var executeTradeSingle: Single<TradeTransaction>? = null

    override fun onViewReady() {
        // Ensure user hasn't got a double encrypted wallet
        subscribeToViewState()
        diagnostics.log("confirm screen enter")
    }

    override fun onViewDestroyed() {
        diagnostics.log("confirm screen destroy")
        super.onViewDestroyed()
    }

    private fun subscribeToViewState() {
        compositeDisposable +=
            view.exchangeViewState
                .flatMapSingle { state ->

                    // State is latest value of behaviour subject.
                    // NOT the state when confirm displayed
                    maxSpendableFiatValue = state.maxTradeLimit
                    minSpendableFiatValue = state.minTradeLimit
                    maxSpendable = state.maxSpendable

                    diagnostics.logMaxSpendable(maxSpendable)

                    showPaxAirdropBottomDialog = state.isPowerPaxTagged
                    if (!payloadDecrypt.isDoubleEncrypted) {
                        executeTrade(state.latestQuote!!, state.fromAccount, state.toAccount)
                    } else {
                        view.showSecondPasswordDialog()
                        executeTradeSingle = executeTrade(state.latestQuote!!, state.fromAccount, state.toAccount)
                        Single.never()
                    }
                }
                .retry()
                .subscribeBy(onError = { Timber.e(it) })
    }

    internal fun updateFee(
        amount: CryptoValue,
        sendingAccount: AccountReference
    ) {
        compositeDisposable +=
            transactionExecutor.getFeeForTransaction(amount, sendingAccount)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        // This fee is never used in a calculation, just displayed.
                        view.updateFee(it)
                    },
                    onError = {
                        Timber.e(it)
                        view.showToast(R.string.homebrew_confirmation_error_fetching_fee, ToastCustom.TYPE_ERROR)
                    }
                )
    }

    private fun executeTrade(
        quote: Quote,
        sendingAccount: AccountReference,
        receivingAccount: AccountReference
    ): Single<TradeTransaction> {

        return deriveAddressPair(sendingAccount, receivingAccount)
            .subscribeOn(Schedulers.io())
            .flatMap { (destination, refund) ->
                tradeExecutionService.executeTrade(quote, destination, refund)
                    .subscribeOn(Schedulers.io())
                    .flatMap { transaction ->
                        diagnostics.log("Sending funds for swap")
                        diagnostics.logQuote(quote)
                        sendFundsForTrade(transaction, sendingAccount)
                            .subscribeOn(Schedulers.io())
                    }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view.showProgressDialog() }
            .doOnEvent { _, _ -> view.dismissProgressDialog() }
            .doOnError { onExecuteTradeFailed(it) }
            .doOnSuccess {
                view.onTradeSubmitted(it.toTrade(),
                    showPaxAirdropBottomDialog && receivingAccount.cryptoCurrency == CryptoCurrency.PAX
                )
                analytics.logEvent(SwapAnalyticsEvents.SwapSummaryConfirmSuccess)
            }
    }

    private fun onExecuteTradeFailed(t: Throwable) {
        Timber.e(t)
        if (t is TransactionInProgressException) {
            view.displayErrorBottomDialog(
                SwapErrorType.CONFIRMATION_ETH_PENDING.toContent(
                    minSpendableFiatValue,
                    maxSpendableFiatValue
                )
            )
        } else {
            val rawError = (t as? HttpException)?.response()?.errorBody()?.string()
            rawError?.takeIf { !it.isBlank() }?.let { error ->
                val swapErrorResponse = SwapErrorResponse::class.fromMoshiJson(error)
                val errorType = swapErrorResponse.toErrorType()
                view.displayErrorBottomDialog(
                    errorType.toContent(
                        minSpendableFiatValue,
                        maxSpendableFiatValue
                    )
                )
            } ?: view.displayErrorBottomDialog(
                SwapErrorType.UNKNOWN.toContent(
                    minSpendableFiatValue,
                    maxSpendableFiatValue
                )
            )
        }
        analytics.logEvent(SwapAnalyticsEvents.SwapSummaryConfirmFailure)
    }

    private fun sendFundsForTrade(
        transaction: TradeTransaction,
        sendingAccount: AccountReference
    ): Single<TradeTransaction> = transactionExecutor.executeTransaction(
            transaction.deposit,
            transaction.depositAddress,
            sendingAccount,
            memo = transaction.memo(),
            diagnostics = diagnostics
        ).flatMap { Single.just(transaction) }
            .onErrorResumeNext { onSendFundsFailed(transaction, it) }
            .doOnSuccess { onSendFundsSucceeded(it) }

    private fun onSendFundsSucceeded(transaction: TradeTransaction) {
        triggerPaxTradeEvent(transaction)
        diagnostics.logSuccess(transaction.hashOut)
    }

    private fun onSendFundsFailed(transaction: TradeTransaction, e: Throwable): Single<TradeTransaction> {
        Timber.e(e, "Transaction execution error, telling nabu")
        analytics.logEvent(AnalyticsEvents.ExchangeExecutionError)
        val hash = (e as? TransactionHashApiException)?.hashString ?: (e as? SendException)?.hash

        diagnostics.logFailure(hash, e.message)

        return tradeExecutionService.putTradeFailureReason(transaction, hash, e.message)
            .andThen(Single.error(e))
    }

    private fun triggerPaxTradeEvent(transaction: TradeTransaction) {
        if (transaction.isPaxTrade()) {
            analytics.logEvent(PaxTradeEvent())
        }
    }

    private fun TradeTransaction.memo() = depositTextMemo?.let { Memo(value = it, type = "text") }

    private fun deriveAddressPair(
        sendingAccount: AccountReference,
        receivingAccount: AccountReference
    ): Single<Pair<String, String>> = Singles.zip(
        transactionExecutor.getReceiveAddress(
            receivingAccount
        ),
        transactionExecutor.getReceiveAddress(
            sendingAccount
        )
    )

    internal fun onSecondPasswordValidated(validatedSecondPassword: String) {
        compositeDisposable +=
            decryptPayload(validatedSecondPassword)
                .andThen(decryptBch())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .andThen(executeTradeSingle?.ignoreElement())
                .doOnSubscribe { view.showProgressDialog() }
                .doOnTerminate { view.dismissProgressDialog() }
                .doOnError { Timber.e(it) }
                .subscribe()
    }

    private fun decryptPayload(validatedSecondPassword: String): Completable =
        Completable.fromCallable {
            payloadDecrypt.decryptHDWallet(
                validatedSecondPassword
            )
        }

    private fun decryptBch(): Completable = Completable.fromCallable {
        payloadDecrypt.decryptWatchOnlyWallet()
    }

    private fun SwapErrorType.toContent(
        minAmountFiat: FiatValue?,
        maxAmountFiat: FiatValue?
    ): SwapErrorDialogContent =
        when (this) {
            SwapErrorType.ORDER_BELOW_MIN_LIMIT -> SwapErrorDialogContent(
                ErrorBottomDialog.Content(
                    stringUtils.getString(R.string.markets_are_moving),
                    stringUtils.getFormattedString(R.string.markets_movement_markets_below_required,
                        minAmountFiat?.formatOrSymbolForZero() ?: ""), R.string.update_order,
                    R.string.more_info, 0),
                { view?.goBack() },
                { view?.openMoreInfoLink(URL_BLOCKCHAIN_ORDER_FAILED_BELOW_MIN) })
            SwapErrorType.ORDER_ABOVE_MAX_LIMIT ->
                SwapErrorDialogContent(
                    ErrorBottomDialog.Content(stringUtils.getString(
                        R.string.markets_are_moving),
                        stringUtils.getFormattedString(R.string.markets_movement_markets_above_required,
                            maxAmountFiat?.formatOrSymbolForZero() ?: ""), R.string.update_order,
                        R.string.more_info, 0),
                    { view?.goBack() },
                    { view?.openMoreInfoLink(URL_BLOCKCHAIN_ORDER_ABOVE_MAX) })
            SwapErrorType.DAILY_LIMIT_EXCEEDED,
            SwapErrorType.WEEKLY_LIMIT_EXCEEDED ->
                SwapErrorDialogContent(
                    ErrorBottomDialog.Content(stringUtils.getString(
                        R.string.hold_your_horses),
                        stringUtils.getFormattedString(R.string.above_limit_description,
                            maxAmountFiat?.formatOrSymbolForZero() ?: ""),
                        R.string.update_order,
                        R.string.more_info,
                        0), { view.goBack() },
                    { view?.openMoreInfoLink(URL_BLOCKCHAIN_ORDER_LIMIT_EXCEED) })
            SwapErrorType.ANNUAL_LIMIT_EXCEEDED ->
                SwapErrorDialogContent(
                    ErrorBottomDialog.Content(stringUtils.getString(
                        R.string.hold_your_horses),
                        stringUtils.getFormattedString(R.string.above_limit_description,
                            maxAmountFiat?.formatOrSymbolForZero() ?: ""),
                        R.string.update_order,
                        R.string.increase_limits,
                        0),
                    { view.goBack() },
                    { view.openTiersCard() })
            SwapErrorType.CONFIRMATION_ETH_PENDING ->
                SwapErrorDialogContent(
                    ErrorBottomDialog.Content(stringUtils.getString(
                        R.string.card_error_title),
                        stringUtils.getString(R.string.morph_confirmation_eth_pending),
                        R.string.try_again, 0, 0),
                    { view.goBack() },
                    { })
            SwapErrorType.ALBERT_EXECUTION_ERROR ->
                SwapErrorDialogContent(
                    ErrorBottomDialog.Content(stringUtils.getString(
                        R.string.card_error_title),
                        stringUtils.getString(R.string.something_went_wrong_description),
                        R.string.try_again,
                        R.string.more_info,
                        0),
                    { view.goBack() },
                    { view?.openMoreInfoLink(URL_BLOCKCHAIN_ORDER_EXPIRED) })
            else ->
                SwapErrorDialogContent(
                    ErrorBottomDialog.Content(stringUtils.getString(
                        R.string.card_error_title),
                        stringUtils.getString(R.string.something_went_wrong_description),
                        R.string.try_again, 0, 0),
                    { view?.goBack() },
                    { })
        }
}

private fun TradeTransaction.toTrade(): Trade {
    return Trade(
        id = id,
        state = MorphTrade.Status.IN_PROGRESS,
        currency = pair.to.displayTicker,
        price = fiatValue.toStringWithSymbol(),
        fee = fee.toStringWithSymbol(),
        pair = pair.pairCode,
        quantity = withdrawal.toStringWithSymbol(),
        createdAt = createdAt,
        depositQuantity = deposit.toStringWithSymbol()
    )
}

private fun CoinPair.isPaxTrade() = (from == CryptoCurrency.PAX) or (to == CryptoCurrency.PAX)

private fun TradeTransaction.isPaxTrade() = pair.isPaxTrade()

private class PaxTradeEvent : AnalyticsEvent {
    override val event = "pax_swap_traded"
    override val params = emptyMap<String, String>()
}

private fun SwapDiagnostics.logQuote(quote: Quote) {
    with(quote) {
        logStateVariable("QUOTE_CONFIRM_FIX", fix.toString())
        logStateVariable("QUOTE_CONFIRM_VAL_FROM", from.toLogString())
        logStateVariable("QUOTE_CONFIRM_VAL_TO", to.toLogString())
        logStateVariable("QUOTE_CONFIRM_RATE_BASE_2_FIAT", baseToFiatRate.toEngineeringString())
        logStateVariable("QUOTE_CONFIRM_RATE_BASE_2_COUNTER", baseToCounterRate.toEngineeringString())
        logStateVariable("QUOTE_CONFIRM_RATE_COUNTER_2_FIAT", counterToFiatRate.toEngineeringString())
    }
}

private fun Quote.Value.toLogString() = "${cryptoValue.toStringWithSymbol()} : ${fiatValue.toStringWithSymbol()}"
