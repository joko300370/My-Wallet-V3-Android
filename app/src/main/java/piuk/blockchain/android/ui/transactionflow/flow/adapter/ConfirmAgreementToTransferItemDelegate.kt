package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.content.res.Resources
import android.graphics.Typeface.BOLD
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRates
import info.blockchain.balance.Money
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.databinding.ItemSendConfirmAgreementTransferBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.setThrottledCheckedChange

class ConfirmAgreementToTransferItemDelegate<in T>(
    private val model: TransactionModel,
    private val exchangeRates: ExchangeRates,
    private val selectedCurrency: String,
    private val assetResources: AssetResources
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean =
        (items[position] as? TxConfirmationValue.TxBooleanConfirmation<*>)?.data?.let {
            it is Money
        } ?: false

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AgreementTextItemViewHolder(
            ItemSendConfirmAgreementTransferBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            exchangeRates,
            selectedCurrency
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AgreementTextItemViewHolder).bind(
        items[position] as TxConfirmationValue.TxBooleanConfirmation<Money>,
        model,
        assetResources
    )
}

private class AgreementTextItemViewHolder(
    private val binding: ItemSendConfirmAgreementTransferBinding,
    private val exchangeRates: ExchangeRates,
    private val selectedCurrency: String
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: TxConfirmationValue.TxBooleanConfirmation<Money>,
        model: TransactionModel,
        assetResources: AssetResources
    ) {
        binding.apply {
            confirmDetailsCheckbox.setText(
                agreementText(
                    item.data ?: return,
                    exchangeRates,
                    selectedCurrency,
                    context.resources,
                    assetResources
                ),
                TextView.BufferType.SPANNABLE
            )

            confirmDetailsCheckbox.isChecked = item.value

            confirmDetailsCheckbox.setThrottledCheckedChange { isChecked ->
                model.process(TransactionIntent.ModifyTxOption(item.copy(value = isChecked)))
            }
        }
    }

    private fun agreementText(
        amount: Money,
        exchangeRates: ExchangeRates,
        selectedCurrency: String,
        resources: Resources,
        assetResources: AssetResources
    ): SpannableStringBuilder {
        val introToHolding = resources.getString(R.string.send_confirmation_interest_holding_period_1)
        val amountInBold =
            amount.toFiat(exchangeRates, selectedCurrency).toStringWithSymbol()
        val outroToHolding = context.resources.getString(
            R.string.send_confirmation_interest_holding_period_2,
            amount.toStringWithSymbol(),
            context.resources.getString(assetResources.assetNameRes((amount as CryptoValue).currency))
        )
        val sb = SpannableStringBuilder()
            .append(introToHolding)
            .append(amountInBold)
            .append(outroToHolding)
        sb.setSpan(StyleSpan(BOLD), introToHolding.length,
            introToHolding.length + amountInBold.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return sb
    }
}