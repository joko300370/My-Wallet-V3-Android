package piuk.blockchain.android.ui.transactionflow.plugin

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import info.blockchain.balance.CryptoValue
import org.koin.core.KoinComponent
import org.koin.core.inject
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.databinding.ViewCheckoutSwapHeaderBinding
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionConfirmationCustomisations
import piuk.blockchain.android.util.visibleIf

class SwapInfoHeaderView @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle), ConfirmSheetWidget, KoinComponent {

    private lateinit var model: TransactionModel
    private lateinit var customiser: TransactionConfirmationCustomisations
    private lateinit var analytics: TxFlowAnalytics
    private val assetResources: AssetResources by inject()

    private val binding: ViewCheckoutSwapHeaderBinding =
        ViewCheckoutSwapHeaderBinding.inflate(LayoutInflater.from(context), this, true)

    override fun initControl(
        model: TransactionModel,
        customiser: TransactionConfirmationCustomisations,
        analytics: TxFlowAnalytics
    ) {
        check(this::model.isInitialized.not()) { "Control already initialised" }

        this.model = model
        this.customiser = customiser
        this.analytics = analytics
    }

    override fun update(state: TransactionState) {
        check(::model.isInitialized) { "Control not initialised" }

        with(binding) {
            state.pendingTx?.amount?.let { amount ->
                sendingAmount.text = amount.toStringWithoutSymbol()
                state.fiatRate?.let {
                    amountFiat.text = it.convert(amount, false).toStringWithSymbol()
                }
            }

            receivingAmount.text = state.targetRate?.convert(state.amount)?.toStringWithoutSymbol() ?: CryptoValue.zero(
                (state.selectedTarget as CryptoAccount).asset
            ).toStringWithoutSymbol()

            sendingIcon.setImageResource(
                assetResources.drawableResFilled(state.sendingAsset)
            )
            (state.selectedTarget as? CryptoAccount)?.let {
                receivingIcon.setImageResource(assetResources.drawableResFilled(it.asset))
            }
        }
    }

    override fun setVisible(isVisible: Boolean) {
        binding.root.visibleIf { isVisible }
    }
}