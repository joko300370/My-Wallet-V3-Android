package piuk.blockchain.android.ui.kyc.email.entry

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.DialogFragment
import com.blockchain.koin.scopedInject
import com.google.android.material.textfield.TextInputEditText
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ChangeEmailBottomSheetBinding
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet
import piuk.blockchain.android.ui.customviews.KeyPreImeEditText
import piuk.blockchain.android.util.AfterTextChangedWatcher

class EditEmailAddressBottomSheet :
    MviBottomSheet<EmailVeriffModel, EmailVeriffIntent, EmailVeriffState, ChangeEmailBottomSheetBinding>() {
    override val model: EmailVeriffModel by scopedInject()

    override fun render(newState: EmailVeriffState) {
        binding.save.isEnabled =
            newState.canUpdateEmail &&
                !newState.isLoading

        if (newState.emailChanged) {
            dismiss()
        }
        if (newState.emailInput == null && newState.email.address.isNotEmpty()) {
            binding.editEmailInput.apply {
                setText(newState.email.address)
                setSelection(newState.email.address.length)
            }
        }
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): ChangeEmailBottomSheetBinding {
        return ChangeEmailBottomSheetBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FloatingBottomSheet)
    }

    private val imm: InputMethodManager by lazy {
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override fun initControls(binding: ChangeEmailBottomSheetBinding) {
        model.process(EmailVeriffIntent.FetchEmail)
        showKeyboard()
        with(binding) {
            editEmailInput.keyImeChangeListener = object : KeyPreImeEditText.KeyImeChange {
                override fun onKeyIme(keyCode: Int, event: KeyEvent?) {
                    if (keyCode == KEYCODE_BACK) {
                        dismiss()
                    }
                }
            }
            editEmailInput.addTextChangedListener(object : AfterTextChangedWatcher() {
                override fun afterTextChanged(textEntered: Editable?) {
                    model.process(EmailVeriffIntent.UpdateEmailInput(textEntered?.toString().orEmpty()))
                }
            })

            save.setOnClickListener {
                model.process(EmailVeriffIntent.UpdateEmail)
            }
        }
    }

    private fun showKeyboard() {
        val inputView = binding.root.findViewById<TextInputEditText>(
            R.id.edit_email_input
        )
        inputView?.run {
            requestFocus()
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }
}