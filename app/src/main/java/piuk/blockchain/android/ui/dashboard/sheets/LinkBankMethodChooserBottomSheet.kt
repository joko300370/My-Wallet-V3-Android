package piuk.blockchain.android.ui.dashboard.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.LinkBankMethodChooserSheetLayoutBinding
import piuk.blockchain.android.databinding.LinkBankMethodItemBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.dashboard.LinkablePaymentMethodsForAction
import piuk.blockchain.android.ui.settings.BankLinkingHost
import piuk.blockchain.android.util.visibleIf

class LinkBankMethodChooserBottomSheet : SlidingModalBottomDialog<LinkBankMethodChooserSheetLayoutBinding>() {
    private val paymentMethods: LinkablePaymentMethodsForAction
        get() = arguments?.getSerializable(LINKABLE_METHODS) as LinkablePaymentMethodsForAction

    private val isForPayment: Boolean
        get() = arguments?.getBoolean(FOR_PAYMENT, false) ?: false

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): LinkBankMethodChooserSheetLayoutBinding =
        LinkBankMethodChooserSheetLayoutBinding.inflate(inflater, container, false)

    override val host: BankLinkingHost
        get() = parentFragment as? BankLinkingHost
            ?: throw IllegalStateException("Host is not a BankLinkingHost")

    override fun initControls(binding: LinkBankMethodChooserSheetLayoutBinding) {
        with(binding) {
            recycler.layoutManager = LinearLayoutManager(activity)
            recycler.adapter = LinkBankMethodChooserAdapter(
                paymentMethods.linkablePaymentMethods.linkMethods, isForPayment
            ) {
                when (it) {
                    PaymentMethodType.BANK_TRANSFER -> kotlin.run {
                        host.onLinkBankSelected(
                            paymentMethods
                        )
                        dismiss()
                    }
                    PaymentMethodType.FUNDS -> kotlin.run {
                        host.onBankWireTransferSelected(
                            paymentMethods.linkablePaymentMethods.currency
                        )
                        dismiss()
                    }
                    else -> throw IllegalStateException("Not supported linking method")
                }
            }

            paymentMethodsTitle.text = getString(
                if (isForPayment) {
                    R.string.add_a_deposit_method
                } else {
                    R.string.add_a_bank_account
                }
            )
        }
    }

    companion object {
        private const val LINKABLE_METHODS = "LINKABLE_METHODS"
        private const val FOR_PAYMENT = "FOR_PAYMENT"
        fun newInstance(
            linkablePaymentMethodsForAction: LinkablePaymentMethodsForAction,
            isForPayment: Boolean = false
        ): LinkBankMethodChooserBottomSheet =
            LinkBankMethodChooserBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(LINKABLE_METHODS, linkablePaymentMethodsForAction)
                    putBoolean(FOR_PAYMENT, isForPayment)
                }
            }
    }
}

class LinkBankMethodChooserAdapter(
    private val paymentMethods: List<PaymentMethodType>,
    private val isForPayment: Boolean,
    private val onClick: (PaymentMethodType) -> Unit
) : RecyclerView.Adapter<LinkBankMethodChooserAdapter.LinkBankMethodViewHolder>() {

    class LinkBankMethodViewHolder(private val binding: LinkBankMethodItemBinding, private val isForPayment: Boolean) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(paymentMethod: PaymentMethodType, onClick: (PaymentMethodType) -> Unit) {
            val item = paymentMethod.toLinkBankMethodItemUI(isForPayment)
            with(binding) {
                paymentMethodTitle.setText(item.title)
                paymentMethodSubtitle.setText(item.subtitle)
                paymentMethodSubtitle.visibleIf { isForPayment }
                paymentMethodBlurb.setText(item.blurb)
                paymentMethodIcon.setImageResource(item.icon)
                paymentMethodRoot.setOnClickListener {
                    onClick(paymentMethod)
                }
                badge.visibleIf { paymentMethod == PaymentMethodType.BANK_TRANSFER }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LinkBankMethodViewHolder {
        val binding = LinkBankMethodItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LinkBankMethodViewHolder(binding, isForPayment)
    }

    override fun onBindViewHolder(holder: LinkBankMethodViewHolder, position: Int) {
        holder.bind(paymentMethods[position], onClick)
    }

    override fun getItemCount(): Int = paymentMethods.size
}

private fun PaymentMethodType.toLinkBankMethodItemUI(isForPayment: Boolean): LinkBankMethodItem =
    when (this) {
        PaymentMethodType.FUNDS -> LinkBankMethodItem(
            title = if (isForPayment) {
                R.string.payment_wire_transfer
            } else {
                R.string.bank_wire_transfer
            },
            subtitle = if (isForPayment) {
                R.string.payment_wire_transfer_subtitle
            } else {
                R.string.empty
            },
            blurb = if (isForPayment) {
                R.string.payment_wire_transfer_blurb
            } else {
                R.string.link_a_bank_wire_transfer
            },
            icon = R.drawable.ic_funds_deposit
        )
        PaymentMethodType.BANK_TRANSFER -> LinkBankMethodItem(
            title = if (isForPayment) {
                R.string.payment_deposit
            } else {
                R.string.link_a_bank
            },
            subtitle = if (isForPayment) {
                R.string.payment_deposit_subtitle
            } else {
                R.string.empty
            },
            blurb = if (isForPayment) {
                R.string.payment_deposit_blurb
            } else {
                R.string.link_a_bank_bank_transfer
            },
            icon = R.drawable.ic_bank_transfer
        )
        else -> throw IllegalStateException("Not supported linking method")
    }

data class LinkBankMethodItem(
    @StringRes val title: Int,
    @StringRes val blurb: Int,
    @DrawableRes val icon: Int,
    @StringRes val subtitle: Int
)