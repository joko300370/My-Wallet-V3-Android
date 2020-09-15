package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.content.res.Resources
import android.graphics.Typeface.BOLD
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import info.blockchain.balance.ExchangeRates
import info.blockchain.balance.Money
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_send_confirm_agreement_transfer.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class ConfirmAgreementToTransferItemDelegate<in T>(
    private val model: TransactionModel,
    private val exchangeRates: ExchangeRates,
    private val selectedCurrency: String
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean =
        (items[position] as? TxOptionValue.TxBooleanOption<*>)?.data?.let {
            it is Money
        } ?: false

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AgreementTextItemViewHolder(
            parent.inflate(R.layout.item_send_confirm_agreement_transfer),
            exchangeRates,
            selectedCurrency
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AgreementTextItemViewHolder).bind(
        items[position] as TxOptionValue.TxBooleanOption<Money>,
        model
    )
}

private class AgreementTextItemViewHolder(
    val parent: View,
    private val exchangeRates: ExchangeRates,
    private val selectedCurrency: String
) :
    RecyclerView.ViewHolder(parent),
    LayoutContainer {

    override val containerView: View?
        get() = itemView

    fun bind(
        item: TxOptionValue.TxBooleanOption<Money>,
        model: TransactionModel
    ) {
        itemView.confirm_details_checkbox.setText(agreementText(
            item.data ?: return,
            exchangeRates,
            selectedCurrency, parent.resources),
            TextView.BufferType.SPANNABLE
        )

        itemView.confirm_details_checkbox.isChecked = item.value

        itemView.confirm_details_checkbox.setOnCheckedChangeListener { view, isChecked ->
            model.process(TransactionIntent.ModifyTxOption(item.copy(value = isChecked)))
            view.isEnabled = false
        }
    }

    private fun agreementText(
        amount: Money,
        exchangeRates: ExchangeRates,
        selectedCurrency: String,
        resources: Resources
    ): SpannableStringBuilder {
        val introToHolding = resources.getString(R.string.send_confirmation_interest_holding_period_1)
        val amountInBold =
            amount.toFiat(exchangeRates, selectedCurrency).toStringWithSymbol()
        val outroToHolding = resources.getString(R.string.send_confirmation_interest_holding_period_2,
            amount.toStringWithSymbol())
        val sb = SpannableStringBuilder()
        sb.append(introToHolding)
        sb.append(amountInBold)
        sb.setSpan(StyleSpan(BOLD), introToHolding.length,
            introToHolding.length + amountInBold.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append(outroToHolding)
        return sb
    }
}