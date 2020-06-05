package piuk.blockchain.android.ui.transfer.send

import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.dialog_send_prototype.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet

abstract class SendInputSheet : MviBottomSheet<SendModel, SendIntent, SendState>() {
    override val model: SendModel by sendInject()

    override fun onSheetHidden() {
        super.onSheetHidden()
        dialog?.let {
            onCancel(it)
        }
    }
}

class EnterSecondPasswordSheet : SendInputSheet() {

    override val layoutResource: Int = R.layout.dialog_send_prototype

    override fun render(newState: SendState) {
        Toast.makeText(context, "Rendering!", Toast.LENGTH_SHORT).show()
    }

    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    private fun onCtaClick() {
//        model.process(ValidatePasswordIntent())
    }

    companion object {
        fun newInstance(): EnterSecondPasswordSheet =
            EnterSecondPasswordSheet()
    }
}

class EnterTargetAddressSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_prototype

    override fun render(newState: SendState) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    private fun onCtaClick() {
//        model.process(ValidatePasswordIntent())
    }

    companion object {
        fun newInstance(): EnterTargetAddressSheet =
            EnterTargetAddressSheet()
    }
}

class EnterAmountSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_prototype

    override fun render(newState: SendState) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    private fun onCtaClick() {
//        model.process(ValidatePasswordIntent())
    }
}
