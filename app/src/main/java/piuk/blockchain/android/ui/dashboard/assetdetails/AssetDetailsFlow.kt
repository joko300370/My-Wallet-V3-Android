package piuk.blockchain.android.ui.dashboard.assetdetails

import androidx.fragment.app.FragmentManager
import com.blockchain.koin.scopedInject
import info.blockchain.balance.CryptoCurrency
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.core.KoinComponent
import piuk.blockchain.android.ui.transfer.send.flow.DialogFlow
import timber.log.Timber

enum class AssetDetailsStep {
    ZERO,
    CUSTODY_INTRO_SHEET,
    ASSET_DETAILS,
    ASSET_ACTIONS
}

class AssetDetailsFlow(
    val cryptoCurrency: CryptoCurrency
) : DialogFlow(), KoinComponent {

    private var currentStep: AssetDetailsStep = AssetDetailsStep.ZERO
    private val disposables = CompositeDisposable()
    private val model: AssetDetailsModel by scopedInject()

    override fun startFlow(fragmentManager: FragmentManager, host: FlowHost) {
        super.startFlow(fragmentManager, host)

        model.apply {
            disposables += state.subscribeBy(
                onNext = { handleStateChange(it) },
                onError = { Timber.e("Asset details state is broken: $it") }
            )
        }

        model.process(ShowRelevantAssetDetailsSheet(cryptoCurrency))
    }

    private fun handleStateChange(newState: AssetDetailsState) {
        if (currentStep != newState.assetDetailsCurrentStep) {
            currentStep = newState.assetDetailsCurrentStep
            if (currentStep == AssetDetailsStep.ZERO) {
                finishFlow()
            } else {
                showFlowStep(currentStep, newState)
            }
        }
    }

    private fun showFlowStep(step: AssetDetailsStep, newState: AssetDetailsState) {
        replaceBottomSheet(
            when (step) {
                AssetDetailsStep.ZERO -> null
                AssetDetailsStep.CUSTODY_INTRO_SHEET -> CustodyWalletIntroSheet.newInstance()
                AssetDetailsStep.ASSET_DETAILS -> AssetDetailSheet.newInstance(cryptoCurrency)
                AssetDetailsStep.ASSET_ACTIONS ->
                    AssetActionsSheet.newInstance(newState.selectedAccount!!,
                        newState.assetFilter!!)
            }
        )
    }

    override fun finishFlow() {
        resetFow()
        super.finishFlow()
    }

    override fun onSheetClosed() {
        if(currentStep == AssetDetailsStep.ZERO) {
            finishFlow()
        }
    }

    private fun resetFow() {
        disposables.clear()
        currentStep = AssetDetailsStep.ZERO
        model.process(ClearSheetDataIntent)
    }
}