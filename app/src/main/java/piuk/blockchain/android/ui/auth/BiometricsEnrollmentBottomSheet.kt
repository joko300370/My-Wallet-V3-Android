package piuk.blockchain.android.ui.auth

import android.view.LayoutInflater
import android.view.ViewGroup
import piuk.blockchain.android.databinding.DialogSheetEnrollBiometricsBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class BiometricsEnrollmentBottomSheet : SlidingModalBottomDialog<DialogSheetEnrollBiometricsBinding>() {
    interface Host : SlidingModalBottomDialog.Host {
        fun enrollBiometrics()
        fun cancel()
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a BiometricsEnrollmentBottomSheet.Host"
        )
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetEnrollBiometricsBinding =
        DialogSheetEnrollBiometricsBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogSheetEnrollBiometricsBinding) {
        binding.biometricEnable.setOnClickListener {
            host.enrollBiometrics()
        }

        binding.biometricCancel.setOnClickListener {
            dismiss()
            host.cancel()
        }
    }

    companion object {
        fun newInstance(): BiometricsEnrollmentBottomSheet = BiometricsEnrollmentBottomSheet()
    }
}