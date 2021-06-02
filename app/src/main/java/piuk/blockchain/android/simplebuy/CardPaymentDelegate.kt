package piuk.blockchain.android.simplebuy

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.datamanagers.PaymentMethod
import piuk.blockchain.android.R
import piuk.blockchain.android.cards.icon
import piuk.blockchain.android.databinding.CardPaymentMethodLayoutBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CardPaymentDelegate : AdapterDelegate<PaymentMethodItem> {

    override fun isForViewType(items: List<PaymentMethodItem>, position: Int): Boolean =
        items[position].paymentMethod is PaymentMethod.Card

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CardPaymentViewHolder(
            CardPaymentMethodLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(items: List<PaymentMethodItem>, position: Int, holder: RecyclerView.ViewHolder) {
        (holder as CardPaymentViewHolder).bind(items[position])
    }

    private class CardPaymentViewHolder(private val binding: CardPaymentMethodLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(paymentMethodItem: PaymentMethodItem) {
            with(binding) {
                (paymentMethodItem.paymentMethod as? PaymentMethod.Card)?.let {
                    paymentMethodIcon.setImageResource(it.cardType.icon())
                    paymentMethodLimit.text =
                        context.getString(
                            R.string.payment_method_limit,
                            paymentMethodItem.paymentMethod.limits.max.toStringWithSymbol()
                        )
                    paymentMethodTitle.text = it.uiLabel()
                    cardNumber.text = it.dottedEndDigits()
                    expDate.text = context.getString(R.string.card_expiry_date, it.expireDate.formatted())
                }
                paymentMethodRoot.setOnClickListener { paymentMethodItem.clickAction() }
            }
        }

        private fun Date.formatted(): String =
            SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(this)
    }
}