package piuk.blockchain.android.ui.kyc.email.entry

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.blockchain.koin.scopedInject
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.change_email_bottom_sheet.view.*
import kotlinx.android.synthetic.main.dialog_tx_flow_enter_amount.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet
import piuk.blockchain.android.ui.customviews.PrefixedOrSuffixedEditText
import piuk.blockchain.android.util.ViewUtils.hideKeyboard

class ChangeEmailAddressBottomSheet : MviBottomSheet<EmailVeriffModel, EmailVeriffIntent, EmailVeriffState>() {
    override val model: EmailVeriffModel by scopedInject()

    override fun render(newState: EmailVeriffState) {
        dialogView.edit_email_input.setText(newState.email?.address)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FloatingBottomSheet)
    }

    private val imm: InputMethodManager by lazy {
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override val layoutResource: Int
        get() = R.layout.change_email_bottom_sheet

    override fun initControls(view: View) {
        model.process(EmailVeriffIntent.FetchEmail)
        showKeyboard()
        view.edit_email_input as EditText { _, keyCode, _ ->
            if (keyCode == KEYCODE_BACK) {
                hideKeyboard(requireActivity())
                dismiss()
            }
            return@setOnKeyListener false
        }
    }

    private fun showKeyboard() {
        val inputView = dialogView.findViewById<TextInputEditText>(
            R.id.edit_email_input
        )
        inputView?.run {
            requestFocus()
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }
}