package piuk.blockchain.android.ui.transactionflow

import androidx.annotation.CallSuper
import androidx.annotation.UiThread
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.core.KoinComponent
import org.koin.core.inject
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.NullCryptoAccount
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviFragment.Companion.BOTTOM_SHEET
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionStep
import piuk.blockchain.android.ui.transactionflow.flow.ConfirmTransactionSheet
import piuk.blockchain.android.ui.transactionflow.flow.EnterAmountSheet
import piuk.blockchain.android.ui.transactionflow.flow.EnterSecondPasswordSheet
import piuk.blockchain.android.ui.transactionflow.flow.EnterTargetAddressSheet
import piuk.blockchain.android.ui.transactionflow.flow.TransactionProgressSheet
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import timber.log.Timber

abstract class DialogFlow : SlidingModalBottomDialog.Host {

    private var fragmentManager: FragmentManager? = null
    private var host: FlowHost? = null
    private var bottomSheetTag: String =
        SHEET_FRAGMENT_TAG

    interface FlowHost {
        fun onFlowFinished()
    }

    @CallSuper
    open fun startFlow(
        fragmentManager: FragmentManager,
        host: FlowHost
    ) {
        this.fragmentManager = fragmentManager
        this.host = host
    }

    @CallSuper
    open fun finishFlow() {
        host?.onFlowFinished()
    }

    @UiThread
    protected fun replaceBottomSheet(bottomSheet: BottomSheetDialogFragment?) {
        val oldSheet = fragmentManager?.findFragmentByTag(bottomSheetTag)

        fragmentManager?.beginTransaction()?.run {
            apply { oldSheet?.let { sheet -> remove(sheet) } }
            apply { bottomSheet?.let { sheet -> add(sheet, bottomSheetTag) } }
            commitNow()
        }
    }

    companion object {
        const val SHEET_FRAGMENT_TAG = BOTTOM_SHEET
    }
}

class TransactionFlow(
    private val sourceAccount: CryptoAccount = NullCryptoAccount(),
    private val target: TransactionTarget = NullCryptoAccount(),
    private val action: AssetAction,
    private val uiScheduler: Scheduler = AndroidSchedulers.mainThread()
) : DialogFlow(), KoinComponent {

    private val disposables: CompositeDisposable = CompositeDisposable()
    private var currentStep: TransactionStep = TransactionStep.ZERO

    private val analyticsHooks: TxFlowAnalytics by inject()

    override fun startFlow(
        fragmentManager: FragmentManager,
        host: FlowHost
    ) {
        super.startFlow(fragmentManager, host)
        // Create the send scope
        openScope()
        // Get the model
        model.apply {
            // Trigger intent to set initial state: source account & password required
            disposables += state.subscribeBy(
                onNext = { handleStateChange(it) },
                onError = { Timber.e("Transaction state is broken: $it") }
            )
        }

        disposables += sourceAccount.requireSecondPassword()
            .observeOn(uiScheduler)
            .subscribeBy(
                onSuccess = { passwordRequired ->
                    if (target !is NullCryptoAccount &&
                        sourceAccount !is NullCryptoAccount
                    ) {
                        model.process(
                            TransactionIntent.InitialiseWithSourceAndTargetAccount(
                                action, sourceAccount, target, passwordRequired
                            )
                        )
                    } else if (sourceAccount !is NullCryptoAccount) {
                        model.process(
                            TransactionIntent.InitialiseWithSourceAccount(action, sourceAccount, passwordRequired)
                        )
                    } else {
                        throw IllegalStateException(
                            "Transaction flow initialised without at least one target")
                    }
                },
                onError = {
                    Timber.e("Unable to configure transaction flow, aborting. e == $it")
                    finishFlow()
                })
    }

    override fun finishFlow() {
        currentStep = TransactionStep.ZERO
        disposables.clear()
        closeScope()
        super.finishFlow()
    }

    private fun handleStateChange(newState: TransactionState) {
        if (currentStep != newState.currentStep) {
            if (newState.currentStep == TransactionStep.ZERO) {
                onTransactionComplete()
            } else {
                currentStep = newState.currentStep
                showFlowStep(currentStep)
                analyticsHooks.onStepChanged(newState)
            }
        }
    }

    private fun showFlowStep(step: TransactionStep) {
        replaceBottomSheet(
            when (step) {
                TransactionStep.ZERO -> null
                TransactionStep.ENTER_PASSWORD -> EnterSecondPasswordSheet(
                    this
                )
                TransactionStep.ENTER_ADDRESS -> EnterTargetAddressSheet(
                    this
                )
                TransactionStep.ENTER_AMOUNT -> EnterAmountSheet(
                    this
                )
                TransactionStep.CONFIRM_DETAIL -> ConfirmTransactionSheet(
                    this
                )
                TransactionStep.IN_PROGRESS -> TransactionProgressSheet(
                    this
                )
            }
        )
    }

    private fun openScope() =
        try {
            createTransactionScope()
        } catch (e: Throwable) {
            Timber.wtf("$e")
        }

    private fun closeScope() =
        closeTransactionScope()

    private val model: TransactionModel
        get() = transactionScope().get()

    private fun onTransactionComplete() =
        finishFlow()

    override fun onSheetClosed() {
        finishFlow()
    }
}
