package piuk.blockchain.android.ui.recurringbuy

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentRecurringBuyOnBoardingBinding
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class RecurringBuyOnBoardingFragment : Fragment() {

    private var _binding: FragmentRecurringBuyOnBoardingBinding? = null
    private val binding: FragmentRecurringBuyOnBoardingBinding
        get() = _binding!!

    private val recurringBuyInfo: RecurringBuyInfo by unsafeLazy {
        arguments?.getParcelable(DATA) as? RecurringBuyInfo ?: throw IllegalStateException(
            "RecurringBuyInfo not provided"
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecurringBuyOnBoardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.apply {
            title.text = recurringBuyInfo.title
            subtitle.text = buildColorStrings()
            mainImage.visibleIf { recurringBuyInfo.hasImage }
        }
    }

    private fun buildColorStrings(): Spannable {
        val subtitle1 = recurringBuyInfo.subtitle1
        val subtitle2 = recurringBuyInfo.subtitle2 ?: return SpannableStringBuilder(subtitle1)
        val sb = SpannableStringBuilder()
        sb.append(subtitle1)
            .append(subtitle2)
            .setSpan(
                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.grey_800)),
                0, subtitle1.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        return sb
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val DATA = "recurring_tx_buy_info"

        fun newInstance(recurringBuyOnBoardingInfo: RecurringBuyInfo): RecurringBuyOnBoardingFragment {
            return RecurringBuyOnBoardingFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(DATA, recurringBuyOnBoardingInfo)
                }
            }
        }
    }
}