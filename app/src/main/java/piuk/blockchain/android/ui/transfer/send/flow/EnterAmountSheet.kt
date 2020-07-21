package piuk.blockchain.android.ui.transfer.send.flow

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
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
import piuk.blockchain.android.ui.transfer.send.SendErrorState
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.util.drawableResFilled
import piuk.blockchain.android.util.setAssetIconColours
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.toCrypto
import timber.log.Timber

class EnterAmountSheet(
    host: SlidingModalBottomDialog.Host
) : FlowInputSheet(host) {
    override val layoutResource: Int = R.layout.dialog_send_enter_amount

    private var state: SendState = SendState()
    private val currencyPrefs: CurrencyPrefs by scopedInject()
    private val exchangeRateDataManager: ExchangeRateDataManager by scopedInject()
    private val compositeDisposable = CompositeDisposable()

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
                    predefinedAmount = FiatValue.zero(currencyPrefs.selectedFiatCurrency)
                )
            }

            if (newState.availableBalance.isPositive) {
                amount_sheet_input.maxLimit = newState.availableBalance

                amount_sheet_max_available.text =
                    "${newState.availableBalance.toFiat(exchangeRateDataManager,
                        currencyPrefs.selectedFiatCurrency)
                        .toStringWithSymbol()} (${newState.availableBalance.toStringWithSymbol()})"
            }

            amount_sheet_asset_icon.setImageDrawable(ContextCompat.getDrawable(context,
                newState.sendingAccount.asset.drawableResFilled()))
            amount_sheet_asset_direction.setAssetIconColours(newState.sendingAccount.asset, context)

            amount_sheet_from.text =
                getString(R.string.send_enter_amount_from, newState.sendingAccount.label)
            amount_sheet_to.text =
                getString(R.string.send_enter_amount_to, newState.sendTarget.label)

            when (newState.errorState) {
                SendErrorState.NONE -> dialogView.amount_sheet_input.hideError()
                SendErrorState.MAX_EXCEEDED -> amount_sheet_input.showError(
                    getString(R.string.send_enter_amount_error_max,
                        newState.sendingAccount.asset.networkTicker))
                SendErrorState.MIN_REQUIRED -> amount_sheet_input.showError(
                    getString(R.string.send_enter_amount_error_min,
                        newState.sendingAccount.asset.networkTicker))
            }
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
//                val imm = requireContext().getSystemService(
//                    Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                imm.showSoftInput(inputView, InputMethodManager.SHOW_FORCED)
//            }
//        }, 200)

        compositeDisposable += view.amount_sheet_input.amount.subscribe {
            when (it) {
                is FiatValue -> model.process(SendIntent.SendAmountChanged(
                    it.toCrypto(exchangeRateDataManager, state.sendingAccount.asset)))
                else -> model.process(SendIntent.SendAmountChanged(it as CryptoValue))
            }
        }
    }

    private fun onUseMaxClick() {
        dialogView.amount_sheet_input.showValue(state.availableBalance)
    }

    private fun onCtaClick() = model.process(SendIntent.PrepareTransaction)
}