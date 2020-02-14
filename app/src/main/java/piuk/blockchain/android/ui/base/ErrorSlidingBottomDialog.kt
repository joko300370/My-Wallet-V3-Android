package piuk.blockchain.android.ui.base

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.error_sliding_bottom_dialog.view.*
import piuk.blockchain.android.R
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.lang.IllegalStateException

class ErrorSlidingBottomDialog : SlidingModalBottomDialog() {
    override val layoutResource: Int
        get() = R.layout.error_sliding_bottom_dialog

    private val errorDialogData: ErrorDialogData by unsafeLazy {
        arguments?.getParcelable(ERROR_DIALOG_DATA_KEY) as? ErrorDialogData
            ?: throw IllegalStateException("No Dialog date provided")
    }

    override fun initControls(view: View) {
        view.title.text = errorDialogData.title
        view.description.text = errorDialogData.description
        view.cta_button.text = errorDialogData.buttonText

        view.cta_button.setOnClickListener {
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
                    putParcelable(ERROR_DIALOG_DATA_KEY,
                        ErrorDialogData(
                            context.getString(R.string.ops),
                            context.getString(R.string.something_went_wrong_try_again),
                            context.getString(R.string.ok_cap)
                        ))
                }
            }
    }
}

@Parcelize
data class ErrorDialogData(val title: String, val description: String, val buttonText: String) : Parcelable