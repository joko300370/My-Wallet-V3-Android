package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.fiat.LinkedBankAccount
import piuk.blockchain.android.databinding.ViewAccountBankOverviewBinding
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.EnterAmountCustomisations
import piuk.blockchain.android.ui.transactionflow.plugin.TxFlowWidget
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
        setOnClickListener { onAccountClicked(account) }
    }

    override fun initControl(
        model: TransactionModel,
        customiser: EnterAmountCustomisations,
        analytics: TxFlowAnalytics
    ) {
        this.model = model
        binding.bankSeparator.visible()
    }

    override fun update(state: TransactionState) {
        binding.bankChevron.visible()

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
            }
        }
    }
}
