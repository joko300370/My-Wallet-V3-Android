package piuk.blockchain.android.ui.auth.newlogin

import android.view.LayoutInflater
import android.view.ViewGroup
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.AuthNewLoginConfirmSheetBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class AuthConfirmationSheet(
    private val isApproved: Boolean
) : SlidingModalBottomDialog<AuthNewLoginConfirmSheetBinding>() {

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): AuthNewLoginConfirmSheetBinding =
        AuthNewLoginConfirmSheetBinding.inflate(inflater, container, false)

    override fun initControls(binding: AuthNewLoginConfirmSheetBinding) {
        with(binding) {
            if (isApproved) {
                newLoginDeviceIcon.setImageResource(R.drawable.ic_login_approved)
                title.text = resources.getString(R.string.auth_new_login_approved_title)
                label.text = resources.getString(R.string.auth_new_login_approved_description)
            } else {
                newLoginDeviceIcon.setImageResource(R.drawable.ic_login_denied)
                title.text = resources.getString(R.string.auth_new_login_denied_title)
                label.text = resources.getString(R.string.auth_new_login_denied_description)
            }
            okButton.setOnClickListener {
                dismiss()
            }
        }
    }
}