package piuk.blockchain.android.ui.dashboard.assetdetails

import androidx.fragment.app.FragmentManager
import com.blockchain.koin.scopedInject
import info.blockchain.balance.CryptoCurrency
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.core.KoinComponent
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.SingleAccount
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

    interface AssetDetailsHost : FlowHost {
        fun launchNewSendFor(account: SingleAccount)
        fun gotoSendFor(account: SingleAccount)
        fun goToReceiveFor(account: SingleAccount)
        fun gotoActivityFor(account: BlockchainAccount)
        fun gotoSwap(account: SingleAccount)
    }

    private var currentStep: AssetDetailsStep = AssetDetailsStep.ZERO
    private val disposables = CompositeDisposable()
    private val model: AssetDetailsModel by scopedInject()

    override fun startFlow(fragmentManager: FragmentManager, host: FlowHost) {
        super.startFlow(fragmentManager, host)

        model.apply {
            disposables += state.subscribeBy(
                onNext = { handleStateChange(it, host) },
                onError = { Timber.e("Asset details state is broken: $it") }
            )
        }

        model.process(ShowRelevantAssetDetailsSheet(cryptoCurrency))
    }

    private fun handleStateChange(
        newState: AssetDetailsState,
        host: FlowHost
    ) {
        if (currentStep != newState.assetDetailsCurrentStep) {
            currentStep = newState.assetDetailsCurrentStep
            if (currentStep == AssetDetailsStep.ZERO) {
                finishFlow()
            } else {
                showFlowStep(currentStep, newState)
            }
        }

        if (newState.hostAction != AssetDetailsAction.NONE) {
            handleHostAction(newState, host as? AssetDetailsHost
            ?: throw IllegalStateException("Flow Host is not an AssetDetailsHost"))
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

    private fun handleHostAction(
        newState: AssetDetailsState,
        host: AssetDetailsHost
    ) {
        val account = newState.selectedAccount.selectFirstAccount()
        when (newState.hostAction) {
            AssetDetailsAction.ACTIVITY -> host.gotoActivityFor(account)
            AssetDetailsAction.SEND -> host.gotoSendFor(account)
            AssetDetailsAction.NEW_SEND -> host.launchNewSendFor(account)
            AssetDetailsAction.RECEIVE -> host.goToReceiveFor(account)
            AssetDetailsAction.SWAP -> host.gotoSwap(account)
            AssetDetailsAction.INTEREST -> TODO()
            AssetDetailsAction.DEPOSIT -> TODO()
            AssetDetailsAction.NONE -> {
                // do nothing
            }
        }
    }

    override fun finishFlow() {
        resetFow()
        super.finishFlow()
    }

    override fun onSheetClosed() {
        if (currentStep == AssetDetailsStep.ZERO) {
            finishFlow()
        }
    }

    private fun resetFow() {
        disposables.clear()
        currentStep = AssetDetailsStep.ZERO
        model.process(ClearSheetDataIntent)
    }
}

fun BlockchainAccount?.selectFirstAccount(): CryptoAccount {
    val selectedAccount = when (this) {
        is SingleAccount -> this
        is AccountGroup -> this.accounts
            .firstOrNull { a -> a.isDefault }
            ?: this.accounts.firstOrNull()
            ?: throw IllegalStateException("No SingleAccount found")
        else -> throw IllegalStateException("Unknown account base")
    }

    return selectedAccount as CryptoAccount
}