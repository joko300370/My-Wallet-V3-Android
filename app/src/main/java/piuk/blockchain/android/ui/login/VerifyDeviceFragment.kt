package piuk.blockchain.android.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.Fragment
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentVerifyDeviceBinding

class VerifyDeviceFragment : Fragment() {

    private var _binding: FragmentVerifyDeviceBinding? = null
    private val binding: FragmentVerifyDeviceBinding
        get() = _binding!!

    lateinit var email: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVerifyDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let { bundle ->
            email = bundle.getString(EMAIL, "")
        }
        with(binding) {
            backButton.setOnClickListener {
                parentFragmentManager.popBackStack()
            }
            verifyDeviceDescription.text = prepareHighlightedStep()
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

    private fun prepareHighlightedStep() =
        SpannableString(getString(R.string.verify_device_desc, email)).apply {
            val span = indexOf(email)
            setSpan(
                ForegroundColorSpan(getColor(requireContext(), R.color.blue_600)),
                span,
                span + email.length,
                Spannable.SPAN_EXCLUSIVE_INCLUSIVE
            )
        }

    companion object {
        private const val EMAIL = "EMAIL"
        fun newInstance(email: String): VerifyDeviceFragment {
            val args = Bundle()
            args.putString(EMAIL, email)
            return VerifyDeviceFragment().apply {
                arguments = args
            }
        }
    }
}