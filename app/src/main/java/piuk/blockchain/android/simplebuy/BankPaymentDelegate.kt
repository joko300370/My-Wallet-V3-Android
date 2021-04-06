package piuk.blockchain.android.simplebuy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.bank_payment_method_layout.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context

class BankPaymentDelegate : AdapterDelegate<PaymentMethodItem> {

    override fun isForViewType(items: List<PaymentMethodItem>, position: Int): Boolean =
        items[position].paymentMethod is PaymentMethod.Bank

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(
                R.layout.bank_payment_method_layout,
                parent,
                false
            )
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(items: List<PaymentMethodItem>, position: Int, holder: RecyclerView.ViewHolder) {
        val headerViewHolder = holder as ViewHolder
        headerViewHolder.bind(items[position])
    }

    private class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val limit: AppCompatTextView = itemView.payment_method_limit
        val title: AppCompatTextView = itemView.payment_method_title
        val details: AppCompatTextView = itemView.payment_method_details
        val icon: AppCompatImageView = itemView.payment_method_icon
        val root: ViewGroup = itemView.payment_method_root

        fun bind(paymentMethodItem: PaymentMethodItem) {
            (paymentMethodItem.paymentMethod as? PaymentMethod.Bank)?.let {
                limit.text =
                    limit.context.getString(
                        R.string.payment_method_limit,
                        paymentMethodItem.paymentMethod.limits.max.toStringWithSymbol()
                    )
                title.text = it.bankName
                details.text = details.context.getString(
                    R.string.payment_method_type_account_info, it.uiAccountType, it.accountEnding
                )
                if (it.iconUrl.isNotEmpty()) {
                    Glide.with(context).load(it.iconUrl).into(icon)
                }
            }
            root.setOnClickListener { paymentMethodItem.clickAction() }
        }
    }
}