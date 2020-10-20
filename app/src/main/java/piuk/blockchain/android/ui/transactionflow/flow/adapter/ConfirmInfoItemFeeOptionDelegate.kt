package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.ui.urllinks.URL_TX_FEES
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_send_confirm_select_fee.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.FeeDetails
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.flow.formatWithExchange
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class ConfirmInfoItemFeeOptionDelegate<in T>(
    private val model: TransactionModel,
    private val activityContext: Activity,
    private val stringUtils: StringUtils
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        return items[position] is TxOptionValue.FeeSelection
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        FeeOptionViewHolder(parent.inflate(R.layout.item_send_confirm_select_fee))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as FeeOptionViewHolder).bind(items[position] as TxOptionValue.FeeSelection, model, activityContext,
        stringUtils)

    private class FeeOptionViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view), LayoutContainer {

        override val containerView: View?
            get() = itemView

        fun bind(
            item: TxOptionValue.FeeSelection,
            model: TransactionModel,
            activityContext: Activity,
            stringUtils: StringUtils
        ) {
            val availableOptions = item.availableLevels
            val selectedOption = item.selectedLevel

            with(itemView) {
                if (availableOptions.size > 1) {
                    fee_option_select_spinner.setupSpinner(selectedOption, model)
                    fee_switcher.displayedChild = SHOW_DROPDOWN
                } else {
                    fee_switcher.displayedChild = SHOW_STATIC
                }

                item.feeDetails?.let {
                    if (it is FeeDetails) {
                        fee_option_value.text = it.absoluteFee.formatWithExchange(item.exchange)
                        fee_option_value.setTextColor(ContextCompat.getColor(context, R.color.grey_800))
                    } else {
                        fee_option_value.text = context.getString(R.string.send_confirmation_insufficient_fee)
                        fee_option_value.setTextColor(ContextCompat.getColor(context, R.color.red_600))
                    }
                }

                val linksMap = mapOf<String, Uri>(
                    "send_tx_fees" to Uri.parse(URL_TX_FEES)
                )

                fee_learn_more.text = stringUtils.getStringWithMappedLinks(
                    R.string.send_confirmation_fee_learn_more,
                    linksMap,
                    activityContext
                )

                fee_learn_more.movementMethod = LinkMovementMethod.getInstance()
            }
        }

        // We can get away with a fixed list ATM, until we support CustomFees at least.
        private val optionList = listOf(
            itemView.context.getString(R.string.fee_options_priority),
            itemView.context.getString(R.string.fee_options_regular)
        )

        private fun AppCompatSpinner.setupSpinner(currentLevel: FeeLevel, model: TransactionModel) {

            val spinnerArrayAdapter: ArrayAdapter<String> =
                CustomPaddingArrayAdapter(
                    context,
                    android.R.layout.simple_spinner_dropdown_item,
                    optionList.toMutableList()
                )

            adapter = spinnerArrayAdapter
            // Assumption here - when we extend the list, this assumption will break
            val newSelection = if (currentLevel == FeeLevel.Priority) PRIORITY_FEE else REGULAR_FEE
            onItemSelectedListener = null
            setSelection(newSelection)
            post { addSpinnerListener(model) }
        }

        private fun AppCompatSpinner.addSpinnerListener(model: TransactionModel) {
            onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        model.process(
                            TransactionIntent.ModifyTxOption(
                                TxOptionValue.FeeSelection(
                                    selectedLevel = if (position == PRIORITY_FEE) {
                                        FeeLevel.Priority
                                    } else {
                                        FeeLevel.Regular
                                    }
                                )
                            ))
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        // do nothing
                    }
                }
        }

        companion object {
            private const val SHOW_DROPDOWN = 0
            private const val SHOW_STATIC = 1
            private const val PRIORITY_FEE = 0
            private const val REGULAR_FEE = 1
        }
    }
}

private class CustomPaddingArrayAdapter<T>(context: Context, layoutId: Int, items: MutableList<T>) :
    ArrayAdapter<T>(context, layoutId, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val view = super.getView(position, convertView, parent)
        val smallPadding = context.resources.getDimension(R.dimen.tiny_margin).toInt()
        view.setPadding(0, smallPadding, view.paddingRight, smallPadding)
        return view
    }
}