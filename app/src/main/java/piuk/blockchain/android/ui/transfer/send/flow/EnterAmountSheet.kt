package piuk.blockchain.android.ui.transfer.send.flow

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.DialogFragment
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.FiatValue
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.dialog_send_enter_amount.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.customviews.CurrencyType
import piuk.blockchain.android.ui.customviews.FiatCryptoViewConfiguration
import piuk.blockchain.android.ui.customviews.PrefixedOrSuffixedEditText
import piuk.blockchain.android.ui.transfer.send.FlowInputSheet
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.util.setAssetIconColours
import piuk.blockchain.android.util.setCoinIcon
import piuk.blockchain.androidcoreui.utils.extensions.gone
import timber.log.Timber

class EnterAmountSheet(
    host: SlidingModalBottomDialog.Host
) : FlowInputSheet(host) {
    override val layoutResource: Int = R.layout.dialog_send_enter_amount

    private var state: SendState = SendState()
    private val currencyPrefs: CurrencyPrefs by scopedInject()
    private val customiser: SendFlowCustomiser by inject()
    private val compositeDisposable = CompositeDisposable()

    private val imm: InputMethodManager by lazy {
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FloatingBottomSheet)
    }

    @SuppressLint("SetTextI18n")
    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! EnterAmountSheet")
        state = newState
        with(dialogView) {
            amount_sheet_cta_button.isEnabled = newState.nextEnabled

            if (!amount_sheet_input.isConfigured) {
                amount_sheet_input.configuration = FiatCryptoViewConfiguration(
                    input = CurrencyType.Crypto,
                    output = CurrencyType.Crypto,
                    fiatCurrency = currencyPrefs.selectedFiatCurrency,
                    cryptoCurrency = newState.sendingAccount.asset,
                    predefinedAmount = newState.sendAmount
                )
                showKeyboard()
            }

            val availableBalance = newState.availableBalance
            if (availableBalance.isPositive || availableBalance.isZero) {
                // The maxLimit set here controls the number of digits that can be entered,
                // but doesn't restrict the input to be always under that value. Which might be
                // strange UX, but is currently by design.
                amount_sheet_input.maxLimit = newState.availableBalance

                newState.fiatRate?.let { rate ->
                    amount_sheet_max_available.text =
                        "${rate.convert(availableBalance).toStringWithSymbol()} " +
                                "(${availableBalance.toStringWithSymbol()})"
                }
            }

            amount_sheet_title.text = customiser.enterAmountTitle(newState)
            amount_sheet_use_max.text = customiser.enterAmountMaxButton(newState)

            updatePendingTxDetails(newState)

            customiser.errorFlashMessage(newState)?.let {
                amount_sheet_input.showError(it)
            } ?: amount_sheet_input.hideError()

            if (!newState.canGoBack) {
                amount_sheet_back.gone()
            }
        }
    }

    override fun initControls(view: View) {
        view.apply {
            amount_sheet_use_max.setOnClickListener { onUseMaxClick() }
            amount_sheet_cta_button.setOnClickListener { onCtaClick() }
            amount_sheet_back.setOnClickListener {
                model.process(SendIntent.InvalidateTransaction)
            }
        }

        compositeDisposable += view.amount_sheet_input.amount.subscribe { amount ->
            state.fiatRate?.let { rate ->
                model.process(
                    SendIntent.SendAmountChanged(
                        if (!state.allowFiatInput && amount is FiatValue) {
                            rate.inverse().convert(amount)
                        } else {
                            amount
                        }
                    )
                )
            }
        }
    }

    private fun updatePendingTxDetails(state: SendState) {
        with(dialogView) {
            amount_sheet_asset_icon.setCoinIcon(state.sendingAccount.asset)

            amount_sheet_asset_direction.setImageResource(customiser.enterAmountActionIcon(state))
            amount_sheet_asset_direction.setAssetIconColours(state.sendingAccount.asset, context)

            amount_sheet_from.text =
                getString(R.string.send_enter_amount_from, state.sendingAccount.label)
            amount_sheet_to.text =
                getString(R.string.send_enter_amount_to, state.sendTarget.label)
        }
    }

    private fun onUseMaxClick() {
        dialogView.amount_sheet_input.showValue(state.availableBalance)
    }

    private fun onCtaClick() {
        imm.hideSoftInputFromWindow(dialogView.windowToken, 0)
        model.process(SendIntent.PrepareTransaction)
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