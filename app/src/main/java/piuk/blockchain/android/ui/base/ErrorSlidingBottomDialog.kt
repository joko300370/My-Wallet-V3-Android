package piuk.blockchain.android.ui.base

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ErrorSlidingBottomDialogBinding
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.lang.IllegalStateException

class ErrorSlidingBottomDialog : SlidingModalBottomDialog<ErrorSlidingBottomDialogBinding>() {

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): ErrorSlidingBottomDialogBinding =
        ErrorSlidingBottomDialogBinding.inflate(inflater, container, false)

    private val errorDialogData: ErrorDialogData by unsafeLazy {
        arguments?.getParcelable(ERROR_DIALOG_DATA_KEY) as? ErrorDialogData
            ?: throw IllegalStateException("No Dialog date provided")
    }

    override fun initControls(binding: ErrorSlidingBottomDialogBinding) {
        binding.title.text = errorDialogData.title
        binding.description.text = errorDialogData.description
        binding.ctaButton.text = errorDialogData.buttonText

        binding.ctaButton.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        private const val ERROR_DIALOG_DATA_KEY = "ERROR_DIALOG_DATA_KEY"

        fun newInstance(errorDialogData: ErrorDialogData): ErrorSlidingBottomDialog =
            ErrorSlidingBottomDialog().apply {
                arguments = Bundle().apply { putParcelable(ERROR_DIALOG_DATA_KEY, errorDialogData) }
            }

        fun newInstance(context: Context): ErrorSlidingBottomDialog =
            ErrorSlidingBottomDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(
                        ERROR_DIALOG_DATA_KEY,
                        ErrorDialogData(
                            context.getString(R.string.ops),
                            context.getString(R.string.something_went_wrong_try_again),
                            context.getString(R.string.ok_cap)
                        )
                    )
                }
            }
    }
}

@Parcelize
data class ErrorDialogData(val title: String, val description: String, val buttonText: String) : Parcelable