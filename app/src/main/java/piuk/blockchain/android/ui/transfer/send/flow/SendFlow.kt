package piuk.blockchain.android.ui.transfer.send.flow

import androidx.annotation.UiThread
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendModel
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.ui.transfer.send.SendStep
import piuk.blockchain.android.ui.transfer.send.TransactionCompleteSheet
import piuk.blockchain.android.ui.transfer.send.TransactionInProgressSheet
import piuk.blockchain.android.ui.transfer.send.closeSendScope
import piuk.blockchain.android.ui.transfer.send.createSendScope
import piuk.blockchain.android.ui.transfer.send.sendScope
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber

class SendFlow(
    private val fragmentManager: FragmentManager,
    private val listener: Listener,
    private val disposables: CompositeDisposable,
    private val bottomSheetTag: String = SHEET_FRAGMENT_TAG,
    private val uiScheduler: Scheduler = AndroidSchedulers.mainThread()
) {

    interface Listener {
        fun onSendFlowFinished()
    }

    private var currentStep: SendStep = SendStep.ZERO

    fun startFlow(account: CryptoAccount, passwordRequired: Boolean) {
        // Create the send scope
        openScope()
        // Get the model
        model.apply {
            // Trigger intent to set initial state: source account & password required
            disposables += state.subscribeBy(
                onNext = { handleStateChange(it) },
                onError = { Timber.e("Send state is broken: $it") }
            )
            process(SendIntent.Initialise(account, passwordRequired))
        }
    }

    fun finishFlow() {
        listener.onSendFlowFinished()
        currentStep = SendStep.ZERO
        closeScope()
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
                SendStep.ENTER_PASSWORD -> EnterSecondPasswordSheet.newInstance()
                SendStep.ENTER_ADDRESS -> EnterTargetAddressSheet.newInstance()
                SendStep.ENTER_AMOUNT -> EnterAmountSheet.newInstance()
                SendStep.CONFIRM_DETAIL -> ConfirmTransactionSheet.newInstance()
                SendStep.IN_PROGRESS -> TransactionInProgressSheet.newInstance()
                SendStep.SEND_ERROR -> TransactionErrorSheet.newInstance()
                SendStep.SEND_COMPLETE -> TransactionCompleteSheet.newInstance()
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

    private val model: SendModel by unsafeLazy {
        sendScope().get<SendModel>()
    }

    private fun onSendComplete() =
        finishFlow()

    @UiThread
    private fun replaceBottomSheet(bottomSheet: BottomSheetDialogFragment?) {
        fragmentManager.findFragmentByTag(bottomSheetTag)?.let {
            fragmentManager.beginTransaction().remove(it).commitNow()
        }
        bottomSheet?.show(fragmentManager, bottomSheetTag)
    }

    companion object {
        private const val SHEET_FRAGMENT_TAG = "BOTTOM_SHEET"
    }
}
