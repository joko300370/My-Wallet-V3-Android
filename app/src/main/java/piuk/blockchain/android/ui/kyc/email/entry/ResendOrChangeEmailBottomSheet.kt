package piuk.blockchain.android.ui.kyc.email.entry

import android.view.LayoutInflater
import android.view.ViewGroup
import piuk.blockchain.android.databinding.ResendOrEditEmailBottomSheetBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class ResendOrChangeEmailBottomSheet : SlidingModalBottomDialog<ResendOrEditEmailBottomSheetBinding>() {

    interface ResendOrChangeEmailHost : Host {
        fun resendEmail()
        fun editEmail()
    }

    override val host: ResendOrChangeEmailHost
        get() {
            check(parentFragment is ResendOrChangeEmailHost)
            return parentFragment as? ResendOrChangeEmailHost ?: throw IllegalStateException(
                "Host is not a ResendOrChangeEmailHost.Host"
            )
        }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): ResendOrEditEmailBottomSheetBinding =
        ResendOrEditEmailBottomSheetBinding.inflate(inflater, container, false)

    override fun initControls(binding: ResendOrEditEmailBottomSheetBinding) {
        binding.apply {
            editEmail.setOnClickListener {
                host.editEmail()
            }
            resend.setOnClickListener {
                host.resendEmail()
            }
        }
    }
}