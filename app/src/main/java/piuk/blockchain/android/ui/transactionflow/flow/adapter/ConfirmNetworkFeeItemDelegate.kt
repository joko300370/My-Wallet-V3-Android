package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.ui.urllinks.URL_TX_FEES
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.databinding.ItemSendConfirmNetworkFeeBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible

class ConfirmNetworkFeeItemDelegate<in T>(
    private val stringUtils: StringUtils,
    private val assetResources: AssetResources
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        return items[position] is TxConfirmationValue.NetworkFee
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        NetworkFeeItemViewHolder(
            ItemSendConfirmNetworkFeeBinding.inflate(LayoutInflater.from(parent.context), parent, false), stringUtils
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as NetworkFeeItemViewHolder).bind(
        items[position] as TxConfirmationValue.NetworkFee,
        assetResources
    )
}

private class NetworkFeeItemViewHolder(
    private val binding: ItemSendConfirmNetworkFeeBinding,
    private val stringUtils: StringUtils
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: TxConfirmationValue.NetworkFee,
        assetResources: AssetResources
    ) {
        val linksMap = mapOf<String, Uri>("send_tx_fees" to Uri.parse(URL_TX_FEES))

        with(binding) {
            confirmationFreeLabel.text =
                context.getString(R.string.tx_confirmation_network_fee, item.txFee.asset.displayTicker)

            if (item.txFee.fee.isZero) {
                confirmationFeeValue.gone()
                confirmationFeeLabel.visible()

                confirmationLearnMore.setText(getFreeFeesText(context, linksMap), TextView.BufferType.SPANNABLE)
            } else {
                confirmationFeeValue.text = item.txFee.fee.toStringWithSymbol()
                confirmationFeeValue.visible()
                confirmationFeeLabel.gone()

                confirmationLearnMore.setText(
                    getFeesText(
                        context,
                        linksMap,
                        assetResources.assetNameRes(item.txFee.asset)
                    ),
                    TextView.BufferType.SPANNABLE
                )
            }

            confirmationLearnMore.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun getFreeFeesText(context: Context, linksMap: Map<String, Uri>): SpannableStringBuilder {
        val introText = context.getString(R.string.tx_confirmation_free_fee_learn_more_1)
        val boldText = context.getString(R.string.tx_confirmation_free_fee_learn_more_2_1)
        val linkedText = stringUtils.getStringWithMappedAnnotations(
            R.string.tx_confirmation_free_fee_learn_more_3,
            linksMap,
            itemView.context
        )
        val sb = SpannableStringBuilder()
            .append(introText)
            .append(boldText)
            .append(linkedText)

        sb.setSpan(
            StyleSpan(Typeface.BOLD), introText.length,
            introText.length + boldText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return sb
    }

    private fun getFeesText(
        context: Context,
        linksMap: Map<String, Uri>,
        @StringRes assetName: Int
    ): SpannableStringBuilder {
        val boldText = context.getString(R.string.tx_confirmation_fee_learn_more_1)
        val networkText = context.getString(R.string.tx_confirmation_fee_learn_more_2, context.getString(assetName))
        val linkedText = stringUtils.getStringWithMappedAnnotations(
            R.string.tx_confirmation_fee_learn_more_3,
            linksMap,
            itemView.context
        )

        val sb = SpannableStringBuilder()
            .append(boldText)
            .append(networkText)
            .append(linkedText)

        sb.setSpan(StyleSpan(Typeface.BOLD), 0, boldText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        return sb
    }
}
