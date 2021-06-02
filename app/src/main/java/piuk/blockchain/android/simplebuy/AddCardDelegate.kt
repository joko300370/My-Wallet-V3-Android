package piuk.blockchain.android.simplebuy

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.datamanagers.PaymentMethod
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.AddNewCardLayoutBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate

class AddCardDelegate : AdapterDelegate<PaymentMethodItem> {

    override fun isForViewType(items: List<PaymentMethodItem>, position: Int): Boolean =
        items[position].paymentMethod is PaymentMethod.UndefinedCard

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder = ViewHolder(
        AddNewCardLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun onBindViewHolder(items: List<PaymentMethodItem>, position: Int, holder: RecyclerView.ViewHolder) {
        val headerViewHolder = holder as ViewHolder
        headerViewHolder.bind(items[position])
    }

    private class ViewHolder(private val binding: AddNewCardLayoutBinding) : RecyclerView.ViewHolder(binding.root) {

        val limit: AppCompatTextView = binding.paymentMethodLimit
        val root: ViewGroup = binding.paymentMethodRoot

        fun bind(paymentMethodItem: PaymentMethodItem) {
            (paymentMethodItem.paymentMethod as? PaymentMethod.UndefinedCard)?.let {
                limit.text =
                    limit.context.getString(
                        R.string.payment_method_limit,
                        paymentMethodItem.paymentMethod.limits.max.toStringWithSymbol()
                    )
            }
            root.setOnClickListener { paymentMethodItem.clickAction() }
        }
    }
}