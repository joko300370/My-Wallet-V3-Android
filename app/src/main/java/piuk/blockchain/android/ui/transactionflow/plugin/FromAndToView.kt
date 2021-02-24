package piuk.blockchain.android.ui.transactionflow.plugin

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.NullAddress
import piuk.blockchain.android.databinding.ViewTxFlowFromAndToBinding
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.EnterAmountCustomisations
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.setAssetIconColours
import piuk.blockchain.android.util.setCoinIcon
import piuk.blockchain.android.util.visibleIf

class FromAndToView @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle),
    TxFlowWidget {

    private lateinit var model: TransactionModel
    private lateinit var customiser: EnterAmountCustomisations
    private lateinit var analytics: TxFlowAnalytics

    private val binding: ViewTxFlowFromAndToBinding =
        ViewTxFlowFromAndToBinding.inflate(LayoutInflater.from(context), this, true)

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
            amountSheetAssetIcon.setImageResource(customiser.enterAmountSourceIcon(state))

            if (customiser.showTargetIcon(state)) {
                (state.selectedTarget as? CryptoAccount)?.let {
                    amountSheetTargetIcon.setCoinIcon(it.asset)
                }
            } else {
                amountSheetTargetIcon.gone()
            }

            amountSheetAssetDirection.setImageResource(customiser.enterAmountActionIcon(state))
            if (customiser.enterAmountActionIconCustomisation(state)) {
                amountSheetAssetDirection.setAssetIconColours(state.sendingAsset, this@FromAndToView.context)
            }
        }

        updateSourceAndTargetDetails(state)
    }

    private fun updateSourceAndTargetDetails(state: TransactionState) {
        if (state.selectedTarget is NullAddress) {
            return
        }
        with(binding) {
            amountSheetFrom.text = customiser.enterAmountSourceLabel(state)
            amountSheetTo.text = customiser.enterAmountTargetLabel(state)
        }
    }

    override var displayMode: TxFlowWidget.DisplayMode = TxFlowWidget.DisplayMode.Fiat
}