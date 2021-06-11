package piuk.blockchain.android.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.annotations.CommonCode
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.MobileNoticeDialogBinding
import piuk.blockchain.android.ui.auth.MobileNoticeDialog

@CommonCode("One of many almost identical bottom dialogs")
class MobileNoticeDialogFragment : BottomSheetDialogFragment() {

    private var _binding: MobileNoticeDialogBinding? = null
    private val binding: MobileNoticeDialogBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)
        val themedInflater = inflater.cloneInContext(contextThemeWrapper)
        _binding = MobileNoticeDialogBinding.inflate(themedInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            dialogTitle.text = arguments?.getString(KEY_TITLE)
            dialogBody.text = arguments?.getString(KEY_MESSAGE)
            buttonCta.text = arguments?.getString(KEY_CTA_BUTTON_TEXT)
            buttonCta.setOnClickListener {
                arguments?.getString(KEY_CTA_LINK)?.let {
                    context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                }
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        val TAG = MobileNoticeDialogFragment::class.java.simpleName!!

        private const val KEY_TITLE = "title"
        private const val KEY_MESSAGE = "message"
        private const val KEY_CTA_BUTTON_TEXT = "cta_button_text"
        private const val KEY_CTA_LINK = "cta_link"

        fun newInstance(
            mobileNoticeDialog: MobileNoticeDialog
        ): MobileNoticeDialogFragment {
            val fragment = MobileNoticeDialogFragment()

            fragment.arguments = Bundle().apply {
                putString(KEY_TITLE, mobileNoticeDialog.title)
                putString(KEY_MESSAGE, mobileNoticeDialog.body)
                putString(KEY_CTA_BUTTON_TEXT, mobileNoticeDialog.ctaText)
                putString(KEY_CTA_LINK, mobileNoticeDialog.ctaLink)
            }
            return fragment
        }
    }
}
