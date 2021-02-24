package piuk.blockchain.android.ui.transactionflow.plugin

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import piuk.blockchain.android.coincore.NullAddress
import piuk.blockchain.android.databinding.ViewTxFlowAccountLimitsBinding
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.EnterAmountCustomisations
import piuk.blockchain.android.util.setAssetIconColours
import piuk.blockchain.android.util.visibleIf

class AccountLimitsView @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle),
    TxFlowWidget {

    private lateinit var model: TransactionModel
    private lateinit var customiser: EnterAmountCustomisations
    private lateinit var analytics: TxFlowAnalytics

    private val binding: ViewTxFlowAccountLimitsBinding =
        ViewTxFlowAccountLimitsBinding.inflate(LayoutInflater.from(context), this, true)

    override fun initControl(
        model: TransactionModel,
        customiser: EnterAmountCustomisations,
        analytics: TxFlowAnalytics
    ) {
        check(this::model.isInitialized.not()) { "Control already initialised" }

        this.model = model
        this.customiser = customiser
        this.analytics = analytics
    }

    override fun update(state: TransactionState) {
        check(::model.isInitialized) { "Control not initialised" }

        updatePendingTxDetails(state)
    }

    override fun setVisible(isVisible: Boolean) {
        binding.root.visibleIf { isVisible }
    }

    private fun updatePendingTxDetails(state: TransactionState) {
        with(binding) {
            amountSheetLimitsIcon.setImageResource(customiser.enterAmountSourceIcon(state))

            amountSheetLimitsDirection.setImageResource(customiser.enterAmountActionIcon(state))
            if (customiser.enterAmountActionIconCustomisation(state)) {
                amountSheetLimitsDirection.setAssetIconColours(state.sendingAsset, this@AccountLimitsView.context)
            }
        }

        updateSourceAndTargetDetails(state)
    }

    private fun updateSourceAndTargetDetails(state: TransactionState) {
        if (state.selectedTarget is NullAddress) {
            return
        }
        with(binding) {
            amountSheetLimitTitle.text = customiser.enterAmountLimitsViewTitle(state)
            amountSheetLimit.text = customiser.enterAmountLimitsViewInfo(state)
        }
    }

    override var displayMode: TxFlowWidget.DisplayMode = TxFlowWidget.DisplayMode.Fiat
}