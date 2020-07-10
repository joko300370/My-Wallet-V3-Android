package piuk.blockchain.android.ui.activity

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import timber.log.Timber

enum class ActivitiesSheet {
    ACCOUNT_SELECTOR,
    CRYPTO_ACTIVITY_DETAILS,
    FIAT_ACTIVITY_DETAILS,
    BANK_TRANSFER_DETAILS,
    BANK_ORDER_CANCEL
}

data class ActivitiesState(
    val account: BlockchainAccount? = null,
    val activityList: ActivitySummaryList = emptyList(),
    val isLoading: Boolean = false,
    val bottomSheet: ActivitiesSheet? = null,
    val isError: Boolean = false,
    val selectedTxId: String = "",
    val selectedCryptoCurrency: CryptoCurrency? = null,
    val selectedFiatCurrency: String? = null,
    val isCustodial: Boolean = false
) : MviState

class ActivitiesModel(
    initialState: ActivitiesState,
    mainScheduler: Scheduler,
    private val interactor: ActivitiesInteractor
) : MviModel<ActivitiesState, ActivitiesIntent>(
    initialState,
    mainScheduler
) {
    override fun performAction(
        previousState: ActivitiesState,
        intent: ActivitiesIntent
    ): Disposable? {
        Timber.d("***> performAction: ${intent.javaClass.simpleName}")

        return when (intent) {
            is AccountSelectedIntent ->
                interactor.getActivityForAccount(intent.account, intent.isRefreshRequested)
                    .subscribeBy(
                        onNext = {
                            process(ActivityListUpdatedIntent(it))
                        },
                        onComplete = {
                            // do nothing
                        },
                        onError = { process(ActivityListUpdatedErrorIntent) }
                    )
            is SelectDefaultAccountIntent ->
                interactor.getDefaultAccount()
                    .subscribeBy(
                        onSuccess = { process(AccountSelectedIntent(it, true)) },
                        onError = { process(ActivityListUpdatedErrorIntent) }
                    )
            is CancelSimpleBuyOrderIntent -> interactor.cancelSimpleBuyOrder(intent.orderId)
            else -> null
        }
    }

    override fun onScanLoopError(t: Throwable) {
        Timber.e("***> Scan loop failed: $t")
    }
}
