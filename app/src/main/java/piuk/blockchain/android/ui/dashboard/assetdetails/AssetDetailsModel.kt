package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.models.data.RecurringBuy
import info.blockchain.balance.Money
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.exchangerate.TimeSpan
import timber.log.Timber
import java.util.Stack

data class AssetDetailsState(
    val asset: CryptoAsset? = null,
    val selectedAccount: BlockchainAccount? = null,
    val actions: AvailableActions = emptySet(),
    val assetDetailsCurrentStep: AssetDetailsStep = AssetDetailsStep.ZERO,
    val assetDisplayMap: AssetDisplayMap? = null,
    val recurringBuys: Map<String, RecurringBuy>? = null,
    val assetFiatPrice: String = "",
    val timeSpan: TimeSpan = TimeSpan.DAY,
    val chartLoading: Boolean = false,
    val chartData: List<PriceDatum> = emptyList(),
    val errorState: AssetDetailsError = AssetDetailsError.NONE,
    val hostAction: AssetAction? = null,
    val selectedAccountCryptoBalance: Money? = null,
    val selectedAccountFiatBalance: Money? = null,
    val navigateToInterestDashboard: Boolean = false,
    val selectedRecurringBuy: RecurringBuy? = null,
    val stepsBackStack: Stack<AssetDetailsStep> = Stack()
) : MviState

enum class AssetDetailsError {
    NONE,
    NO_CHART_DATA,
    NO_ASSET_DETAILS,
    NO_EXCHANGE_RATE,
    TX_IN_FLIGHT,
    NO_RECURRING_BUYS,
    RECURRING_BUY_DELETE
}

class AssetDetailsModel(
    initialState: AssetDetailsState,
    mainScheduler: Scheduler,
    private val interactor: AssetDetailsInteractor,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<AssetDetailsState, AssetDetailsIntent>(initialState, mainScheduler, environmentConfig, crashLogger) {
    override fun performAction(
        previousState: AssetDetailsState,
        intent: AssetDetailsIntent
    ): Disposable? {
        Timber.d("***> performAction: ${intent.javaClass.simpleName}")

        return when (intent) {
            is ShowRelevantAssetDetailsSheet -> interactor.shouldShowCustody(intent.cryptoCurrency)
                .subscribeBy(
                    onSuccess = {
                        if (it) {
                            process(ShowCustodyIntroSheetIntent)
                        } else {
                            process(ShowAssetDetailsIntent)
                        }
                    },
                    onError = {
                        // fail silently, try to show AssetSheet instead
                        process(ShowAssetDetailsIntent)
                    }
                )
            is ShowAssetActionsIntent -> accountActions(intent.account)
            is UpdateTimeSpan -> previousState.asset?.let { updateChartData(it, intent.updatedTimeSpan) }
            is LoadAsset -> {
                updateChartData(intent.asset, previousState.timeSpan)
                loadFiatExchangeRate(intent.asset)
                loadAssetDetails(intent.asset)
                loadRecurringBuysForAsset(intent.asset)
            }
            is DeleteRecurringBuy -> {
                previousState.selectedRecurringBuy?.let {
                    deleteRecurringBuy(it.id)
                }
            }
            is HandleActionIntent,
            is ChartLoading,
            is ChartDataLoaded,
            is ChartDataLoadFailed,
            is AssetDisplayDetailsLoaded,
            is AssetDisplayDetailsFailed,
            is AssetExchangeRateLoaded,
            is AssetExchangeRateFailed,
            is ShowAssetDetailsIntent,
            is ShowCustodyIntroSheetIntent,
            is SelectAccount,
            is ReturnToPreviousStep,
            is ClearSheetDataIntent,
            is CustodialSheetFinished,
            is TransactionInFlight,
            is ShowInterestDashboard,
            is ClearActionStates,
            is AccountActionsLoaded,
            is RecurringBuyDataFailed,
            is RecurringBuyDataLoaded,
            is ShowRecurringBuySheet,
            is ClearSelectedRecurringBuy,
            is UpdateRecurringBuy,
            is UpdateRecurringBuyError -> null
        }
    }

    private fun deleteRecurringBuy(id: String) =
        interactor.deleteRecurringBuy(id)
            .subscribeBy(
                onComplete = {
                    process(UpdateRecurringBuy)
                },
                onError = {
                    process(UpdateRecurringBuyError)
                }
            )

    private fun loadAssetDetails(asset: CryptoAsset) =
        interactor.loadAssetDetails(asset)
            .subscribeBy(
                onSuccess = {
                    process(AssetDisplayDetailsLoaded(it))
                },
                onError = {
                    process(AssetDisplayDetailsFailed)
                })

    private fun loadFiatExchangeRate(asset: CryptoAsset) =
        interactor.loadExchangeRate(asset)
            .subscribeBy(
                onSuccess = {
                    process(AssetExchangeRateLoaded(it))
                }, onError = {
                    process(AssetExchangeRateFailed)
                })

    private fun loadRecurringBuysForAsset(asset: CryptoAsset): Disposable =
        interactor.loadRecurringBuysForAsset(asset.asset.networkTicker)
            .subscribeBy(
                onSuccess = { list ->
                    process(RecurringBuyDataLoaded(list.map { it.id to it }.toMap()))
                },
                onError = {
                    process(RecurringBuyDataFailed)
                }
            )

    private fun updateChartData(asset: CryptoAsset, timeSpan: TimeSpan) =
        interactor.loadHistoricPrices(asset, timeSpan).doOnSubscribe {
            process(ChartLoading)
        }.subscribeBy(
            onSuccess = {
                process(ChartDataLoaded(it))
            },
            onError = {
                process(ChartDataLoadFailed)
            }
        )

    private fun accountActions(account: BlockchainAccount): Disposable =
        account.actions.subscribeBy(
            onSuccess = {
                process(AccountActionsLoaded(account, it))
            },
            onError = { Timber.e("***> Error Loading account actions: $it") }
        )
}
