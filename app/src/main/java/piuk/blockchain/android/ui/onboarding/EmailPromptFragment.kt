package piuk.blockchain.android.ui.onboarding

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import piuk.blockchain.android.databinding.FragmentEmailPromptBinding

class EmailPromptFragment : Fragment() {

    private var listener: OnFragmentInteractionListener? = null

    private var _binding: FragmentEmailPromptBinding? = null
    private val binding: FragmentEmailPromptBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmailPromptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            buttonEnable.setOnClickListener { listener?.onVerifyEmailClicked() }
            emailAddress.text = arguments?.emailAddress
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    internal interface OnFragmentInteractionListener {
        fun onVerifyEmailClicked()
    }

    companion object {

        private const val ARGUMENT_EMAIL = "email"

        fun newInstance(email: String): EmailPromptFragment {
            val fragment = EmailPromptFragment()

            fragment.arguments = Bundle().apply {
                emailAddress = email
            }
            return fragment
        }

        private var Bundle?.emailAddress: String
            get() = this?.getString(ARGUMENT_EMAIL) ?: ""
            set(v) = this?.putString(ARGUMENT_EMAIL, v) ?: throw NullPointerException()
    }
}
