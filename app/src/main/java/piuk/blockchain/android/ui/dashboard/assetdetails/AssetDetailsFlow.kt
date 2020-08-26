package piuk.blockchain.android.ui.dashboard.assetdetails

import androidx.fragment.app.FragmentManager
import com.blockchain.koin.scopedInject
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.core.KoinComponent
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.android.coincore.SendState
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
    val cryptoCurrency: CryptoCurrency,
    val coincore: Coincore
) : DialogFlow(), KoinComponent, AccountSelectSheet.SelectAndBackHost {

    interface AssetDetailsHost : FlowHost {
        fun launchNewSendFor(account: SingleAccount, action: AssetAction)
        fun gotoSendFor(account: SingleAccount)
        fun goToReceiveFor(account: SingleAccount)
        fun gotoActivityFor(account: BlockchainAccount)
        fun gotoSwap(account: SingleAccount)
        fun goToDeposit(
            fromAccount: SingleAccount,
            toAccount: SingleAccount,
            cryptoAsset: CryptoAsset,
            action: AssetAction
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
                showFlowStep(currentStep)
            }
        }

        if (newState.hostAction != null) {
            handleHostAction(newState, assetFlowHost)
        }
    }

    private fun showFlowStep(step: AssetDetailsStep) {
        replaceBottomSheet(
            when (step) {
                AssetDetailsStep.ZERO -> null
                AssetDetailsStep.CUSTODY_INTRO_SHEET -> CustodyWalletIntroSheet.newInstance()
                AssetDetailsStep.ASSET_DETAILS -> AssetDetailSheet.newInstance(cryptoCurrency)
                AssetDetailsStep.ASSET_ACTIONS -> AssetActionsSheet.newInstance()
                AssetDetailsStep.SELECT_ACCOUNT -> AccountSelectSheet.newInstance(
                    this, interestAccountsFilter(), R.string.select_deposit_source_title)
            }
        )
    }

    private fun interestAccountsFilter(): Single<List<BlockchainAccount>> =
        coincore[cryptoCurrency].accountGroup(AssetFilter.NonCustodial)
            .map { it.accounts }.toSingle(emptyList())
            .map { it.filter { a -> a.isFunded } }

    private fun handleHostAction(
        newState: AssetDetailsState,
        host: AssetDetailsHost
    ) {
        val account = newState.selectedAccount.selectFirstAccount()
        when (newState.hostAction) {
            AssetAction.ViewActivity -> {
                host.gotoActivityFor(account)
                finishFlow()
            }
            AssetAction.Send -> {
                host.gotoSendFor(account)
                finishFlow()
            }
            AssetAction.NewSend -> {
                host.launchNewSendFor(account, newState.hostAction)
                finishFlow()
            }
            AssetAction.Receive -> {
                host.goToReceiveFor(account)
                finishFlow()
            }
            AssetAction.Swap -> {
                host.gotoSwap(account)
                finishFlow()
            }
            AssetAction.Summary -> TODO()
            AssetAction.Deposit -> newState.asset!!.accountGroup(AssetFilter.NonCustodial)
                .subscribeBy {
                    getInterestAccountAndNavigate(it.accounts.first(), newState.hostAction)
                }
        }
    }

    override fun finishFlow() {
        resetFlow()
        super.finishFlow()
    }

    override fun onAccountSelected(account: BlockchainAccount) {
        getInterestAccountAndNavigate(account as SingleAccount, AssetAction.Deposit)
    }

    private fun getInterestAccountAndNavigate(account: SingleAccount, assetAction: AssetAction) {
        localState.asset?.let { ca ->
            disposables += account.sendState.subscribeBy {
                if (it == SendState.SEND_IN_FLIGHT) {
                    model.process(TransactionInFlight)
                } else {
                    ca.accountGroup(AssetFilter.Interest).subscribeBy { ag ->
                        assetFlowHost.goToDeposit(
                            account,
                            ag.accounts.first(),
                            ca,
                            assetAction)
                        finishFlow()
                    }
                }
            }
        } ?: throw IllegalStateException("No asset defined in local state - action: $assetAction")
    }

    override fun onAccountSelectorBack() {
        model.process(ReturnToPreviousStep)
    }

    override fun onSheetClosed() {
        if (currentStep == AssetDetailsStep.ZERO) {
            finishFlow()
        }
    }

    private fun resetFlow() {
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