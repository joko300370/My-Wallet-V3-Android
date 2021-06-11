package piuk.blockchain.android.simplebuy

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.bumptech.glide.Glide
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.BankPaymentMethodLayoutBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context

class BankPaymentDelegate : AdapterDelegate<PaymentMethodItem> {

    override fun isForViewType(items: List<PaymentMethodItem>, position: Int): Boolean =
        items[position].paymentMethod is PaymentMethod.Bank

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        BankPaymentViewHolder(
            BankPaymentMethodLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(items: List<PaymentMethodItem>, position: Int, holder: RecyclerView.ViewHolder) {
        (holder as BankPaymentViewHolder).bind(items[position])
    }

    private class BankPaymentViewHolder(private val binding: BankPaymentMethodLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(paymentMethodItem: PaymentMethodItem) {
            with(binding) {
                (paymentMethodItem.paymentMethod as? PaymentMethod.Bank)?.let {
                    paymentMethodLimit.text =
                        context.getString(
                            R.string.payment_method_limit,
                            paymentMethodItem.paymentMethod.limits.max.toStringWithSymbol()
                        )
                    paymentMethodTitle.text = it.bankName
                    paymentMethodDetails.text = context.getString(
                        R.string.payment_method_type_account_info, it.uiAccountType, it.accountEnding
                    )
                    if (it.iconUrl.isNotEmpty()) {
                        Glide.with(context).load(it.iconUrl).into(paymentMethodIcon)
                    }
                }
                paymentMethodRoot.setOnClickListener { paymentMethodItem.clickAction() }
            }
        }
    }
}