package piuk.blockchain.android.ui.activity.detail.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import info.blockchain.wallet.multiaddress.TransactionSummary
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_activity_detail_info.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.activity.detail.Action
import piuk.blockchain.android.ui.activity.detail.ActivityDetailsType
import piuk.blockchain.android.ui.activity.detail.Amount
import piuk.blockchain.android.ui.activity.detail.BuyCryptoWallet
import piuk.blockchain.android.ui.activity.detail.BuyFee
import piuk.blockchain.android.ui.activity.detail.BuyPaymentMethod
import piuk.blockchain.android.ui.activity.detail.BuyPurchaseAmount
import piuk.blockchain.android.ui.activity.detail.BuyTransactionId
import piuk.blockchain.android.ui.activity.detail.CancelAction
import piuk.blockchain.android.ui.activity.detail.Created
import piuk.blockchain.android.ui.activity.detail.Description
import piuk.blockchain.android.ui.activity.detail.Fee
import piuk.blockchain.android.ui.activity.detail.FeeForTransaction
import piuk.blockchain.android.ui.activity.detail.From
import piuk.blockchain.android.ui.activity.detail.HistoricValue
import piuk.blockchain.android.ui.activity.detail.To
import piuk.blockchain.android.ui.activity.detail.Value
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.assetName
import piuk.blockchain.android.util.extensions.toFormattedString
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class ActivityDetailInfoItemDelegate<in T> : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position] as ActivityDetailsType
        return item !is Action && item !is Description && item !is CancelAction
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        InfoItemViewHolder(
            parent.inflate(R.layout.item_activity_detail_info)
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as InfoItemViewHolder).bind(
        items[position] as ActivityDetailsType
    )
}

private class InfoItemViewHolder(var parent: View) : RecyclerView.ViewHolder(parent),
    LayoutContainer {
    override val containerView: View?
        get() = itemView

    fun bind(item: ActivityDetailsType) {
        itemView.item_activity_detail_title.text = getHeaderForType(item)
        itemView.item_activity_detail_description.text = getValueForType(item)
    }

    private fun getHeaderForType(infoType: ActivityDetailsType): String =
        parent.context.getString(
            when (infoType) {
                is Created -> R.string.activity_details_created
                is Amount -> R.string.activity_details_amount
                is Fee -> R.string.activity_details_fee
                is Value -> R.string.activity_details_value
                is HistoricValue -> {
                    when (infoType.direction) {
                        TransactionSummary.Direction.SENT,
                        TransactionSummary.Direction.SELL ->
                            R.string.activity_details_historic_sent
                        TransactionSummary.Direction.RECEIVED,
                        TransactionSummary.Direction.BUY ->
                            R.string.activity_details_historic_received
                        TransactionSummary.Direction.TRANSFERRED,
                        TransactionSummary.Direction.SWAP
                        -> R.string.activity_details_historic_transferred
                    }
                }
                is To -> R.string.activity_details_to
                is From -> R.string.activity_details_from
                is FeeForTransaction -> R.string.activity_details_transaction_fee
                is BuyFee -> R.string.activity_details_buy_fees
                is BuyPurchaseAmount -> R.string.activity_details_buy_purchase_amount
                is BuyTransactionId -> R.string.activity_details_buy_tx_id
                is BuyCryptoWallet -> R.string.activity_details_buy_sending_to
                is BuyPaymentMethod -> R.string.activity_details_buy_payment_method
                else -> R.string.empty
            })

    private fun getValueForType(infoType: ActivityDetailsType): String =
        when (infoType) {
            is Created -> infoType.date.toFormattedString()
            is Amount -> infoType.cryptoValue.toStringWithSymbol()
            is Fee -> infoType.feeValue?.toStringWithSymbol() ?: parent.context.getString(
                R.string.activity_details_fee_load_fail)
            is Value -> infoType.currentFiatValue?.toStringWithSymbol() ?: parent.context.getString(
                R.string.activity_details_value_load_fail)
            is HistoricValue -> infoType.fiatAtExecution?.toStringWithSymbol()
                ?: parent.context.getString(
                    R.string.activity_details_historic_value_load_fail)
            is To -> infoType.toAddress ?: parent.context.getString(
                R.string.activity_details_to_load_fail)
            is From -> infoType.fromAddress ?: parent.context.getString(
                R.string.activity_details_from_load_fail)
            is FeeForTransaction -> {
                when (infoType.direction) {
                    TransactionSummary.Direction.SENT -> parent.context.getString(
                        R.string.activity_details_transaction_fee_send,
                        infoType.cryptoValue.toStringWithSymbol())
                    else -> parent.context.getString(
                        R.string.activity_details_transaction_fee_unknown)
                }
            }
            is BuyFee -> infoType.feeValue.toStringWithSymbol()
            is BuyPurchaseAmount -> infoType.fundedFiat.toStringWithSymbol()
            is BuyTransactionId -> infoType.txId
            is BuyCryptoWallet -> parent.context.getString(R.string.custodial_wallet_default_label,
                parent.context.getString(infoType.crypto.assetName()))
            is BuyPaymentMethod ->
                if (infoType.paymentDetails.paymentMethodId == PaymentMethod.BANK_PAYMENT_ID) {
                    parent.context.getString(R.string.checkout_bank_transfer_label)
                } else if (infoType.paymentDetails.endDigits != null &&
                    infoType.paymentDetails.label != null) {
                    parent.context.getString(R.string.common_hyphenated_strings,
                        infoType.paymentDetails.label,
                        infoType.paymentDetails.endDigits)
                } else {
                    parent.context.getString(
                        R.string.activity_details_payment_load_fail)
                }
            else -> ""
        }
}
