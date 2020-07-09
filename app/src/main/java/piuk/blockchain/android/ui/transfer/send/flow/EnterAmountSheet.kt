package piuk.blockchain.android.ui.transfer.send.flow

import android.text.Editable
import android.view.View
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import kotlinx.android.synthetic.main.dialog_send_enter_amount.view.*
import kotlinx.android.synthetic.main.dialog_send_enter_amount.view.cta_button
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.transfer.send.SendInputSheet
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.androidcoreui.utils.helperfunctions.AfterTextChangedWatcher
import timber.log.Timber
import java.text.DecimalFormatSymbols

class EnterAmountSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_enter_amount

    private var state: SendState = SendState()

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! EnterAmountSheet")

        with(dialogView) {
            cta_button.isEnabled = newState.nextEnabled

            max_available.text = newState.availableBalance.toStringWithSymbol()
        }
//        view.error_msg
        state = newState
    }

    override fun initControls(view: View) {
        view.enter_amount.addTextChangedListener(amountTextWatcher)
        view.use_max.setOnClickListener { onUseMaxClick() }
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    private val amountTextWatcher = object : AfterTextChangedWatcher() {
        override fun afterTextChanged(s: Editable?) {
            val txtAmount = s.toString()

            val r = textToCryptoValue(txtAmount, state.sendingAccount.asset)
            model.process(SendIntent.SendAmountChanged(r))
        }
    }

    private fun onUseMaxClick() {
        dialogView.enter_amount.setText(state.availableBalance.toStringWithoutSymbol())
    }

    private fun onCtaClick() =
        model.process(SendIntent.PrepareTransaction)

    companion object {
        fun newInstance(): EnterAmountSheet =
            EnterAmountSheet()
    }
}

private fun textToCryptoValue(text: String, ccy: CryptoCurrency): CryptoValue {
    if (text.isEmpty()) return CryptoValue.zero(ccy)

    val decimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator.toString()

    val amount = text.trim { it <= ' ' }
        .replace(" ", "")
        .replace(decimalSeparator, ".")

    return CryptoValue.fromMajor(ccy, amount.toBigDecimal())
}
