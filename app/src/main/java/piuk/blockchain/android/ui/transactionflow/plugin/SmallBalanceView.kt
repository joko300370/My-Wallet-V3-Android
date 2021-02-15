package piuk.blockchain.android.ui.transactionflow.plugin

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import piuk.blockchain.android.databinding.ViewTxFlowSmallBalanceBinding
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowCustomiser
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visibleIf

class SmallBalanceView @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle),
    TxFlowWidget {

    private lateinit var model: TransactionModel
    private lateinit var customiser: TransactionFlowCustomiser
    private lateinit var analytics: TxFlowAnalytics

    private val binding: ViewTxFlowSmallBalanceBinding =
        ViewTxFlowSmallBalanceBinding.inflate(LayoutInflater.from(context), this, true)

    override var displayMode: TxFlowWidget.DisplayMode = TxFlowWidget.DisplayMode.Crypto

    override fun initControl(
        model: TransactionModel,
        customiser: TransactionFlowCustomiser,
        analytics: TxFlowAnalytics
    ) {
        check(this::model.isInitialized.not()) { "Control already initialised" }

        this.model = model
        this.customiser = customiser
        this.analytics = analytics

        binding.useMax.gone()
    }

    override fun update(state: TransactionState) {
        check(::model.isInitialized) { "Control not initialised" }

        updateSendMax(state)
        updateBalance(state)
    }

    @SuppressLint("SetTextI18n")
    private fun updateBalance(state: TransactionState) {
        val availableBalance = state.availableBalance
        if (availableBalance.isPositive || availableBalance.isZero) {
            state.fiatRate?.let { rate ->
                binding.maxAvailableValue.text =
                    "${rate.convert(availableBalance).toStringWithSymbol()} " +
                        "(${availableBalance.toStringWithSymbol()})"
                }
        }
    }

    private fun updateSendMax(state: TransactionState) =
        with(binding.useMax) {
            text = customiser.enterAmountMaxButton(state)
            visibleIf { !customiser.shouldDisableInput(state.errorState) }
            setOnClickListener {
                analytics.onMaxClicked(state)
                model.process(TransactionIntent.UseMaxSpendable)
            }
        }
}