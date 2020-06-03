package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import kotlinx.android.synthetic.main.layout_payment_method_chooser_item.view.*
import kotlinx.android.synthetic.main.simple_buy_crypto_currency_chooser.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.cards.icon
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class PaymentMethodChooserBottomSheet : SlidingModalBottomDialog() {
    private val paymentMethods: List<PaymentMethod> by unsafeLazy {
        arguments?.getSerializable(SUPPORTED_PAYMENT_METHODS) as? List<PaymentMethod>
            ?: emptyList()
    }

    private val canAdd: Boolean by unsafeLazy {
        arguments?.getBoolean(CAN_ADD_KEY) ?: false
    }

    override val layoutResource: Int
        get() = R.layout.simple_buy_payment_method_chooser

    override fun initControls(view: View) {
        view.recycler.adapter =
            BottomSheetPaymentMethodsAdapter(
                paymentMethods
                    .map {
                        PaymentMethodItem(it, if (it is PaymentMethod.UndefinedCard && canAdd) {
                            {
                                (parentFragment as? PaymentMethodChangeListener)?.addPaymentMethod()
                                dismiss()
                            }
                        } else {
                            {
                                (parentFragment as? PaymentMethodChangeListener)?.onPaymentMethodChanged(it)
                                dismiss()
                            }
                        })
                    }, canAdd)
        view.recycler.layoutManager = LinearLayoutManager(context)

        analytics.logEvent(SimpleBuyAnalytics.PAYMENT_METHODS_SHOWN)
    }

    companion object {
        private const val SUPPORTED_PAYMENT_METHODS = "supported_payment_methods_key"
        private const val CAN_ADD_KEY = "can_add_key"
        fun newInstance(paymentMethods: List<PaymentMethod>, canAdd: Boolean): PaymentMethodChooserBottomSheet {
            val bundle = Bundle()
            bundle.putSerializable(SUPPORTED_PAYMENT_METHODS, paymentMethods as Serializable)
            bundle.putBoolean(CAN_ADD_KEY, canAdd)
            return PaymentMethodChooserBottomSheet().apply {
                arguments = bundle
            }
        }
    }
}

private data class PaymentMethodItem(val paymentMethod: PaymentMethod, val clickAction: () -> Unit)

private class BottomSheetPaymentMethodsAdapter(
    private val adapterItems: List<PaymentMethodItem>,
    private val canAdd: Boolean
) : RecyclerView.Adapter<BottomSheetPaymentMethodsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(
                R.layout.layout_payment_method_chooser_item,
                parent,
                false
            )
        return ViewHolder(itemView)
    }

    override fun getItemCount(): Int =
        adapterItems.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = adapterItems[position]
        when (item.paymentMethod) {
            is PaymentMethod.Card -> configureUIforCard(item.paymentMethod, holder)
            is PaymentMethod.BankTransfer -> configureUIforBankTransfer(item.paymentMethod, holder)
            is PaymentMethod.UndefinedCard -> configureUIforUndefinedCard(item.paymentMethod, holder, canAdd)
        }
        holder.root.setOnClickListener {
            adapterItems[position].clickAction()
        }
    }

    private fun configureUIforUndefinedCard(
        paymentMethod: PaymentMethod.UndefinedCard,
        holder: ViewHolder,
        canAdd: Boolean
    ) {
        with(holder) {
            addIcon.visibleIf { canAdd }
            cardNumber.gone()
            expiryDate.gone()
            icon.setImageResource(R.drawable.ic_payment_card)
            title.text = if (canAdd) title.context.getString(R.string.add_credit_or_debit_card) else
                title.context.getString(R.string.credit_or_debit_card)
            limit.text =
                limit.context.getString(R.string.payment_method_limit, paymentMethod.limits.max.toStringWithSymbol())
        }
    }

    private fun configureUIforBankTransfer(paymentMethod: PaymentMethod.BankTransfer, holder: ViewHolder) {
        with(holder) {
            addIcon.gone()
            cardNumber.gone()
            expiryDate.gone()
            icon.setImageResource(R.drawable.ic_bank_transfer)
            title.text = title.context.getString(R.string.bank_wire_transfer)
            limit.text =
                limit.context.getString(R.string.payment_method_limit, paymentMethod.limits.max.toStringWithSymbol())
        }
    }

    private fun configureUIforCard(paymentMethod: PaymentMethod.Card, holder: ViewHolder) {
        with(holder) {
            addIcon.gone()
            icon.setImageResource(paymentMethod.cardType.icon())
            title.text = paymentMethod.uiLabel()
            limit.text = paymentMethod.limits.max.toStringWithSymbol()
            cardNumber.text = paymentMethod.dottedEndDigits()
            expiryDate.text =
                expiryDate.context.getString(R.string.card_expiry_date, paymentMethod.expireDate.formatted())
            cardNumber.visible()
            expiryDate.visible()
        }
    }

    private class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: AppCompatTextView = itemView.payment_method_title
        val limit: AppCompatTextView = itemView.payment_method_limit
        val icon: ImageView = itemView.payment_method_icon
        val addIcon: ImageView = itemView.add_payment_method_icon
        val root: ViewGroup = itemView.payment_method_root
        val expiryDate: AppCompatTextView = itemView.exp_date
        val cardNumber: AppCompatTextView = itemView.card_number
    }

    private fun Date.formatted(): String =
        SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(this)
}
