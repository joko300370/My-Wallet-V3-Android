package piuk.blockchain.android.ui.transfer.send

import android.view.View
import android.widget.Toast
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import kotlinx.android.synthetic.main.dialog_send_prototype.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAddress
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

    override val layoutResource: Int = R.layout.dialog_send_password

    override fun render(newState: SendState) {
        Toast.makeText(context, "Rendering!", Toast.LENGTH_SHORT).show()
    }

    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    private fun onCtaClick() {
        model.process(SendIntent.ValidatePassword("second password"))
    }

    companion object {
        fun newInstance(): EnterSecondPasswordSheet =
            EnterSecondPasswordSheet()
    }
}

class EnterTargetAddressSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_address

    override fun render(newState: SendState) {
        Toast.makeText(context, "Rendering!", Toast.LENGTH_SHORT).show()
    }

    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    private fun onCtaClick() {
        model.process(SendIntent.AddressSelected(
            object: CryptoAddress(CryptoCurrency.ETHER, "An Address") {
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
        Toast.makeText(context, "Rendering!", Toast.LENGTH_SHORT).show()
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
        fun newInstance(): EnterTargetAddressSheet =
            EnterTargetAddressSheet()
    }
}

class ConfirmTransactionSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_prototype

    override fun render(newState: SendState) {
        Toast.makeText(context, "Rendering!", Toast.LENGTH_SHORT).show()
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
    override val layoutResource: Int = R.layout.dialog_send_prototype

    override fun render(newState: SendState) {
        Toast.makeText(context, "Rendering!", Toast.LENGTH_SHORT).show()
    }

    override fun initControls(view: View) {
//        view.cta_button.setOnClickListener { onCtaClick() }
    }

    companion object {
        fun newInstance(): TransactionInProgressSheet =
            TransactionInProgressSheet()
    }
}

class TransactionErrorSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_prototype

    override fun render(newState: SendState) {
        Toast.makeText(context, "Rendering!", Toast.LENGTH_SHORT).show()
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
    override val layoutResource: Int = R.layout.dialog_send_prototype

    override fun render(newState: SendState) {
        Toast.makeText(context, "Rendering!", Toast.LENGTH_SHORT).show()
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