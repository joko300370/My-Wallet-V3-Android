package piuk.blockchain.android.ui.transfer.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import com.blockchain.koin.scopedInject
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import info.blockchain.balance.CryptoCurrency
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_transfer.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import timber.log.Timber

class TransferSendFragment : Fragment() {

    private val disposables = CompositeDisposable()
    private val coincore: Coincore by scopedInject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_transfer)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_go.setOnClickListener { startSendFlow() }
    }

    private fun startSendFlow() {
        // Get the selected crypto account - for now, just grab the default non-custodial
        // when we have a configurable account selector, and have placed it in this fragment, we'll use that
        // We also need to know if the selected account has a second password, so we'll query coincore for that

        disposables += Singles.zip(
            coincore[CryptoCurrency.ETHER].defaultAccount(),
            coincore.requireSecondPassword()
        ) { account, secondPassword ->
            initialiseSendFlow(account, secondPassword)
        }.subscribeBy(
            onError = {
                Timber.e("Unable to configure send flow, aborting. e == $it")
                activity?.finish()
            }
        )
    }

    private fun initialiseSendFlow(account: CryptoSingleAccount, passwordRequired: Boolean) {
        // Create the send scope
        openScope()
        // Get the model
        model.apply {
            // Trigger intent to set initial state: source account & password required
            process(InitialiseWithAccount(account, passwordRequired))
            disposables += state.subscribeBy(
                onNext = { handleStateChange(it) },
                onError = { Timber.e("Send state is broken: $it")}
            )
        }
    }

    private var currentStep: SendStep = SendStep.ZERO

    private fun handleStateChange(newState: SendState) {
        if(currentStep != newState.currentStep) {
            currentStep = newState.currentStep
            when (currentStep) {
                SendStep.ZERO -> onSendComplete()
            }
        }
    }

    private fun showFlowStep(step: SendStep) {
        showBottomSheet(
            when (step) {
                SendStep.ZERO -> null
                SendStep.ENTER_PASSWORD -> EnterSecondPasswordSheet.newInstance()
                SendStep.ENTER_ADDRESS -> EnterTargetAddressSheet.newInstance()
                SendStep.ENTER_AMOUNT -> null
                SendStep.CONFIRM_DETAIL -> null
                SendStep.IN_PROGRESS -> null
                SendStep.SEND_ERROR -> null
                SendStep.SEND_COMPLETE -> null
            }
        )
    }
    private fun openScope() =
        try {
            createSendScope()
        } catch(e: Throwable) {
            Timber.d("wtf? $e")
        }

    private fun closeScope() =
        closeSendScope()

    private val model: SendModel
        get() = sendScope().get()

    private fun onSendComplete() {
        disposables.clear()
        closeScope()
//        activity?.finish()
    }

    @UiThread
    fun showBottomSheet(bottomSheet: BottomSheetDialogFragment?) =
        bottomSheet?.show(childFragmentManager, "BOTTOM_SHEET")

    companion object {
        fun newInstance() = TransferSendFragment()
    }
}
