package piuk.blockchain.android.ui.transactionflow.flow

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogTxFlowPasswordBinding
import piuk.blockchain.android.ui.transactionflow.engine.TransactionErrorState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionStep
import timber.log.Timber

class EnterSecondPasswordSheet : TransactionFlowSheet<DialogTxFlowPasswordBinding>() {

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogTxFlowPasswordBinding =
        DialogTxFlowPasswordBinding.inflate(inflater, container, false)

    override fun render(newState: TransactionState) {
        require(newState.currentStep == TransactionStep.ENTER_PASSWORD)

        if (newState.errorState == TransactionErrorState.INVALID_PASSWORD) {
            Toast.makeText(requireContext(), getString(R.string.invalid_password), Toast.LENGTH_SHORT).show()
        }

        Timber.d("!TRANSACTION!> Rendering! EnterSecondPasswordSheet")
        cacheState(newState)
    }

    override fun initControls(binding: DialogTxFlowPasswordBinding) {
        binding.ctaButton.setOnClickListener { onCtaClick() }
        binding.passwordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                onCtaClick()
            }
            true
        }
    }

    private fun onCtaClick() {
        model.process(TransactionIntent.ValidatePassword(binding.passwordInput.text.toString()))
    }
}
