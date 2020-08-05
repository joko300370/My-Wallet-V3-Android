package piuk.blockchain.android.ui.dashboard.assetdetails

import androidx.fragment.app.FragmentManager
import com.blockchain.koin.scopedInject
import info.blockchain.balance.CryptoCurrency
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.core.KoinComponent
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.ui.customviews.account.AccountSelectSheet
import piuk.blockchain.android.ui.transfer.send.flow.DialogFlow
import timber.log.Timber

enum class AssetDetailsStep {
    ZERO,
    CUSTODY_INTRO_SHEET,
    ASSET_DETAILS,
    ASSET_ACTIONS,
    SELECT_ACCOUNT
}

class AssetDetailsFlow(
    val cryptoCurrency: CryptoCurrency
) : DialogFlow(), KoinComponent, AccountSelectSheet.Host {

    interface AssetDetailsHost : FlowHost {
        fun launchNewSendFor(account: SingleAccount)
        fun gotoSendFor(account: SingleAccount)
        fun goToReceiveFor(account: SingleAccount)
        fun gotoActivityFor(account: BlockchainAccount)
        fun gotoSwap(account: SingleAccount)
        fun goToDeposit(
            fromAccount: SingleAccount,
            toAccount: SingleAccount,
            cryptoAsset: CryptoAsset
        )
    }

    private var currentStep: AssetDetailsStep = AssetDetailsStep.ZERO
    private var localState: AssetDetailsState = AssetDetailsState()
    private val disposables = CompositeDisposable()
    private val model: AssetDetailsModel by scopedInject()
    private lateinit var assetFlowHost: AssetDetailsHost

    override fun startFlow(fragmentManager: FragmentManager, host: FlowHost) {
        super.startFlow(fragmentManager, host)

        assetFlowHost = host as? AssetDetailsHost
            ?: throw IllegalStateException("Flow Host is not an AssetDetailsHost")

        model.apply {
            disposables += state.subscribeBy(
                onNext = { handleStateChange(it) },
                onError = { Timber.e("Asset details state is broken: $it") }
            )
        }

        model.process(ShowRelevantAssetDetailsSheet(cryptoCurrency))
    }

    private fun handleStateChange(
        newState: AssetDetailsState
    ) {
        localState = newState
        if (currentStep != newState.assetDetailsCurrentStep) {
            currentStep = newState.assetDetailsCurrentStep
            if (currentStep == AssetDetailsStep.ZERO) {
                finishFlow()
            } else {
                showFlowStep(currentStep, newState)
            }
        }

        if (newState.hostAction != AssetDetailsAction.NONE) {
            handleHostAction(newState, assetFlowHost)
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
                AssetDetailsStep.SELECT_ACCOUNT -> AccountSelectSheet.newInstance(
                    newState.assetFilter!!,
                    cryptoCurrency, this)
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
            AssetDetailsAction.INTEREST_SUMMARY -> TODO()
            AssetDetailsAction.DEPOSIT -> {
                newState.asset!!.accountGroup(AssetFilter.NonCustodial).subscribeBy {
                    getInterestAccountAndNavigate(it.accounts.first())
                }
            }
            AssetDetailsAction.NONE -> {
                // do nothing
            }
        }
    }

    override fun finishFlow() {
        resetFow()
        super.finishFlow()
    }

    override fun onAccountSelected(account: BlockchainAccount) {
        getInterestAccountAndNavigate(account as SingleAccount)
    }

    private fun getInterestAccountAndNavigate(account: SingleAccount) {
        disposables += localState.asset!!.accountGroup(AssetFilter.Interest).subscribeBy {
            assetFlowHost.goToDeposit(
                account,
                it.accounts.first(),
                localState.asset!!)
        }
    }

    override fun onAccountSelectorBack() {
        model.process(ReturnToPreviousStep)
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