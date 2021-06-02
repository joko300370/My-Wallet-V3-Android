package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.TxConfirmation
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.databinding.ItemSendConfirmErrorNoticeBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context

class ConfirmInfoItemValidationStatusDelegate<in T> :
    AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        return (items[position] as? TxConfirmationValue)?.confirmation == TxConfirmation.ERROR_NOTICE
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ViewHolder(
            ItemSendConfirmErrorNoticeBinding.inflate(LayoutInflater.from(parent.context), parent, false), parent
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as ViewHolder).bind(
        items[position] as TxConfirmationValue.ErrorNotice
    )

    class ViewHolder(
        private val binding: ItemSendConfirmErrorNoticeBinding,
        private val parentView: ViewGroup
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TxConfirmationValue.ErrorNotice) {
            if (parentView is RecyclerView) {
                parentView.smoothScrollToPosition(parentView.adapter!!.itemCount - 1)
            }
            binding.errorMsg.text = item.toText(context)
        }

        // By the time we are on the confirmation screen most of these possible error should have been
        // filtered out. A few remain possible, because BE failures or BitPay invoices, thus:
        private fun TxConfirmationValue.ErrorNotice.toText(ctx: Context) =
            when (this.status) {
                ValidationState.CAN_EXECUTE -> throw IllegalStateException("Displaying OK in error status")
                ValidationState.UNINITIALISED -> throw IllegalStateException("Displaying OK in error status")
                ValidationState.INSUFFICIENT_FUNDS -> ctx.getString(R.string.confirm_status_msg_insufficient_funds)
                ValidationState.INSUFFICIENT_GAS -> ctx.getString(R.string.confirm_status_msg_insufficient_gas)
                ValidationState.OPTION_INVALID -> ctx.getString(R.string.confirm_status_msg_option_invalid)
                ValidationState.INVOICE_EXPIRED -> ctx.getString(R.string.confirm_status_msg_invoice_expired)
                ValidationState.UNDER_MIN_LIMIT -> {
                    this.money?.toStringWithSymbol()?.let {
                        ctx.getString(R.string.min_with_value, it)
                    } ?: ctx.getString(R.string.fee_options_sat_byte_min_error)
                }
                ValidationState.INVALID_AMOUNT -> ctx.getString(R.string.fee_options_invalid_amount)
                ValidationState.HAS_TX_IN_FLIGHT -> ctx.getString(R.string.send_error_tx_in_flight)
                else -> ctx.getString(R.string.confirm_status_msg_unexpected_error)
            }
    }
}
