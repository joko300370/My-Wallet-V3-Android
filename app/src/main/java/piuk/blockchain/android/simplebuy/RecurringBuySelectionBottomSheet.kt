package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetRecurringBuyBinding
import piuk.blockchain.android.ui.base.HostedBottomSheet
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class RecurringBuySelectionBottomSheet : SlidingModalBottomDialog<DialogSheetRecurringBuyBinding>() {

    interface Host : HostedBottomSheet.Host {
        fun onIntervalSelected(interval: RecurringBuyFrequency)
    }

    private val interval: RecurringBuyFrequency by lazy {
        arguments?.getSerializable(PREVIOUS_SELECTED_STATE) as RecurringBuyFrequency
    }

    private var selectedFrequency: RecurringBuyFrequency = RecurringBuyFrequency.ONE_TIME

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetRecurringBuyBinding =
        DialogSheetRecurringBuyBinding.inflate(inflater, container, false)

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a RecurringBuySelectionBottomSheet.Host"
        )
    }

    override fun initControls(binding: DialogSheetRecurringBuyBinding) {
        with(binding) {
            recurringBuySelectionGroup.check(intervalToId(interval))
            recurringBuySelectionGroup.setOnCheckedChangeListener { _, checkedId ->
                selectedFrequency = idToInterval(checkedId)
            }
            recurringBuySelectCta.setOnClickListener {
                host.onIntervalSelected(selectedFrequency)
                dismiss()
            }
        }
    }

    private fun intervalToId(interval: RecurringBuyFrequency) =
        when (interval) {
            RecurringBuyFrequency.DAILY -> R.id.rb_daily
            RecurringBuyFrequency.ONE_TIME -> R.id.rb_one_time
            RecurringBuyFrequency.WEEKLY -> R.id.rb_weekly
            RecurringBuyFrequency.BI_WEEKLY -> R.id.rb_bi_weekly
            RecurringBuyFrequency.MONTHLY -> R.id.rb_monthly
        }

    private fun idToInterval(checkedId: Int) =
        when (checkedId) {
            R.id.rb_one_time -> RecurringBuyFrequency.ONE_TIME
            R.id.rb_daily -> RecurringBuyFrequency.DAILY
            R.id.rb_weekly -> RecurringBuyFrequency.WEEKLY
            R.id.rb_bi_weekly -> RecurringBuyFrequency.BI_WEEKLY
            R.id.rb_monthly -> RecurringBuyFrequency.MONTHLY
            else -> throw IllegalStateException("option selected RecurringBuyFrequency unknown")
        }

    companion object {
        private const val PREVIOUS_SELECTED_STATE = "recurring_buy_check"
        fun newInstance(interval: RecurringBuyFrequency): RecurringBuySelectionBottomSheet =
            RecurringBuySelectionBottomSheet().apply {
                arguments = Bundle().apply { putSerializable(PREVIOUS_SELECTED_STATE, interval) }
            }
    }
}