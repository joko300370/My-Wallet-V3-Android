package piuk.blockchain.android.ui.transactionflow.flow

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.DialogFragment
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.dialog_tx_flow_enter_amount.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.customviews.CurrencyType
import piuk.blockchain.android.ui.customviews.FiatCryptoInputView
import piuk.blockchain.android.ui.customviews.FiatCryptoViewConfiguration
import piuk.blockchain.android.ui.customviews.PrefixedOrSuffixedEditText
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.util.setAssetIconColours
import piuk.blockchain.android.util.setCoinIcon
import piuk.blockchain.androidcoreui.utils.extensions.gone
import timber.log.Timber

class EnterAmountSheet(
    host: SlidingModalBottomDialog.Host
) : TransactionFlowSheet(host) {
    override val layoutResource: Int = R.layout.dialog_tx_flow_enter_amount

    private val customiser: TransactionFlowCustomiser by inject()
    private val compositeDisposable = CompositeDisposable()

    private val imm: InputMethodManager by lazy {
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FloatingBottomSheet)
    }

    @SuppressLint("SetTextI18n")
    override fun render(newState: TransactionState) {
        Timber.d("!SEND!> Rendering! EnterAmountSheet")
        cacheState(newState)
        with(dialogView) {
            amount_sheet_cta_button.isEnabled = newState.nextEnabled

            if (!amount_sheet_input.isConfigured) {
                amount_sheet_input.configure(newState, customiser.defInputType(state))
            }

            val availableBalance = newState.availableBalance
            if (availableBalance.isPositive || availableBalance.isZero) {
                // The maxLimit set here controls the number of digits that can be entered,
                // but doesn't restrict the input to be always under that value. Which might be
                // strange UX, but is currently by design.
                if (amount_sheet_input.isConfigured) {
                    amount_sheet_input.maxLimit = newState.availableBalance
                    if (amount_sheet_input.exchangeRate != newState.fiatRate)
                        amount_sheet_input.exchangeRate = newState.fiatRate
                }

                newState.fiatRate?.let { rate ->
                    amount_sheet_max_available.text =
                        "${rate.convert(availableBalance).toStringWithSymbol()} " +
                                "(${availableBalance.toStringWithSymbol()})"
                }
            }

            amount_sheet_title.text = customiser.enterAmountTitle(newState)
            amount_sheet_use_max.text = customiser.enterAmountMaxButton(newState)

            updatePendingTxDetails(newState)

            customiser.issueFlashMessage(newState)?.let {
                when (customiser.selectIssueType(newState)) {
                    IssueType.ERROR -> amount_sheet_input.showError(it)
                    IssueType.INFO -> amount_sheet_input.showInfo(it) {
                        dismiss()
                        KycNavHostActivity.start(requireActivity(), CampaignType.Swap)
                    }
                }
            } ?: amount_sheet_input.hideLabels()

            if (!newState.canGoBack) {
                amount_sheet_back.gone()
            }
        }
    }

    override fun initControls(view: View) {
        view.apply {
            amount_sheet_use_max.setOnClickListener {
                analyticsHooks.onMaxClicked(state)
                onUseMaxClick()
            }
            amount_sheet_cta_button.setOnClickListener {
                analyticsHooks.onEnterAmountCtaClick(state)
                onCtaClick()
            }
            amount_sheet_back.setOnClickListener {
                analyticsHooks.onStepBackClicked(state)
                model.process(TransactionIntent.InvalidateTransaction)
            }
        }

        compositeDisposable += view.amount_sheet_input.amount.subscribe { amount ->
            state.fiatRate?.let { rate ->
                model.process(
                    TransactionIntent.AmountChanged(
                        if (!state.allowFiatInput && amount is FiatValue) {
                            rate.inverse().convert(amount)
                        } else {
                            amount
                        }
                    )
                )
            }
        }

        compositeDisposable += view.amount_sheet_input.onImeAction.subscribe {
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

        compositeDisposable += view.amount_sheet_input.onInputToggle.subscribe {
            analyticsHooks.onCryptoToggle(it, state)
        }
    }

    private fun updatePendingTxDetails(state: TransactionState) {
        with(dialogView) {
            amount_sheet_asset_icon.setCoinIcon(state.sendingAccount.asset)

            if (customiser.showTargetIcon(state)) {
                amount_sheet_target_icon.setCoinIcon((state.selectedTarget as CryptoAccount).asset)
                amount_sheet_asset_direction.translationZ = 1f
            } else amount_sheet_target_icon.gone()

            amount_sheet_asset_direction.setImageResource(customiser.enterAmountActionIcon(state))
            amount_sheet_asset_direction.setAssetIconColours(state.sendingAccount.asset, context)
        }

        updateSourceAndTargetDetails(state)
    }

    private fun updateSourceAndTargetDetails(state: TransactionState) {
        with(dialogView) {
            amount_sheet_from.text = customiser.enterAmountSourceLabel(state)
            amount_sheet_to.text = customiser.enterAmountTargetLabel(state)
        }
    }

    private fun onUseMaxClick() {
        dialogView.amount_sheet_input.showValue(state.maxSpendable)
    }

    private fun onCtaClick() {
        hideKeyboard()
        model.process(TransactionIntent.PrepareTransaction)
    }

    private fun hideKeyboard() {
        imm.hideSoftInputFromWindow(dialogView.windowToken, 0)
    }

    private fun FiatCryptoInputView.configure(newState: TransactionState, input: CurrencyType) {
        if (input == CurrencyType.Crypto) {
            newState.pendingTx?.selectedFiat?.let {
                configuration = FiatCryptoViewConfiguration(
                    input = CurrencyType.Crypto,
                    output = CurrencyType.Crypto,
                    fiatCurrency = it,
                    cryptoCurrency = newState.sendingAccount.asset,
                    predefinedAmount = newState.amount
                )
            }
        } else {
            val selectedFiat = newState.pendingTx?.selectedFiat ?: return
            val fiatRate = newState.fiatRate ?: return
            val amount = newState.amount as? CryptoValue ?: return
            configuration = FiatCryptoViewConfiguration(
                input = CurrencyType.Fiat,
                output = CurrencyType.Crypto,
                fiatCurrency = selectedFiat,
                cryptoCurrency = newState.sendingAccount.asset,
                predefinedAmount = fiatRate.applyRate(amount)
            )
        }
        showKeyboard()
    }

    private fun showKeyboard() {
        val inputView = dialogView.amount_sheet_input.findViewById<PrefixedOrSuffixedEditText>(
            R.id.enter_amount)
        inputView?.run {
            requestFocus()
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }
}