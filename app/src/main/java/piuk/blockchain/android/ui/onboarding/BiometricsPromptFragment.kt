package piuk.blockchain.android.ui.onboarding

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_fingerprint_prompt.*
import piuk.blockchain.android.R

class BiometricsPromptFragment : Fragment() {

    private var listener: OnFragmentInteractionListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_fingerprint_prompt, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        button_enable.setOnClickListener { listener?.onEnableFingerprintClicked() }
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
        fun onEnableFingerprintClicked()
    }

    companion object {
        fun newInstance(): BiometricsPromptFragment {
            return BiometricsPromptFragment()
        }
    }
}
