package piuk.blockchain.android.simplebuy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.extensions.exhaustive
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.CurrencyChangedFromBuyForm
import com.blockchain.notifications.analytics.PaymentMethodSelected
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import com.blockchain.notifications.analytics.buyConfirmClicked
import com.blockchain.notifications.analytics.cryptoChanged
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_simple_buy_buy_crypto.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.cards.CardDetailsActivity
import piuk.blockchain.android.cards.CardDetailsActivity.Companion.ADD_CARD_REQUEST_CODE
import piuk.blockchain.android.cards.icon
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.ui.customviews.CurrencyType
import piuk.blockchain.android.ui.customviews.FiatCryptoViewConfiguration
import piuk.blockchain.android.ui.dashboard.sheets.LinkBankAccountDetailsBottomSheet
import piuk.blockchain.android.util.assetName
import piuk.blockchain.android.util.drawableResFilled
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf

class SimpleBuyCryptoFragment : MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState>(),
    SimpleBuyScreen,
    PaymentMethodChangeListener,
    ChangeCurrencyHost {

    override val model: SimpleBuyModel by scopedInject()
    private val exchangeRateDataManager: ExchangeRateDataManager by scopedInject()

    private var lastState: SimpleBuyState? = null
    private val compositeDesposable = CompositeDisposable()

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator)
            ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    private val currencyPrefs: CurrencyPrefs by inject()

    override fun onBackPressed(): Boolean = true
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_simple_buy_buy_crypto)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.setupToolbar(R.string.simple_buy_buy_crypto_title)
        model.process(SimpleBuyIntent.FetchBuyLimits(currencyPrefs.selectedFiatCurrency))
        model.process(SimpleBuyIntent.FlowCurrentScreen(FlowScreen.ENTER_AMOUNT))
        model.process(SimpleBuyIntent.FetchSuggestedPaymentMethod(currencyPrefs.selectedFiatCurrency))
        model.process(SimpleBuyIntent.FetchSupportedFiatCurrencies)
        analytics.logEvent(SimpleBuyAnalytics.BUY_FORM_SHOWN)

        compositeDesposable += input_amount.amount.subscribe {
            when (it) {
                is FiatValue -> model.process(SimpleBuyIntent.AmountUpdated(it))
                else -> throw IllegalStateException("CryptoValue is not supported as input yet")
            }
        }

        btn_continue.setOnClickListener {
            model.process(SimpleBuyIntent.BuyButtonClicked)
            model.process(SimpleBuyIntent.CancelOrderIfAnyAndCreatePendingOne)
            analytics.logEvent(buyConfirmClicked(
                lastState?.order?.amount?.valueMinor.toString(),
                lastState?.fiatCurrency ?: "")
            )
        }

        payment_method_details_root.setOnClickListener {
            lastState?.paymentOptions?.let {
                showBottomSheet(PaymentMethodChooserBottomSheet.newInstance(it.availablePaymentMethods.filterNot {
                    it is PaymentMethod.Undefined
                },
                    it.canAddCard, it.canLinkFunds))
            }
        }
    }

    override fun onFiatCurrencyChanged(fiatCurrency: String) {
        if (fiatCurrency == lastState?.fiatCurrency) return
        model.process(SimpleBuyIntent.FiatCurrencyUpdated(fiatCurrency))
        model.process(SimpleBuyIntent.FetchBuyLimits(fiatCurrency))
        model.process(SimpleBuyIntent.FetchSuggestedPaymentMethod(currencyPrefs.selectedFiatCurrency))
        analytics.logEvent(CurrencyChangedFromBuyForm(fiatCurrency))
    }

    override fun onCryptoCurrencyChanged(currency: CryptoCurrency) {
        model.process(SimpleBuyIntent.NewCryptoCurrencySelected(currency))
        analytics.logEvent(cryptoChanged(currency))
        input_amount.configuration = input_amount.configuration.copy(
            cryptoCurrency = currency,
            predefinedAmount = CryptoValue.zero(currency)
        )
    }

    override fun render(newState: SimpleBuyState) {
        lastState = newState

        if (newState.errorState != null) {
            showErrorState(newState.errorState)
            return
        }
        newState.selectedCryptoCurrency?.let {
            if (!input_amount.isConfigured) {
                input_amount.configuration = FiatCryptoViewConfiguration(
                    input = CurrencyType.Fiat,
                    output = CurrencyType.Fiat,
                    fiatCurrency = newState.fiatCurrency,
                    cryptoCurrency = it,
                    canSwap = false,
                    predefinedAmount = newState.order.amount ?: FiatValue.zero(newState.fiatCurrency)
                )
            }
        }
        newState.selectedCryptoCurrency?.let {
            crypto_icon.setImageResource(it.drawableResFilled())
            crypto_text.setText(it.assetName())
        }

        newState.exchangePrice?.let {
            crypto_exchange_rate.text =
                "1 ${newState.selectedCryptoCurrency?.displayTicker} = ${it.toStringWithSymbol()}"
        }

        arrow.visibleIf { newState.availableCryptoCurrencies.size > 1 }

        input_amount.maxLimit = newState.maxFiatAmount

        newState.selectedPaymentMethodDetails?.let {
            renderPaymentMethod(it)
        } ?: hidePaymentMethod()

        btn_continue.isEnabled = canContinue(newState)
        newState.error?.let {
            handleError(it, newState)
        } ?: kotlin.run {
            clearError()
        }

        coin_selector.takeIf { newState.availableCryptoCurrencies.size > 1 }?.setOnClickListener {
            showBottomSheet(
                CryptoCurrencyChooserBottomSheet
                    .newInstance(newState.availableCryptoCurrencies)
            )
        }

        if (newState.confirmationActionRequested &&
            newState.kycVerificationState != null &&
            newState.orderState == OrderState.PENDING_CONFIRMATION
        ) {
            when (newState.kycVerificationState) {
                // Kyc state unknown because error, or gold docs unsubmitted
                KycState.PENDING -> {
                    model.process(SimpleBuyIntent.ConfirmationHandled)
                    model.process(SimpleBuyIntent.KycStarted)
                    navigator().startKyc()
                    analytics.logEvent(SimpleBuyAnalytics.START_GOLD_FLOW)
                }
                // Awaiting results state
                KycState.IN_REVIEW,
                KycState.UNDECIDED -> {
                    navigator().goToKycVerificationScreen()
                }
                // Got results, kyc verification screen will show error
                KycState.VERIFIED_BUT_NOT_ELIGIBLE,
                KycState.FAILED -> {
                    navigator().goToKycVerificationScreen()
                }
                // We have done kyc and are verified
                KycState.VERIFIED_AND_ELIGIBLE -> {
                    navigator().goToCheckOutScreen()
                }
            }.exhaustive
        }
    }

    private fun hidePaymentMethod() {
        payment_method.gone()
        payment_method_separator.gone()
        payment_method_details_root.gone()
    }

    private fun canContinue(state: SimpleBuyState) =
        state.isAmountValid && state.selectedPaymentMethod != null

    private fun renderPaymentMethod(selectedPaymentMethod: PaymentMethod) {

        when (selectedPaymentMethod) {
            is PaymentMethod.Undefined -> {
                payment_method_icon.setImageResource(R.drawable.ic_add_payment_method)
            }
            is PaymentMethod.BankTransfer -> renderBankPayment(selectedPaymentMethod)
            is PaymentMethod.Card -> renderCardPayment(selectedPaymentMethod)
            is PaymentMethod.Funds -> renderFundsPayment(selectedPaymentMethod)
            is PaymentMethod.UndefinedCard -> renderUndefinedCardPayment(selectedPaymentMethod)
        }
        payment_method.visible()
        payment_method_separator.visible()
        payment_method_details_root.visible()
        undefined_payment_text.visibleIf { selectedPaymentMethod is PaymentMethod.Undefined }
        payment_method_title.visibleIf { (selectedPaymentMethod is PaymentMethod.Undefined).not() }
        payment_method_limit.visibleIf { (selectedPaymentMethod is PaymentMethod.Undefined).not() }
    }

    private fun renderFundsPayment(paymentMethod: PaymentMethod.Funds) {
        payment_method_icon.setImageResource(
            paymentMethod.icon()
        )
        payment_method_title.text = getString(paymentMethod.label())

        payment_method_limit.text =
            getString(R.string.payment_method_limit, paymentMethod.limits.max.toStringWithSymbol())
    }

    private fun renderUndefinedCardPayment(selectedPaymentMethod: PaymentMethod.UndefinedCard) {
        payment_method_icon.setImageResource(R.drawable.ic_payment_card)
        payment_method_title.text = getString(R.string.credit_or_debit_card)
        payment_method_limit.text =
            getString(R.string.payment_method_limit, selectedPaymentMethod.limits.max.toStringWithSymbol())
    }

    private fun renderCardPayment(selectedPaymentMethod: PaymentMethod.Card) {
        payment_method_icon.setImageResource(selectedPaymentMethod.cardType.icon())
        payment_method_title.text = selectedPaymentMethod.uiLabelWithDigits()
        payment_method_limit.text =
            getString(R.string.payment_method_limit, selectedPaymentMethod.limits.max.toStringWithSymbol())
    }

    private fun renderBankPayment(selectedPaymentMethod: PaymentMethod.BankTransfer) {
        payment_method_title.text = getString(R.string.bank_wire_transfer)
        payment_method_icon.setImageResource(R.drawable.ic_bank_transfer)
        payment_method_limit.text =
            getString(R.string.payment_method_limit, selectedPaymentMethod.limits.max.toStringWithSymbol())
    }

    private fun clearError() {
        input_amount.hideError()
    }

    private fun showErrorState(errorState: ErrorState) {
        showBottomSheet(ErrorSlidingBottomDialog.newInstance(activity))
    }

    private fun handleError(error: InputError, state: SimpleBuyState) {
        when (error) {
            InputError.ABOVE_MAX -> {
                input_amount.showError(
                    if (input_amount.configuration.input == CurrencyType.Fiat)
                        resources.getString(R.string.maximum_buy, state.maxFiatAmount.toStringWithSymbol())
                    else
                        resources.getString(R.string.maximum_buy,
                            state.maxCryptoAmount(exchangeRateDataManager)?.toStringWithSymbol())
                )
            }
            InputError.BELOW_MIN -> {
                input_amount.showError(
                    if (input_amount.configuration.input == CurrencyType.Fiat)
                        resources.getString(R.string.minimum_buy, state.minFiatAmount.toStringWithSymbol())
                    else
                        resources.getString(R.string.minimum_buy,
                            state.minCryptoAmount(exchangeRateDataManager)?.toStringWithSymbol())
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        model.process(SimpleBuyIntent.ConfirmationHandled)
    }

    override fun onSheetClosed() {
        model.process(SimpleBuyIntent.ClearError)
    }

    override fun onPaymentMethodChanged(paymentMethod: PaymentMethod) {
        model.process(SimpleBuyIntent.SelectedPaymentMethodUpdate(paymentMethod))
        analytics.logEvent(PaymentMethodSelected(
            if (paymentMethod is PaymentMethod.BankTransfer) BANK_ANALYTICS
            else CARD_ANALYTICS
        ))
    }

    override fun addPaymentMethod(type: PaymentMethodType) {
        when (type) {
            PaymentMethodType.PAYMENT_CARD -> {
                val intent = Intent(activity, CardDetailsActivity::class.java)
                startActivityForResult(intent, ADD_CARD_REQUEST_CODE)
                analytics.logEvent(PaymentMethodSelected(NEW_CARD_ANALYTICS))
            }
            PaymentMethodType.FUNDS -> {
                showBottomSheet(LinkBankAccountDetailsBottomSheet.newInstance(
                    lastState?.fiatCurrency ?: return,
                    false
                ))
            }
            else -> {
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_CARD_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            model.process(
                SimpleBuyIntent.FetchSuggestedPaymentMethod(currencyPrefs.selectedFiatCurrency,
                    (data?.extras?.getSerializable(CardDetailsActivity.CARD_KEY) as? PaymentMethod.Card)?.id
                ))
        }
    }

    companion object {
        private const val BANK_ANALYTICS = "BANK"
        private const val CARD_ANALYTICS = "CARD"
        private const val NEW_CARD_ANALYTICS = "CARD"
    }
}

interface PaymentMethodChangeListener {
    fun onPaymentMethodChanged(paymentMethod: PaymentMethod)
    fun addPaymentMethod(type: PaymentMethodType)
}

interface ChangeCurrencyHost : SimpleBuyScreen {
    fun onFiatCurrencyChanged(fiatCurrency: String)
    fun onCryptoCurrencyChanged(currency: CryptoCurrency)
}

fun PaymentMethod.Funds.icon() =
    when (fiatCurrency) {
        "GBP" -> R.drawable.ic_funds_gbp
        "EUR" -> R.drawable.ic_euro_funds
        else -> throw IllegalStateException("Unsupported currency")
    }

fun PaymentMethod.Funds.label() =
    when (fiatCurrency) {
        "GBP" -> R.string.pounds
        "EUR" -> R.string.euros
        else -> throw IllegalStateException("Unsupported currency")
    }