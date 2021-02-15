package piuk.blockchain.android.ui.transactionflow.flow

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.fragment.app.DialogFragment
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.NullAddress
import piuk.blockchain.android.databinding.DialogTxFlowEnterAmountBinding
import piuk.blockchain.android.ui.customviews.CurrencyType
import piuk.blockchain.android.ui.customviews.FiatCryptoInputView
import piuk.blockchain.android.ui.customviews.FiatCryptoViewConfiguration
import piuk.blockchain.android.ui.customviews.PrefixedOrSuffixedEditText
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.plugin.SmallBalanceView
import piuk.blockchain.android.ui.transactionflow.plugin.TxFlowWidget
import piuk.blockchain.android.util.setAssetIconColours
import piuk.blockchain.android.util.setCoinIcon
import piuk.blockchain.android.util.gone
import timber.log.Timber
import java.math.RoundingMode

class EnterAmountSheet : TransactionFlowSheet<DialogTxFlowEnterAmountBinding>() {

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogTxFlowEnterAmountBinding =
        DialogTxFlowEnterAmountBinding.inflate(inflater, container, false)

    private val customiser: TransactionFlowCustomiser by inject()
    private val compositeDisposable = CompositeDisposable()

    private var lowerSlot: TxFlowWidget? = null

    private val imm: InputMethodManager by lazy {
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FloatingBottomSheet)
    }

    @SuppressLint("SetTextI18n")
    override fun render(newState: TransactionState) {
        Timber.d("!TRANSACTION!> Rendering! EnterAmountSheet")
        cacheState(newState)
        with(binding) {
            amountSheetCtaButton.isEnabled = newState.nextEnabled

            if (!amountSheetInput.configured) {
                newState.pendingTx?.selectedFiat?.let {
                    amountSheetInput.configure(newState, customiser.defInputType(state, it))
                }
            }

            val availableBalance = newState.availableBalance
            if (availableBalance.isPositive || availableBalance.isZero) {
                // The maxLimit set here controls the number of digits that can be entered,
                // but doesn't restrict the input to be always under that value. Which might be
                // strange UX, but is currently by design.
                if (amountSheetInput.configured) {
                    amountSheetInput.maxLimit = newState.availableBalance
                    if (amountSheetInput.customInternalExchangeRate != newState.fiatRate)
                        amountSheetInput.customInternalExchangeRate = newState.fiatRate
                }
            }

            if (state.setMax) {
                amountSheetInput.updateValue(state.maxSpendable)
            }

            amountSheetTitle.text = customiser.enterAmountTitle(newState)

            lowerSlot?.update(newState)

            updatePendingTxDetails(newState)

            customiser.issueFlashMessage(newState, amountSheetInput.configuration.inputCurrency)?.let {
                when (customiser.selectIssueType(newState)) {
                    IssueType.ERROR -> amountSheetInput.showError(it, customiser.shouldDisableInput(state.errorState))
                    IssueType.INFO -> amountSheetInput.showInfo(it) {
                        dismiss()
                        KycNavHostActivity.start(requireActivity(), CampaignType.Swap, true)
                    }
                }
            } ?: amountSheetInput.hideLabels()

            if (!newState.canGoBack) {
                amountSheetBack.gone()
            }
        }
    }

    override fun initControls(binding: DialogTxFlowEnterAmountBinding) {
        binding.apply {
            amountSheetCtaButton.setOnClickListener {
                analyticsHooks.onEnterAmountCtaClick(state)
                onCtaClick()
            }
            amountSheetBack.setOnClickListener {
                analyticsHooks.onStepBackClicked(state)
                model.process(TransactionIntent.InvalidateTransaction)
            }

            lowerSlot = customiser.installEnterAmountLowerSlotView(requireContext(), frameLowerSlot).apply {
                initControl(model, customiser, analyticsHooks)
            }
        }

        compositeDisposable += binding.amountSheetInput.amount.subscribe { amount ->
            state.fiatRate?.let { rate ->
                check(state.pendingTx != null) { "Px is not initialised yet" }
                model.process(
                    TransactionIntent.AmountChanged(
                        if (!state.allowFiatInput && amount is FiatValue) {
                            convertFiatToCrypto(amount, rate, state).also {
                                binding.amountSheetInput.fixExchange(it)
                            }
                        } else {
                            amount
                        }
                    )
                )
            }
        }

        compositeDisposable += binding.amountSheetInput
            .onImeAction
            .subscribe {
                when (it) {
                    PrefixedOrSuffixedEditText.ImeOptions.NEXT -> {
                        if (state.nextEnabled) {
                            onCtaClick()
                        }
                    }
                    PrefixedOrSuffixedEditText.ImeOptions.BACK -> {
                        hideKeyboard()
                        dismiss()
                    }
                    else -> {
                        // do nothing
                    }
                }
            }

        compositeDisposable += binding.amountSheetInput.onInputToggle.subscribe {
            analyticsHooks.onCryptoToggle(it, state)
        }
    }

    // in this method we try to convert the fiat value coming out from
    // the view to a crypto which is withing the min and max limits allowed.
    // We use floor rounding for max and ceiling for min just to make sure that we wont have problem with rounding once
    // the amount reach the engine where the comparison with limits will happen.

    private fun convertFiatToCrypto(
        amount: FiatValue,
        rate: ExchangeRate.CryptoToFiat,
        state: TransactionState
    ): Money {
        val min = state.pendingTx?.minLimit ?: return rate.inverse().convert(amount)
        val max = state.maxSpendable
        val roundedUpAmount = rate.inverse(RoundingMode.CEILING, state.asset.userDp)
            .convert(amount)
        val roundedDownAmount = rate.inverse(RoundingMode.FLOOR, state.asset.userDp)
            .convert(amount)
        return if (roundedUpAmount >= min && roundedUpAmount <= max)
            roundedUpAmount
        else roundedDownAmount
    }

    private fun updatePendingTxDetails(state: TransactionState) {
        with(binding) {
            amountSheetAssetIcon.setCoinIcon(state.sendingAccount.asset)

            if (customiser.showTargetIcon(state)) {
                (state.selectedTarget as? CryptoAccount)?.let {
                    amountSheetTargetIcon.setCoinIcon(it.asset)
                }
            } else {
                amountSheetTargetIcon.gone()
            }

            amountSheetAssetDirection.setImageResource(customiser.enterAmountActionIcon(state))
            if (customiser.enterAmountActionIconCustomisation(state)) {
                amountSheetAssetDirection.setAssetIconColours(state.asset, requireContext())
            }
        }

        updateSourceAndTargetDetails(state)
    }

    private fun updateSourceAndTargetDetails(state: TransactionState) {
        if (state.selectedTarget is NullAddress)
            return
        with(binding) {
            amountSheetFrom.text = customiser.enterAmountSourceLabel(state)
            amountSheetTo.text = customiser.enterAmountTargetLabel(state)
        }
    }

    private fun onCtaClick() {
        hideKeyboard()
        model.process(TransactionIntent.PrepareTransaction)
    }

    private fun hideKeyboard() {
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun FiatCryptoInputView.configure(
        newState: TransactionState,
        inputCurrency: CurrencyType
    ) {
        if (inputCurrency is CurrencyType.Crypto || newState.amount.isPositive) {
            val selectedFiat = newState.pendingTx?.selectedFiat ?: return
            configuration = FiatCryptoViewConfiguration(
                inputCurrency = CurrencyType.Crypto(newState.sendingAccount.asset),
                exchangeCurrency = CurrencyType.Fiat(selectedFiat),
                predefinedAmount = newState.amount
            )
        } else {
            val selectedFiat = newState.pendingTx?.selectedFiat ?: return
            val fiatRate = newState.fiatRate ?: return
            val amount = newState.amount as? CryptoValue ?: return
            configuration = FiatCryptoViewConfiguration(
                inputCurrency = CurrencyType.Fiat(selectedFiat),
                exchangeCurrency = CurrencyType.Crypto(newState.sendingAccount.asset),
                predefinedAmount = fiatRate.applyRate(amount)
            )
        }
        showKeyboard()
    }

    private fun showKeyboard() {
        val inputView = binding.amountSheetInput.findViewById<PrefixedOrSuffixedEditText>(
            R.id.enter_amount
        )
        inputView?.run {
            requestFocus()
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }
}

// todo This should be an actual method on the customiser, but for now:
fun TransactionFlowCustomiser.installEnterAmountLowerSlotView(ctx: Context, frame: FrameLayout): TxFlowWidget {
    return SmallBalanceView(ctx).also {
        frame.addView(it)
    }
}
