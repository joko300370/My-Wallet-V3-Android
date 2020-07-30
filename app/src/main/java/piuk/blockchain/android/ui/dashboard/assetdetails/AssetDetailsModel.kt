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
    val chartData: List<PriceDatum> = emptyList()
) : MviState

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
            is LoadAssetDisplayDetails -> interactor.loadAssetDetails(previousState.asset!!)
                .subscribeBy(
                    onSuccess = {
                        process(AssetDisplayDetailsLoaded(it))
                    },
                    onError = {
                    })
            is LoadAssetFiatValue -> interactor.loadExchangeRate(previousState.asset!!)
                .subscribeBy(
                    onSuccess = {
                        process(AssetExchangeRateLoaded(it))
                    }, onError = {
                })
            is LoadHistoricPrices -> updateChartData(previousState.asset!!, previousState.timeSpan)
            is UpdateTimeSpan -> updateChartData(previousState.asset!!, intent.updatedTimeSpan)
            is LoadAsset,
            is ChartLoading,
            is ChartDataLoaded,
            is AssetDisplayDetailsLoaded,
            is AssetExchangeRateLoaded,
            is ShowAssetDetailsIntent,
            is ShowAssetActionsIntent,
            is ReturnToPreviousStep -> null
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
                // TODO
            }
        )

    override fun onScanLoopError(t: Throwable) {
        Timber.e("***> Scan loop failed: $t")
    }
}
