package piuk.blockchain.android.ui.transfer.send.flow

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.DialogFragment
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.dialog_send_enter_amount.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.customviews.CurrencyType
import piuk.blockchain.android.ui.customviews.FiatCryptoViewConfiguration
import piuk.blockchain.android.ui.transfer.send.FlowInputSheet
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.util.setAssetIconColours
import piuk.blockchain.android.util.setCoinIcon
import timber.log.Timber

class EnterAmountSheet(
    host: SlidingModalBottomDialog.Host
) : FlowInputSheet(host) {
    override val layoutResource: Int = R.layout.dialog_send_enter_amount

    private var state: SendState = SendState()
    private val currencyPrefs: CurrencyPrefs by scopedInject()
    private val compositeDisposable = CompositeDisposable()

    private val imm: InputMethodManager by lazy {
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogStyle)
    }

    @SuppressLint("SetTextI18n")
    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! EnterAmountSheet")

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
            }

            val balance = newState.availableBalance
            if (balance.isPositive || balance.isZero) {
                amount_sheet_input.maxLimit = newState.availableBalance

                newState.fiatRate?.let { rate ->
                    amount_sheet_max_available.text =
                        "${rate.convert(balance).toStringWithSymbol()} (${balance.toStringWithSymbol()})"
                }
            }

            amount_sheet_asset_icon.setCoinIcon(newState.sendingAccount.asset)
            amount_sheet_asset_direction.setAssetIconColours(newState.sendingAccount.asset, context)

            amount_sheet_from.text =
                getString(R.string.send_enter_amount_from, newState.sendingAccount.label)
            amount_sheet_to.text =
                getString(R.string.send_enter_amount_to, newState.sendTarget.label)

            newState.errorState.toString(
                newState.sendingAccount.asset.networkTicker,
                resources
            )?.let {
                amount_sheet_input.showError(it)
            } ?: amount_sheet_input.hideError()
        }

        state = newState
    }

    override fun initControls(view: View) {
        view.apply {
            amount_sheet_use_max.setOnClickListener { onUseMaxClick() }
            amount_sheet_cta_button.setOnClickListener { onCtaClick() }
            amount_sheet_back.setOnClickListener {
                model.process(SendIntent.ReturnToPreviousStep)
            }
        }

//        // TODO: kill this before shipping, we need to find a better way of showing the keyboard
//        // TODO: tried a ViewTreeObserver - View.post {} - onViewCreated
//        Handler().postDelayed({
//            val inputView = view.amount_sheet_input.findViewById<PrefixedOrSuffixedEditText>(
//                R.id.enter_amount)
//            inputView?.let {
//                inputView.requestFocus()
//                imm.showSoftInput(inputView, InputMethodManager.SHOW_FORCED)
//            }
//        }, 200)

        compositeDisposable += view.amount_sheet_input.amount.subscribe { amount ->
            state.fiatRate?.let { rate ->
                SendIntent.SendAmountChanged(
                    if (amount is FiatValue) {
                        rate.inverse().convert(amount) as CryptoValue
                    } else {
                        amount as CryptoValue
                    }
                )
            }
        }
    }

    private fun onUseMaxClick() {
        dialogView.amount_sheet_input.showValue(state.availableBalance)
    }

    private fun onCtaClick() {
        imm.hideSoftInputFromWindow(dialogView.windowToken, 0)
        model.process(SendIntent.PrepareTransaction)
    }
}