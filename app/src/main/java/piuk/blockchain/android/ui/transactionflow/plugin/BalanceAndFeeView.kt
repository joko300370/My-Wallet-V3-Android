package piuk.blockchain.android.ui.transactionflow.plugin

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import info.blockchain.balance.Money
import piuk.blockchain.android.databinding.ViewTxFlowFeeAndBalanceBinding
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowCustomiser
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.isVisible
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf

class BalanceAndFeeView @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle),
    TxFlowWidget {

    private lateinit var model: TransactionModel
    private lateinit var customiser: TransactionFlowCustomiser
    private lateinit var analytics: TxFlowAnalytics

    private val binding: ViewTxFlowFeeAndBalanceBinding =
        ViewTxFlowFeeAndBalanceBinding.inflate(LayoutInflater.from(context), this, true)

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

        binding.root.setOnClickListener {
            toggleDropdown()
        }

        binding.toggleIndicator.rotation += 180f
    }

    override fun update(state: TransactionState) {
        check(::model.isInitialized) { "Control not initialised" }

        updateSendMax(state)
        updateBalance(state)
    }

    private fun updateBalance(state: TransactionState) {
        val availableBalance = state.availableBalance
        binding.maxAvailableValue.text = makeFiatString(availableBalance, state)

        state.pendingTx?.totalBalance?.let {
            binding.totalAvailableValue.text = makeFiatString(it, state)
        }

        state.pendingTx?.fees?.let {
            binding.networkFeeValue.text = makeFiatString(it, state)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun makeFiatString(value: Money, state: TransactionState): String {
        if (value.isPositive || value.isZero) {
            state.fiatRate?.let { rate ->
                return "${rate.convert(value).toStringWithSymbol()} " +
                        "(${value.toStringWithSymbol()})"
            }
        }
        return "--"
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

    private var externalFocus: View? = null
    private fun toggleDropdown() {
        val revealDropdown = !binding.dropdown.isVisible()
        // Clear focus - and keyboard - remember it, so we can set it back when we close
        if (revealDropdown) {
            val viewGroup = findRootView()
            externalFocus = viewGroup?.findFocus()
            externalFocus?.clearFocus()
        } else {
            externalFocus?.requestFocus()
            externalFocus = null
        }

        // Hide the use max button
        binding.useMax.visibleIf { !revealDropdown }
        // Expand the dropdown view
//        TransitionManager.beginDelayedTransition(
//            binding.root,
//            Slide(Gravity.BOTTOM).apply {
//                duration = 600
//                addTarget(binding.dropdown)
//            }
//        )
        with(binding.dropdown) {
            if (revealDropdown) {
                visible()
            } else {
                gone()
            }
        }
        // And flip the toggle indicator
        binding.toggleIndicator.rotation += 180f
    }

    private fun findRootView(): ViewGroup? {
        var v = binding.root.parent as? ViewGroup
        while(v?.parent is ViewGroup) {
            v = v.parent as? ViewGroup
        }
        return v
    }
}