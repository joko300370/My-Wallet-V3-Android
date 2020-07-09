package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import kotlinx.android.synthetic.main.simple_buy_crypto_currency_chooser.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.io.Serializable

class PaymentMethodChooserBottomSheet : SlidingModalBottomDialog() {
    private val paymentMethods: List<PaymentMethod> by unsafeLazy {
        arguments?.getSerializable(SUPPORTED_PAYMENT_METHODS) as? List<PaymentMethod>
            ?: emptyList()
    }

    override val layoutResource: Int
        get() = R.layout.simple_buy_payment_method_chooser

    override fun initControls(view: View) {
        view.recycler.adapter =
            PaymentMethodsAdapter(
                paymentMethods
                    .map {
                        it.toPaymentMethodItem()
                    })

        view.recycler.layoutManager = LinearLayoutManager(context)

        analytics.logEvent(SimpleBuyAnalytics.PAYMENT_METHODS_SHOWN)
    }

    private fun PaymentMethod.toPaymentMethodItem(): PaymentMethodItem {
        return PaymentMethodItem(this, clickAction())
    }

    private fun PaymentMethod.clickAction(): () -> Unit = when (this) {
        is PaymentMethod.UndefinedCard -> {
            {
                (parentFragment as? PaymentMethodChangeListener)?.addPaymentMethod(
                    PaymentMethodType.PAYMENT_CARD
                )
                dismiss()
            }
        }
        is PaymentMethod.UndefinedFunds -> {
            {
                (parentFragment as? PaymentMethodChangeListener)?.addPaymentMethod(
                    PaymentMethodType.FUNDS
                )
                dismiss()
            }
        }
        else -> {
            {
                (parentFragment as? PaymentMethodChangeListener)?.onPaymentMethodChanged(this)
                dismiss()
            }
        }
    }

    companion object {
        private const val SUPPORTED_PAYMENT_METHODS = "supported_payment_methods_key"
        private const val CAN_ADD_CARD_KEY = "can_add_card_key"
        private const val CAN_LINK_FUNDS_KEY = "can_link_funds_key"
        fun newInstance(paymentMethods: List<PaymentMethod>, canAddCard: Boolean, canLindFunds: Boolean):
                PaymentMethodChooserBottomSheet {
            val bundle = Bundle()
            bundle.putSerializable(SUPPORTED_PAYMENT_METHODS, paymentMethods as Serializable)
            bundle.putBoolean(CAN_ADD_CARD_KEY, canAddCard)
            bundle.putBoolean(CAN_LINK_FUNDS_KEY, canLindFunds)
            return PaymentMethodChooserBottomSheet().apply {
                arguments = bundle
            }
        }
    }
}

data class PaymentMethodItem(val paymentMethod: PaymentMethod, val clickAction: () -> Unit)

private class PaymentMethodsAdapter(adapterItems: List<PaymentMethodItem>) :
    DelegationAdapter<PaymentMethodItem>(AdapterDelegatesManager(), adapterItems) {
    init {
        val bankPaymentDelegate = BankPaymentDelegate()
        val cardPaymentDelegate = CardPaymentDelegate()
        val addFundsPaymentDelegate = AddFundsDelegate()
        val addCardPaymentDelegate = AddCardDelegate()
        val fundsPaymentDelegate = FundsPaymentDelegate()

        delegatesManager.apply {
            addAdapterDelegate(bankPaymentDelegate)
            addAdapterDelegate(cardPaymentDelegate)
            addAdapterDelegate(fundsPaymentDelegate)
            addAdapterDelegate(addCardPaymentDelegate)
            addAdapterDelegate(addFundsPaymentDelegate)
        }
    }
}