package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.fiat.LinkedBankAccount
import piuk.blockchain.android.databinding.ViewAccountBankOverviewBinding
import piuk.blockchain.android.ui.customviews.StatusPill
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.EnterAmountCustomisations
import piuk.blockchain.android.ui.transactionflow.plugin.TxFlowWidget
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible

class AccountInfoBank @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle), TxFlowWidget {

    private lateinit var model: TransactionModel

    val binding: ViewAccountBankOverviewBinding =
        ViewAccountBankOverviewBinding.inflate(LayoutInflater.from(context), this, true)

    fun updateAccount(
        account: LinkedBankAccount,
        onAccountClicked: (LinkedBankAccount) -> Unit
    ) {

        with(binding) {
            bankName.text = account.label
            bankLogo.setImageResource(R.drawable.ic_bank_transfer)
            bankDetails.text = context.getString(
                R.string.common_hyphenated_strings,
                if (account.accountType.isBlank()) {
                    context.getString(R.string.bank_account_info_default)
                } else {
                    account.accountType
                }, account.accountNumber
            )
        }
        showBadgeForType(account.type)
        setOnClickListener { onAccountClicked(account) }
    }

    private fun showBadgeForType(type: PaymentMethodType) {
        require(type == PaymentMethodType.BANK_TRANSFER || type == PaymentMethodType.FUNDS) {
            "Using incorrect payment method for Bank view"
        }

        with(binding.bankStatus) {
            when (type) {
                PaymentMethodType.BANK_TRANSFER -> update(
                    context.getString(R.string.common_free), StatusPill.StatusType.UPSELL
                )
                // TODO this can be replaced with the fee data when we have that from the endpoint
                PaymentMethodType.FUNDS -> update(
                    context.getString(R.string.bank_wire_transfer_fee_default), StatusPill.StatusType.WARNING
                )
                else -> gone()
            }
        }
    }

    override fun initControl(
        model: TransactionModel,
        customiser: EnterAmountCustomisations,
        analytics: TxFlowAnalytics
    ) {
        this.model = model
        binding.bankSeparator.visible()
        binding.bankChevron.visible()
        binding.bankStatus.gone()
    }

    override fun update(state: TransactionState) {
        when (state.action) {
            AssetAction.FiatDeposit ->
                if (state.sendingAccount is LinkedBankAccount) {
                    // only try to update if we have a linked bank source
                    updateAccount(state.sendingAccount) {
                        if (::model.isInitialized) {
                            model.process(TransactionIntent.InvalidateTransactionKeepingTarget)
                        }
                    }
                }
            AssetAction.Withdraw ->
                if (state.selectedTarget is LinkedBankAccount) {
                    updateAccount(state.selectedTarget) {
                        if (::model.isInitialized) {
                            model.process(TransactionIntent.InvalidateTransaction)
                        }
                    }
                }
            else -> {
                // do nothing
            }
        }
    }
}
