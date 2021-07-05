package piuk.blockchain.android.ui.activity.detail.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.utils.toFormattedString
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemListInfoRowBinding
import piuk.blockchain.android.simplebuy.toHumanReadableRecurringBuy
import piuk.blockchain.android.simplebuy.toHumanReadableRecurringDate
import piuk.blockchain.android.ui.activity.detail.Action
import piuk.blockchain.android.ui.activity.detail.ActivityDetailsType
import piuk.blockchain.android.ui.activity.detail.Amount
import piuk.blockchain.android.ui.activity.detail.BuyCryptoWallet
import piuk.blockchain.android.ui.activity.detail.BuyFee
import piuk.blockchain.android.ui.activity.detail.BuyPaymentMethod
import piuk.blockchain.android.ui.activity.detail.BuyPurchaseAmount
import piuk.blockchain.android.ui.activity.detail.Created
import piuk.blockchain.android.ui.activity.detail.Description
import piuk.blockchain.android.ui.activity.detail.Fee
import piuk.blockchain.android.ui.activity.detail.FeeAmount
import piuk.blockchain.android.ui.activity.detail.FeeForTransaction
import piuk.blockchain.android.ui.activity.detail.From
import piuk.blockchain.android.ui.activity.detail.HistoricValue
import piuk.blockchain.android.ui.activity.detail.NetworkFee
import piuk.blockchain.android.ui.activity.detail.NextPayment
import piuk.blockchain.android.ui.activity.detail.RecurringBuyFrequency
import piuk.blockchain.android.ui.activity.detail.SellCryptoWallet
import piuk.blockchain.android.ui.activity.detail.SellPurchaseAmount
import piuk.blockchain.android.ui.activity.detail.SwapReceiveAmount
import piuk.blockchain.android.ui.activity.detail.To
import piuk.blockchain.android.ui.activity.detail.TotalCostAmount
import piuk.blockchain.android.ui.activity.detail.TransactionId
import piuk.blockchain.android.ui.activity.detail.Value
import piuk.blockchain.android.ui.activity.detail.XlmMemo
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context

class ActivityDetailInfoItemDelegate<in T> : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position] as ActivityDetailsType
        return item !is Action && item !is Description
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        InfoItemViewHolder(
            ItemListInfoRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as InfoItemViewHolder).bind(
        items[position] as ActivityDetailsType
    )
}

private class InfoItemViewHolder(private val binding: ItemListInfoRowBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ActivityDetailsType) {
        with(binding) {
            itemListInfoRowTitle.text = getHeaderForType(item)
            itemListInfoRowDescription.text = getValueForType(item)
        }
    }

    private fun getHeaderForType(infoType: ActivityDetailsType): String =
        when (infoType) {
            is Created -> context.getString(R.string.activity_details_created)
            is NextPayment -> context.getString(R.string.recurring_buy_details_next_payment)
            is Amount -> context.getString(R.string.activity_details_amount)
            is Fee -> context.getString(R.string.activity_details_fee)
            is Value -> context.getString(R.string.activity_details_value)
            is HistoricValue -> {
                when (infoType.transactionType) {
                    TransactionSummary.TransactionType.SENT,
                    TransactionSummary.TransactionType.SELL ->
                        context.getString(R.string.activity_details_historic_sent)
                    TransactionSummary.TransactionType.RECEIVED,
                    TransactionSummary.TransactionType.BUY ->
                        context.getString(R.string.activity_details_historic_received)
                    TransactionSummary.TransactionType.TRANSFERRED,
                    TransactionSummary.TransactionType.SWAP
                    -> context.getString(R.string.activity_details_historic_transferred)
                    else -> context.getString(R.string.empty)
                }
            }
            is To -> context.getString(R.string.activity_details_to)
            is From -> context.getString(R.string.activity_details_from)
            is FeeForTransaction -> context.getString(R.string.activity_details_transaction_fee)
            is BuyFee -> context.getString(R.string.activity_details_buy_fees)
            is BuyPurchaseAmount -> context.getString(R.string.activity_details_buy_purchase_amount)
            is TotalCostAmount -> context.getString(R.string.recurring_buy_details_total_cost)
            is FeeAmount -> context.getString(R.string.recurring_buy_details_fees)
            is SellPurchaseAmount -> context.getString(R.string.common_total)
            is TransactionId -> context.getString(R.string.activity_details_buy_tx_id)
            is BuyCryptoWallet,
            is SellCryptoWallet -> context.getString(R.string.activity_details_buy_sending_to)
            is BuyPaymentMethod -> context.getString(R.string.activity_details_buy_payment_method)
            is SwapReceiveAmount -> context.getString(R.string.activity_details_swap_for)
            is NetworkFee -> context.getString(
                R.string.tx_confirmation_network_fee,
                (infoType.feeValue as CryptoValue).currency.displayTicker
            )
            is XlmMemo -> context.getString(R.string.xlm_memo_text)
            is RecurringBuyFrequency -> context.getString(R.string.recurring_buy_details_recurring)
            else -> context.getString(R.string.empty)
        }

    private fun getValueForType(infoType: ActivityDetailsType): String =
        when (infoType) {
            is Created -> infoType.date.toFormattedString()
            is NextPayment -> infoType.date.toFormattedString()
            is RecurringBuyFrequency -> "${
                infoType.frequency.toHumanReadableRecurringBuy(context)
            } ${infoType.frequency.toHumanReadableRecurringDate(context)}"
            is Amount -> infoType.value.toStringWithSymbol()
            is Fee -> infoType.feeValue?.toStringWithSymbol() ?: context.getString(
                R.string.activity_details_fee_load_fail
            )
            is Value -> infoType.currentFiatValue?.toStringWithSymbol() ?: context.getString(
                R.string.activity_details_value_load_fail
            )
            is HistoricValue -> infoType.fiatAtExecution?.toStringWithSymbol()
                ?: context.getString(
                    R.string.activity_details_historic_value_load_fail
                )
            is To -> infoType.toAddress ?: context.getString(
                R.string.activity_details_to_load_fail
            )
            is From -> infoType.fromAddress ?: context.getString(
                R.string.activity_details_from_load_fail
            )
            is FeeForTransaction -> {
                when (infoType.transactionType) {
                    TransactionSummary.TransactionType.SENT -> context.getString(
                        R.string.activity_details_transaction_fee_send,
                        infoType.cryptoValue.toStringWithSymbol()
                    )
                    else -> context.getString(
                        R.string.activity_details_transaction_fee_unknown
                    )
                }
            }
            is BuyFee -> infoType.feeValue.toStringWithSymbol()
            is BuyPurchaseAmount -> infoType.fundedFiat.toStringWithSymbol()
            is TotalCostAmount -> infoType.fundedFiat.toStringWithSymbol()
            is FeeAmount -> infoType.fundedFiat.toStringWithSymbol()
            is TransactionId -> infoType.txId
            is BuyCryptoWallet -> context.getString(
                R.string.custodial_wallet_default_label_2, infoType.crypto.displayTicker
            )
            is SellCryptoWallet -> context.getString(
                R.string.fiat_currency_funds_wallet_name_1, infoType.currency
            )
            is SellPurchaseAmount -> infoType.value.toStringWithSymbol()
            is BuyPaymentMethod -> {
                when {
                    infoType.paymentDetails.endDigits != null &&
                        infoType.paymentDetails.label != null -> {
                        with(context) {
                            infoType.paymentDetails.accountType?.let {
                                val accType = getString(
                                    R.string.payment_method_type_account_info,
                                    infoType.paymentDetails.accountType,
                                    infoType.paymentDetails.endDigits
                                )

                                getString(
                                    R.string.common_spaced_strings,
                                    infoType.paymentDetails.label,
                                    accType
                                )
                            } ?: getString(
                                R.string.common_hyphenated_strings,
                                infoType.paymentDetails.label,
                                infoType.paymentDetails.endDigits
                            )
                        }
                    }
                    infoType.paymentDetails.paymentMethodId == PaymentMethod.FUNDS_PAYMENT_ID -> {
                        context.getString(R.string.checkout_funds_label)
                    }
                    else -> {
                        context.getString(R.string.activity_details_payment_load_fail)
                    }
                }
            }
            is SwapReceiveAmount -> infoType.receivedAmount.toStringWithSymbol()
            is NetworkFee -> infoType.feeValue.toStringWithSymbol()
            is XlmMemo -> infoType.memo
            else -> ""
        }
}
