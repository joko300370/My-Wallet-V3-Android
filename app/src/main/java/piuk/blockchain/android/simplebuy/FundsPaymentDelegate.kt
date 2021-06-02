package piuk.blockchain.android.simplebuy

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.datamanagers.PaymentMethod
import piuk.blockchain.android.databinding.FundsPaymentMethodLayoutBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context

class FundsPaymentDelegate : AdapterDelegate<PaymentMethodItem> {

    override fun isForViewType(items: List<PaymentMethodItem>, position: Int): Boolean =
        items[position].paymentMethod is PaymentMethod.Funds

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        FundsPaymentViewHolder(
            FundsPaymentMethodLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(items: List<PaymentMethodItem>, position: Int, holder: RecyclerView.ViewHolder) {
        (holder as FundsPaymentViewHolder).bind(items[position])
    }

    private class FundsPaymentViewHolder(private val binding: FundsPaymentMethodLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(paymentMethodItem: PaymentMethodItem) {
            with(binding) {
                (paymentMethodItem.paymentMethod as? PaymentMethod.Funds)?.let {
                    paymentMethodIcon.setImageResource(it.icon())
                    ticker.text = paymentMethodItem.paymentMethod.fiatCurrency
                    paymentMethodTitle.text = context.getString(it.label())
                    balance.text = it.balance.toStringWithSymbol()
                }
                paymentMethodRoot.setOnClickListener { paymentMethodItem.clickAction() }
            }
        }
    }
}