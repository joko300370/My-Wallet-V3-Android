package piuk.blockchain.android.ui.swap.homebrew.exchange.confirmation

import android.support.annotation.VisibleForTesting
import com.blockchain.datamanagers.TransactionExecutorWithoutFees
import com.blockchain.logging.CrashLogger
import com.blockchain.morph.CoinPair
import com.blockchain.morph.exchange.SwapFailureDiagnostics
import com.blockchain.morph.exchange.mvi.Quote
import com.blockchain.morph.exchange.service.TradeExecutionService
import com.blockchain.morph.exchange.service.TradeTransaction
import com.blockchain.morph.trade.MorphTrade
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.payload.PayloadDecrypt
import com.blockchain.serialization.fromMoshiJson
import com.blockchain.transactions.Memo
import com.blockchain.transactions.SendException
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
import piuk.blockchain.android.ui.swap.homebrew.exchange.model.SwapErrorDialogContent
import piuk.blockchain.android.ui.swap.homebrew.exchange.model.SwapErrorResponse
import piuk.blockchain.android.ui.swap.homebrew.exchange.model.SwapErrorType
import piuk.blockchain.android.ui.swap.homebrew.exchange.model.Trade
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.ethereum.exceptions.TransactionInProgressException
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.ui.dlg.ErrorBottomDialog
import retrofit2.HttpException
import timber.log.Timber
import java.util.Locale

class ExchangeConfirmationPresenter internal constructor(
    private val transactionExecutor: TransactionExecutorWithoutFees,
    private val tradeExecutionService: TradeExecutionService,
    private val payloadDecrypt: PayloadDecrypt,
    private val stringUtils: StringUtils,
    private val locale: Locale,
    private val analytics: Analytics,
    private val crashLogger: CrashLogger
) : BasePresenter<ExchangeConfirmationView>() {

    private var showPaxAirdropBottomDialog: Boolean = false
    private var minSpendableFiatValue: FiatValue? = null
    private var maxSpendableFiatValue: FiatValue? = null
    private var maxSpendable: CryptoValue? = null
    private var executeTradeSingle: Single<TradeTransaction>? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val diagnostics = SwapFailureDiagnostics()

    override fun onViewReady() {
        // Ensure user hasn't got a double encrypted wallet
        subscribeToViewState()
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

                    diagnostics.maxSpendable = maxSpendable

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
                        diagnostics.fee = it
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

        diagnostics.quote = quote

        return deriveAddressPair(sendingAccount, receivingAccount)
            .subscribeOn(Schedulers.io())
            .flatMap { (destination, refund) ->
                tradeExecutionService.executeTrade(quote, destination, refund)
                    .subscribeOn(Schedulers.io())
                    .flatMap { transaction ->
                        sendFundsForTrade(transaction, sendingAccount, diagnostics)
                            .subscribeOn(Schedulers.io())
                    }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view.showProgressDialog() }
            .doOnEvent { _, _ -> view.dismissProgressDialog() }
            .doOnError { onExecuteTradeFailed(it) }
            .doOnSuccess {
                view.onTradeSubmitted(it.toTrade(locale),
                    showPaxAirdropBottomDialog && receivingAccount.cryptoCurrency == CryptoCurrency.PAX
                )
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
            val rawError = (t as?HttpException)?.response()?.errorBody()?.string()
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
    }

    private fun sendFundsForTrade(
        transaction: TradeTransaction,
        sendingAccount: AccountReference,
        diagnostics: SwapFailureDiagnostics
    ): Single<TradeTransaction> {

        return transactionExecutor.executeTransaction(
            transaction.deposit,
            transaction.depositAddress,
            sendingAccount,
            memo = transaction.memo(),
            diagnostics = diagnostics
        ).flatMap {
            Single.just(transaction)
        }.onErrorResumeNext {
            Timber.e(it, "Transaction execution error, telling nabu")
            analytics.logEvent(AnalyticsEvents.ExchangeExecutionError)
            val hash = (it as? TransactionHashApiException)?.hashString ?: (it as? SendException)?.hash

            crashLogger.logException(diagnostics.toLoggable())

            tradeExecutionService.putTradeFailureReason(transaction, hash, it.message)
                .andThen(Single.error(it))
        }.doOnSuccess { triggerPaxTradeEvent(it) }
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
            SwapErrorType.ORDER_BELOW_MIN_LIMIT -> SwapErrorDialogContent(ErrorBottomDialog.Content(
                stringUtils.getString(R.string.markets_are_moving),
                stringUtils.getFormattedString(R.string.markets_movement_markets_below_required,
                    minAmountFiat?.formatOrSymbolForZero() ?: ""), R.string.update_order,
                R.string.more_info, 0),
                { view?.goBack() },
                { view?.openMoreInfoLink(URL_BLOCKCHAIN_ORDER_FAILED_BELOW_MIN) })
            SwapErrorType.ORDER_ABOVE_MAX_LIMIT ->
                SwapErrorDialogContent(ErrorBottomDialog.Content(stringUtils.getString(
                    R.string.markets_are_moving),
                    stringUtils.getFormattedString(R.string.markets_movement_markets_above_required,
                        maxAmountFiat?.formatOrSymbolForZero() ?: ""), R.string.update_order,
                    R.string.more_info, 0),
                    { view?.goBack() },
                    { view?.openMoreInfoLink(URL_BLOCKCHAIN_ORDER_ABOVE_MAX) })
            SwapErrorType.DAILY_LIMIT_EXCEEDED,
            SwapErrorType.WEEKLY_LIMIT_EXCEEDED ->
                SwapErrorDialogContent(ErrorBottomDialog.Content(stringUtils.getString(
                    R.string.hold_your_horses),
                    stringUtils.getFormattedString(R.string.above_limit_description,
                        maxAmountFiat?.formatOrSymbolForZero() ?: ""),
                    R.string.update_order,
                    R.string.more_info,
                    0), { view.goBack() },
                    { view?.openMoreInfoLink(URL_BLOCKCHAIN_ORDER_LIMIT_EXCEED) })
            SwapErrorType.ANNUAL_LIMIT_EXCEEDED ->
                SwapErrorDialogContent(ErrorBottomDialog.Content(stringUtils.getString(
                    R.string.hold_your_horses),
                    stringUtils.getFormattedString(R.string.above_limit_description,
                        maxAmountFiat?.formatOrSymbolForZero() ?: ""),
                    R.string.update_order,
                    R.string.increase_limits,
                    0),
                    { view.goBack() },
                    { view.openTiersCard() })
            SwapErrorType.CONFIRMATION_ETH_PENDING ->
                SwapErrorDialogContent(ErrorBottomDialog.Content(stringUtils.getString(
                    R.string.ops_something_went_wrong),
                    stringUtils.getString(R.string.morph_confirmation_eth_pending),
                    R.string.try_again, 0, 0),
                    { view.goBack() },
                    { })
            SwapErrorType.ALBERT_EXECUTION_ERROR ->
                SwapErrorDialogContent(ErrorBottomDialog.Content(stringUtils.getString(
                    R.string.ops_something_went_wrong),
                    stringUtils.getString(R.string.something_went_wrong_description),
                    R.string.try_again,
                    R.string.more_info,
                    0),
                    { view.goBack() },
                    { view?.openMoreInfoLink(URL_BLOCKCHAIN_ORDER_EXPIRED) })
            else ->
                SwapErrorDialogContent(ErrorBottomDialog.Content(stringUtils.getString(
                    R.string.ops_something_went_wrong),
                    stringUtils.getString(R.string.something_went_wrong_description),
                    R.string.try_again, 0, 0),
                    { view?.goBack() },
                    { })
        }
}

private fun TradeTransaction.toTrade(locale: Locale): Trade {
    return Trade(
        id = id,
        state = MorphTrade.Status.IN_PROGRESS,
        currency = pair.to.symbol,
        price = fiatValue.toStringWithSymbol(locale),
        fee = fee.toStringWithSymbol(locale),
        pair = pair.pairCode,
        quantity = withdrawal.toStringWithSymbol(locale),
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
