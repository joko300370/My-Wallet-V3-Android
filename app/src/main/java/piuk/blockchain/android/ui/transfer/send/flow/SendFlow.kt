package piuk.blockchain.android.ui.transfer.send.flow

import androidx.annotation.CallSuper
import androidx.annotation.UiThread
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendModel
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.ui.transfer.send.SendStep
import piuk.blockchain.android.ui.transfer.send.closeSendScope
import piuk.blockchain.android.ui.transfer.send.createSendScope
import piuk.blockchain.android.ui.transfer.send.sendScope
import timber.log.Timber

interface FlowStep

abstract class DialogFlow : SlidingModalBottomDialog.Host {

    private var fragmentManager: FragmentManager? = null
    private var host: FlowHost? = null
    private var bottomSheetTag: String = SHEET_FRAGMENT_TAG

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
        const val SHEET_FRAGMENT_TAG = "BOTTOM_SHEET"
    }
}

class SendFlow(
    private val coincore: Coincore,
    private val account: CryptoAccount,
    private val uiScheduler: Scheduler = AndroidSchedulers.mainThread()
) : DialogFlow() {

    private val disposables: CompositeDisposable = CompositeDisposable()
    private var currentStep: SendStep = SendStep.ZERO

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
                onError = { Timber.e("Send state is broken: $it") }
            )
        }

        disposables += coincore.requireSecondPassword()
            .observeOn(uiScheduler)
            .subscribeBy(
                onSuccess = { passwordRequired ->
                    model.process(SendIntent.Initialise(account, passwordRequired))
                },
                onError = {
                    Timber.e("Unable to configure send flow, aborting. e == $it")
                    finishFlow()
                })
    }

    override fun finishFlow() {
        currentStep = SendStep.ZERO
        disposables.clear()
        closeScope()
        super.finishFlow()
    }

    private fun handleStateChange(newState: SendState) {
        if (currentStep != newState.currentStep) {
            currentStep = newState.currentStep
            if (currentStep == SendStep.ZERO) {
                onSendComplete()
            } else {
                showFlowStep(currentStep)
            }
        }
    }

    private fun showFlowStep(step: SendStep) {
        replaceBottomSheet(
            when (step) {
                SendStep.ZERO -> null
                SendStep.ENTER_PASSWORD -> EnterSecondPasswordSheet(this)
                SendStep.ENTER_ADDRESS -> EnterTargetAddressSheet(this)
                SendStep.ENTER_AMOUNT -> EnterAmountSheet(this)
                SendStep.CONFIRM_DETAIL -> ConfirmTransactionSheet(this)
                SendStep.IN_PROGRESS -> TransactionProgressSheet(this)
            }
        )
    }

    private fun openScope() =
        try {
            createSendScope()
        } catch (e: Throwable) {
            Timber.wtf("$e")
        }

    private fun closeScope() =
        closeSendScope()

    private val model: SendModel
        get() = sendScope().get()

    private fun onSendComplete() =
        finishFlow()

    override fun onSheetClosed() {
        finishFlow()
    }
}
