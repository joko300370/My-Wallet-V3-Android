package piuk.blockchain.android.ui.dashboard

import androidx.fragment.app.FragmentManager
import com.blockchain.koin.scopedInject
import info.blockchain.balance.CryptoCurrency
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.core.KoinComponent
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetActionsSheet
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailSheet
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsModel
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsState
import piuk.blockchain.android.ui.dashboard.assetdetails.ShowAssetDetailsIntent
import piuk.blockchain.android.ui.transfer.send.flow.DialogFlow
import timber.log.Timber

enum class DashboardStep {
    ZERO,
    ASSET_DETAILS,
    ASSET_ACTIONS
}

class DashboardFlow(
    val cryptoCurrency: CryptoCurrency
) : DialogFlow(), KoinComponent {

    private var currentStep: DashboardStep = DashboardStep.ZERO
    private val disposables = CompositeDisposable()
    private val model: AssetDetailsModel by scopedInject()

    override fun startFlow(fragmentManager: FragmentManager, host: FlowHost) {
        super.startFlow(fragmentManager, host)

        model.apply {
            disposables += state.subscribeBy(
                onNext = { handleStateChange(it) },
                onError = { Timber.e("Dashboard state is broken: $it") }
            )
        }

        model.process(ShowAssetDetailsIntent)
    }

    private fun handleStateChange(newState: AssetDetailsState) {
        if (currentStep != newState.assetDetailsCurrentStep) {
            currentStep = newState.assetDetailsCurrentStep
            if (currentStep == DashboardStep.ZERO) {
                finishFlow()
            } else {
                showFlowStep(currentStep, newState)
            }
        }
    }

    private fun showFlowStep(step: DashboardStep, newState: AssetDetailsState) {
        replaceBottomSheet(
            when (step) {
                DashboardStep.ZERO -> null
                DashboardStep.ASSET_DETAILS -> AssetDetailSheet.newInstance(cryptoCurrency)
                DashboardStep.ASSET_ACTIONS -> AssetActionsSheet.newInstance(newState.selectedAccount!!, newState.assetFilter!!)
            }
        )
    }

    override fun finishFlow() {
        disposables.clear()
        currentStep = DashboardStep.ZERO
        super.finishFlow()
    }

    override fun onSheetClosed() {
        finishFlow()
    }
}