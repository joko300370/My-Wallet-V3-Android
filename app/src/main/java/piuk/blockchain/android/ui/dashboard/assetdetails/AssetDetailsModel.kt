package piuk.blockchain.android.ui.dashboard.assetdetails

import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.androidcore.data.charts.TimeSpan
import timber.log.Timber

data class AssetDetailsState(
    val asset: CryptoAsset? = null,
    val selectedAccount: BlockchainAccount? = null,
    val assetDetailsCurrentStep: AssetDetailsStep = AssetDetailsStep.ZERO,
    val assetFilter: AssetFilter? = null,
    val assetDisplayMap: AssetDisplayMap? = null,
    val assetFiatValue: String = "",
    val timeSpan: TimeSpan = TimeSpan.DAY,
    val chartLoading: Boolean = false,
    val chartData: List<PriceDatum> = emptyList(),
    val errorState: AssetDetailsError = AssetDetailsError.NONE,
    val hostAction: AssetDetailsAction = AssetDetailsAction.NONE
) : MviState

enum class AssetDetailsError {
    NONE,
    NO_CHART_DATA,
    NO_ASSET_DETAILS,
    NO_EXCHANGE_RATE
}

enum class AssetDetailsAction {
    NONE,
    ACTIVITY,
    SEND,
    NEW_SEND,
    RECEIVE,
    SWAP,
    INTEREST_SUMMARY,
    DEPOSIT
}

class AssetDetailsModel(
    initialState: AssetDetailsState,
    mainScheduler: Scheduler,
    private val interactor: AssetDetailsInteractor
) : MviModel<AssetDetailsState, AssetDetailsIntent>(initialState, mainScheduler) {
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
            is LoadAssetDisplayDetails -> interactor.loadAssetDetails(previousState.asset!!)
                .subscribeBy(
                    onSuccess = {
                        process(AssetDisplayDetailsLoaded(it))
                    },
                    onError = {
                        process(AssetDisplayDetailsFailed)
                    })
            is LoadAssetFiatValue -> interactor.loadExchangeRate(previousState.asset!!)
                .subscribeBy(
                    onSuccess = {
                        process(AssetExchangeRateLoaded(it))
                    }, onError = {
                        process(AssetExchangeRateFailed)
                    })
            is LoadHistoricPrices -> updateChartData(previousState.asset!!, previousState.timeSpan)
            is UpdateTimeSpan -> updateChartData(previousState.asset!!, intent.updatedTimeSpan)
            is HandleActionIntent,
            is LoadAsset,
            is ChartLoading,
            is ChartDataLoaded,
            is ChartDataLoadFailed,
            is AssetDisplayDetailsLoaded,
            is AssetDisplayDetailsFailed,
            is AssetExchangeRateLoaded,
            is AssetExchangeRateFailed,
            is ShowAssetDetailsIntent,
            is ShowAssetActionsIntent,
            is ShowCustodyIntroSheetIntent,
            is ReturnToPreviousStep,
            is ClearSheetDataIntent -> null
        }
    }

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

    override fun onScanLoopError(t: Throwable) {
        Timber.e("***> Scan loop failed: $t")
    }
}
