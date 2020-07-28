package piuk.blockchain.android.ui.dashboard

import androidx.fragment.app.FragmentManager
import info.blockchain.balance.CryptoCurrency
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetActionsSheet
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailSheet
import piuk.blockchain.android.ui.transfer.send.flow.DialogFlow
import timber.log.Timber

enum class DashboardStep {
    ZERO,
    ASSET_DETAILS,
    ASSET_ACTIONS
}

class DashboardFlow(
    val cryptoCurrency: CryptoCurrency,
    val model: DashboardModel
) : DialogFlow() {

    init {
        Timber.e("----- starting dashboard flow for $cryptoCurrency")
    }

    private var currentStep: DashboardStep = DashboardStep.ZERO
    private val disposables = CompositeDisposable()

    override fun startFlow(fragmentManager: FragmentManager, host: FlowHost) {
        super.startFlow(fragmentManager, host)

        model.apply {
            disposables += state.subscribeBy(
                onNext = { handleStateChange(it) },
                onError = { Timber.e("Send state is broken: $it") }
            )
        }

        model.process(ShowAssetDetailsIntent)
    }

    private fun handleStateChange(newState: DashboardState) {
        if (currentStep != newState.assetDetailsCurrentStep) {
            currentStep = newState.assetDetailsCurrentStep
            if (currentStep == DashboardStep.ZERO) {
                // onSendComplete()
            } else {
                showFlowStep(currentStep, newState)
            }
        }
    }

    private fun showFlowStep(step: DashboardStep, newState: DashboardState) {
        replaceBottomSheet(
            when (step) {
                DashboardStep.ZERO -> null
                DashboardStep.ASSET_DETAILS -> AssetDetailSheet.newInstance(cryptoCurrency)
                DashboardStep.ASSET_ACTIONS -> AssetActionsSheet.newInstance(newState.selectedAccount!!)
            }
        )
    }

    override fun onSheetClosed() {
        finishFlow()
    }
}