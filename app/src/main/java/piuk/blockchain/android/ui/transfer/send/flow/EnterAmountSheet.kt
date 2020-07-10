package piuk.blockchain.android.ui.transfer.send.flow

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.FiatValue
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_send_enter_amount.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.customviews.CurrencyType
import piuk.blockchain.android.ui.customviews.FiatCryptoViewConfiguration
import piuk.blockchain.android.ui.transfer.send.SendInputSheet
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.util.drawableResFilled
import piuk.blockchain.androidcoreui.utils.helperfunctions.AfterTextChangedWatcher
import timber.log.Timber


class EnterAmountSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_enter_amount

    private var state: SendState = SendState()
    private val currencyPrefs: CurrencyPrefs by scopedInject()
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogStyle)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        compositeDisposable += dialogView.amount_sheet_input.amount.subscribe {
            when (it) {
                is FiatValue -> Timber.e("---- fiat amount changed $it")
                else -> Timber.e("---- crypto amount changed $it")
            }
        }
    }

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! EnterAmountSheet")

        with(dialogView) {
            cta_button.isEnabled = newState.nextEnabled

            //max_available.text = newState.availableBalance.toStringWithSymbol()
            if (!amount_sheet_input.isConfigured) {
                amount_sheet_input.configuration = FiatCryptoViewConfiguration(
                    input = CurrencyType.Crypto,
                    output = CurrencyType.Crypto,
                    fiatCurrency = currencyPrefs.selectedFiatCurrency,
                    cryptoCurrency = newState.sendingAccount.asset,
                    predefinedAmount = FiatValue.zero(currencyPrefs.selectedFiatCurrency)
                )

                amount_sheet_input.hideError()
            }

            amount_sheet_asset_icon.setImageDrawable(ContextCompat.getDrawable(context,
                newState.sendingAccount.asset.drawableResFilled()))
            enter_amount.setText(newState.availableBalance.toStringWithoutSymbol())
            amount_sheet_from.text =
                getString(R.string.send_enter_amount_from, newState.sendingAccount.label)
            amount_sheet_to.text =
                getString(R.string.send_enter_amount_to, newState.targetAddress.label)

            compositeDisposable += newState.sendingAccount.balance.subscribeBy(
                onSuccess = {
                    Timber.e("---- max limit set $it")
                    amount_sheet_input.maxLimit = it
                },
                onError = {
                    Timber.e("--- error getting sendingaccount balance $it")
                }
            )
        }

        state = newState
    }

    override fun initControls(view: View) {
        view.apply {
            enter_amount.addTextChangedListener(amountTextWatcher)
            use_max.setOnClickListener { onUseMaxClick() }
            cta_button.setOnClickListener { onCtaClick() }
            amount_sheet_back.setOnClickListener {
                model.process(SendIntent.ReturnToPreviousStep)
            }

            amount_sheet_input.requestFocus()
        }

        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    private val amountTextWatcher = object : AfterTextChangedWatcher() {
        override fun afterTextChanged(s: Editable?) {
            val txtAmount = s.toString()

            // val r = textToCryptoValue(txtAmount, state.sendingAccount.asset)
            //model.process(SendIntent.SendAmountChanged(r))
        }
    }

    private fun onUseMaxClick() {
        dialogView.amount_sheet_input
        dialogView.enter_amount.setText(state.availableBalance.toStringWithoutSymbol())
    }

    private fun onCtaClick() =
        model.process(SendIntent.PrepareTransaction)

    companion object {
        fun newInstance(): EnterAmountSheet =
            EnterAmountSheet()
    }
}

/*
private fun textToCryptoValue(text: String, ccy: CryptoCurrency): CryptoValue {
    if (text.isEmpty()) return CryptoValue.zero(ccy)

    val decimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator.toString()

    val amount = text.trim { it <= ' ' }
        .replace(" ", "")
        .replace(decimalSeparator, ".")

    return CryptoValue.fromMajor(ccy, amount.toBigDecimal())
}
*/
