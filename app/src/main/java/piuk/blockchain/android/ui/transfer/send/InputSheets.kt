package piuk.blockchain.android.ui.transfer.send

import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import kotlinx.android.synthetic.main.dialog_send_password.view.*
import kotlinx.android.synthetic.main.dialog_send_prototype.view.cta_button
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet
import timber.log.Timber

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

    override val layoutResource: Int = R.layout.dialog_send_password

    override fun render(newState: SendState) {
        if (!newState.processing && !newState.nextEnabled && newState.secondPassword.isEmpty() &&
            newState.currentStep == SendStep.ENTER_PASSWORD) {
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

    companion object {
        fun newInstance(): EnterSecondPasswordSheet =
            EnterSecondPasswordSheet()
    }
}

class EnterTargetAddressSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_address

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! EnterTargetAddressSheet")
    }

    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    private fun onCtaClick() {
        model.process(SendIntent.AddressSelected(
            object : CryptoAddress(CryptoCurrency.ETHER, "An Address") {
                override val label: String = "Label for ETH address"
            }
        ))
    }

    companion object {
        fun newInstance(): EnterTargetAddressSheet =
            EnterTargetAddressSheet()
    }
}

class EnterAmountSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_enter_amount

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! EnterAmountSheet")
    }

    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    private fun onCtaClick() {
        model.process(SendIntent.PrepareTransaction(
            CryptoValue.fromMinor(CryptoCurrency.ETHER, 1000000.toBigDecimal()))
        )
    }

    companion object {
        fun newInstance(): EnterAmountSheet =
            EnterAmountSheet()
    }
}

class ConfirmTransactionSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_confirm

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! ConfirmTransactionSheet")
    }

    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    private fun onCtaClick() {
        model.process(SendIntent.ExecuteTransaction)
    }

    companion object {
        fun newInstance(): ConfirmTransactionSheet =
            ConfirmTransactionSheet()
    }
}

class TransactionInProgressSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_in_progress

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! TransactionInProgressSheet")
    }

    override fun initControls(view: View) {}

    companion object {
        fun newInstance(): TransactionInProgressSheet =
            TransactionInProgressSheet()
    }
}

class TransactionErrorSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_error

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! TransactionErrorSheet")
    }

    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    private fun onCtaClick() {
        dismiss()
    }

    companion object {
        fun newInstance(): TransactionErrorSheet =
            TransactionErrorSheet()
    }
}

class TransactionCompleteSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_complete

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! TransactionCompleteSheet")
    }

    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    private fun onCtaClick() {
        dismiss()
    }

    companion object {
        fun newInstance(): TransactionCompleteSheet =
            TransactionCompleteSheet()
    }
}