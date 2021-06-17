package piuk.blockchain.android.ui.transactionflow.plugin

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import org.koin.core.KoinComponent
import piuk.blockchain.android.databinding.ViewCheckoutHeaderBinding
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionConfirmationCustomisations
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.goneIf
import piuk.blockchain.android.util.visibleIf

class SimpleInfoHeaderView @JvmOverloads constructor(
    ctx: Context,
    private val showExchange: Boolean = true,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle), ConfirmSheetWidget, KoinComponent {

    private lateinit var model: TransactionModel
    private lateinit var customiser: TransactionConfirmationCustomisations
    private lateinit var analytics: TxFlowAnalytics

    private val binding: ViewCheckoutHeaderBinding =
        ViewCheckoutHeaderBinding.inflate(LayoutInflater.from(context), this, true)

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
            state.pendingTx?.amount?.let { amnt ->
                headerTitle.text = amnt.toStringWithSymbol()
                if (showExchange) {
                    state.fiatRate?.let {
                        headerSubtitle.text = it.convert(amnt, false).toStringWithSymbol()
                    }
                } else {
                    headerSubtitle.gone()
                }
            }
        }
    }

    fun setDetails(title: String, subtitle: String) {
        with(binding) {
            headerTitle.goneIf { title.isBlank() }
            headerSubtitle.goneIf { subtitle.isBlank() }

            headerTitle.text = title
            headerSubtitle.text = subtitle
        }
    }

    override fun setVisible(isVisible: Boolean) {
        binding.root.visibleIf { isVisible }
    }
}