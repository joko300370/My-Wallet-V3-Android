package piuk.blockchain.android.ui.transfer.send.flow

import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import kotlinx.android.synthetic.main.dialog_send_password.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.transfer.send.FlowInputSheet
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.ui.transfer.send.SendStep
import timber.log.Timber

class EnterSecondPasswordSheet(
    host: SlidingModalBottomDialog.Host
) : FlowInputSheet(host) {

    override val layoutResource: Int = R.layout.dialog_send_password

    override fun render(newState: SendState) {
        require(newState.currentStep == SendStep.ENTER_PASSWORD)

        if (!newState.nextEnabled && newState.secondPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Incorrect password", Toast.LENGTH_SHORT).show()
        }
        Timber.d("!SEND!> Rendering! EnterSecondPasswordSheet")
    }

    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick(view) }
        view.password_input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                onCtaClick(view)
            }
            true
        }
    }

    private fun onCtaClick(view: View) {
        model.process(SendIntent.ValidatePassword(view.password_input.text.toString()))
    }
}
