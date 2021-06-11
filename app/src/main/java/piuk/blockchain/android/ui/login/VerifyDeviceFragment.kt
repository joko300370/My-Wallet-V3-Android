package piuk.blockchain.android.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentVerifyDeviceBinding

class VerifyDeviceFragment : Fragment() {

    private var _binding: FragmentVerifyDeviceBinding? = null
    private val binding: FragmentVerifyDeviceBinding
        get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVerifyDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            backButton.setOnClickListener {
                parentFragmentManager.popBackStack()
            }
            verifyDeviceDescription.text = getString(R.string.verify_device_desc)
            openEmailButton.setOnClickListener {
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_EMAIL)
                    startActivity(Intent.createChooser(this, getString(R.string.security_centre_email_check)))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}