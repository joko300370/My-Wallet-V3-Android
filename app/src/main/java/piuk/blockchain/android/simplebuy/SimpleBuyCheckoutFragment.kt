package piuk.blockchain.android.simplebuy

import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.ui.urllinks.ORDER_PRICE_EXPLANATION
import com.blockchain.ui.urllinks.PRIVATE_KEY_EXPLANATION
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentSimplebuyCheckoutBinding
import piuk.blockchain.android.ui.base.ErrorDialogData
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.secondsToDays
import piuk.blockchain.android.util.setOnClickListenerDebounced
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class SimpleBuyCheckoutFragment :
    MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState, FragmentSimplebuyCheckoutBinding>(),
    SimpleBuyScreen,
    SimpleBuyCancelOrderBottomSheet.Host {

    override val model: SimpleBuyModel by scopedInject()

    private val internalFlags: InternalFeatureFlagApi by inject()
    private val stringUtils: StringUtils by inject()

    private var lastState: SimpleBuyState? = null
    private val checkoutAdapter = CheckoutAdapter()
    private val checkoutAdapterDelegate = CheckoutAdapterDelegate()

    private val isForPendingPayment: Boolean by unsafeLazy {
        arguments?.getBoolean(PENDING_PAYMENT_ORDER_KEY, false) ?: false
    }

    private val showOnlyOrderData: Boolean by unsafeLazy {
        arguments?.getBoolean(SHOW_ONLY_ORDER_DATA, false) ?: false
    }

    private val shouldShowNewCheckout: Boolean by lazy {
        internalFlags.isFeatureEnabled(GatedFeature.CHECKOUT)
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSimplebuyCheckoutBinding =
        FragmentSimplebuyCheckoutBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = if (shouldShowNewCheckout) checkoutAdapterDelegate else checkoutAdapter
            addItemDecoration(BlockchainListDividerDecor(requireContext()))
        }

        model.process(SimpleBuyIntent.FlowCurrentScreen(FlowScreen.CHECKOUT))
        if (!showOnlyOrderData)
            setUpToolbar()

        model.process(SimpleBuyIntent.FetchQuote)
        model.process(SimpleBuyIntent.FetchWithdrawLockTime)
    }

    private fun setUpToolbar() {
        activity.setupToolbar(
            if (isForPendingPayment) {
                R.string.order_details
            } else {
                R.string.checkout
            },
            !isForPendingPayment
        )
    }

    override fun backPressedHandled(): Boolean = isForPendingPayment

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator) ?: throw IllegalStateException(
            "Parent must implement SimpleBuyNavigator"
        )

    override fun onBackPressed(): Boolean = true

    override fun render(newState: SimpleBuyState) {
        // Event should be triggered only the first time a state is rendered
        if (lastState == null) {
            analytics.logEvent(
                eventWithPaymentMethod(
                    SimpleBuyAnalytics.CHECKOUT_SUMMARY_SHOWN,
                    newState.selectedPaymentMethod?.paymentMethodType?.toAnalyticsString().orEmpty()
                )
            )
            lastState = newState
        }

        newState.selectedCryptoCurrency?.let { renderPrivateKeyLabel(it) }
        binding.progress.visibleIf { newState.isLoading }
        val payment = newState.selectedPaymentMethod
        val note = when {
            payment?.isCard() == true -> showWithdrawalPeriod(newState)
            payment?.isFunds() == true -> getString(R.string.purchase_funds_note)
            payment?.isBank() == true -> showWithdrawalPeriod(newState)
            else -> ""
        }

        binding.purchaseNote.apply {
            if (note.isNullOrBlank())
                gone()
            else {
                visible()
                text = note
            }
        }

        if (newState.errorState != null) {
            showErrorState(newState.errorState)
            return
        }

        showAmountForMethod(newState)

        updateStatusPill(newState)

        if (shouldShowNewCheckout) {
            if (newState.paymentOptions.availablePaymentMethods.isEmpty()) {
                model.process(
                    SimpleBuyIntent.FetchPaymentDetails(
                        newState.fiatCurrency, newState.selectedPaymentMethod?.id.orEmpty()
                    )
                )
            } else {
                checkoutAdapterDelegate.items = getCheckoutFields(newState)
            }
        } else {
            checkoutAdapter.items = getListFields(newState)
        }

        configureButtons(newState)

        when (newState.order.orderState) {
            OrderState.FINISHED, // Funds orders are getting finished right after confirmation
            OrderState.AWAITING_FUNDS -> {
                if (newState.confirmationActionRequested) {
                    navigator().goToPaymentScreen()
                }
            }
            OrderState.CANCELED -> {
                if (activity is SmallSimpleBuyNavigator) {
                    (activity as SmallSimpleBuyNavigator).exitSimpleBuyFlow()
                } else {
                    navigator().exitSimpleBuyFlow()
                }
            }
            else -> {
                // do nothing
            }
        }
    }

    private fun renderPrivateKeyLabel(selectedCryptoCurrency: CryptoCurrency) {
        if (selectedCryptoCurrency.hasFeature(CryptoCurrency.CUSTODIAL_ONLY)) {
            val map = mapOf("learn_more_link" to Uri.parse(PRIVATE_KEY_EXPLANATION))
            val learnMoreLink = stringUtils.getStringWithMappedAnnotations(
                R.string.common_linked_learn_more,
                map,
                requireContext()
            )

            val sb = SpannableStringBuilder()
            val privateKeyExplanation =
                getString(R.string.checkout_item_private_key_wallet_explanation, selectedCryptoCurrency.displayTicker)
            sb.append(privateKeyExplanation)
                .append(learnMoreLink)
                .setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(activity, R.color.blue_600)),
                    privateKeyExplanation.length, privateKeyExplanation.length + learnMoreLink.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            binding.privateKeyExplanation.apply {
                setText(sb, TextView.BufferType.SPANNABLE)
                movementMethod = LinkMovementMethod.getInstance()
                visible()
            }
        }
    }

    private fun showWithdrawalPeriod(newState: SimpleBuyState) =
        newState.withdrawalLockPeriod.secondsToDays().takeIf { it > 0 }?.let {
            getString(R.string.security_locked_funds_bank_transfer_explanation, it.toString())
        }

    private fun showAmountForMethod(newState: SimpleBuyState) {
        binding.amount.text = newState.orderValue?.toStringWithSymbol()
        binding.amountFiat.text = newState.order.amount?.toStringWithSymbol()
    }

    private fun updateStatusPill(newState: SimpleBuyState) {
        with(binding.status) {
            when {
                isPendingOrAwaitingFunds(newState.orderState) -> {
                    text = getString(R.string.order_pending)
                    background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.bkgd_status_unconfirmed)
                    setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.grey_800)
                    )
                }
                newState.orderState == OrderState.FINISHED -> {
                    text = getString(R.string.order_complete)
                    background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.bkgd_green_100_rounded)
                    setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.green_600)
                    )
                }
                else -> {
                    gone()
                }
            }
        }
    }

    private fun getListFields(state: SimpleBuyState) =
        listOfNotNull(
            CheckoutItem(
                getString(R.string.quote_price, state.selectedCryptoCurrency?.displayTicker),
                state.orderExchangePrice?.toStringWithSymbol() ?: ""
            ),

            CheckoutItem(
                getString(R.string.fee),
                state.fee?.toStringWithSymbol() ?: FiatValue.zero(state.fiatCurrency)
                    .toStringWithSymbol()
            ),

            CheckoutItem(
                getString(R.string.common_total),
                state.order.amount?.toStringWithSymbol() ?: ""
            ),

            CheckoutItem(getString(R.string.payment_method),
                state.selectedPaymentMethod?.let {
                    paymentMethodLabel(it, state.fiatCurrency)
                } ?: ""
            ),
            state.selectedPaymentMethod?.isBank()?.let {
                CheckoutItem(
                    getString(R.string.available_to_trade),
                    getString(R.string.instantly)
                )
            }
        )

    private fun getCheckoutFields(state: SimpleBuyState): List<SimpleBuyCheckoutItem> {
        val linksMap = mapOf<String, Uri>(
            "learn_more" to Uri.parse(ORDER_PRICE_EXPLANATION)
        )

        val priceExplanation = stringUtils.getStringWithMappedAnnotations(
            R.string.checkout_item_price_blurb,
            linksMap,
            requireContext()
        )

        return listOfNotNull(
            SimpleBuyCheckoutItem.ExpandableCheckoutItem(
                getString(R.string.quote_price, state.selectedCryptoCurrency?.displayTicker),
                state.orderExchangePrice?.toStringWithSymbol().orEmpty(),
                priceExplanation
            ),
            buildPaymentMethodItem(state),
            SimpleBuyCheckoutItem.SimpleCheckoutItem(
                getString(R.string.purchase),
                state.order.amount.addStringWithSymbolOrDefault(state.fiatCurrency)
            ),

            buildPaymentFee(
                state, stringUtils.getStringWithMappedAnnotations(
                    R.string.checkout_item_price_fee,
                    linksMap,
                    requireContext()
                )
            ),

            SimpleBuyCheckoutItem.SimpleCheckoutItem(
                getString(R.string.common_total),
                (state.order.amount?.plus(state.fee ?: FiatValue.zero(state.fiatCurrency)) as? FiatValue)
                    .addStringWithSymbolOrDefault(state.fiatCurrency), true
            )
        )
    }

    private fun FiatValue?.addStringWithSymbolOrDefault(fiatCurrency: String): String {
        return this?.toStringWithSymbol() ?: FiatValue.zero(fiatCurrency).toStringWithSymbol()
    }

    private fun buildPaymentMethodItem(state: SimpleBuyState): SimpleBuyCheckoutItem? =
        state.selectedPaymentMethod?.let {
            when (it.paymentMethodType) {
                PaymentMethodType.FUNDS -> SimpleBuyCheckoutItem.SimpleCheckoutItem(
                    getString(R.string.payment_method),
                    getString(R.string.fiat_currency_funds_wallet_name_1, state.fiatCurrency)
                )
                PaymentMethodType.BANK_TRANSFER,
                PaymentMethodType.BANK_ACCOUNT,
                PaymentMethodType.PAYMENT_CARD -> {
                    state.selectedPaymentMethodDetails?.let { details ->
                        SimpleBuyCheckoutItem.ComplexCheckoutItem(
                            getString(R.string.payment_method),
                            details.methodDetails(),
                            details.methodName()
                        )
                    }
                }
                PaymentMethodType.UNKNOWN -> null
            }
        }

    private fun buildPaymentFee(state: SimpleBuyState, feeExplanation: CharSequence): SimpleBuyCheckoutItem? =
        state.fee?.let { fee ->
            if (!fee.isZero) {
                SimpleBuyCheckoutItem.ExpandableCheckoutItem(
                    if (state.selectedPaymentMethod?.paymentMethodType == PaymentMethodType.PAYMENT_CARD) {
                        getString(R.string.card_fee)
                    } else {
                        getString(R.string.fee)
                    },
                    fee.toStringWithSymbol(),
                    feeExplanation
                )
            } else {
                null
            }
        }

    private fun isPendingOrAwaitingFunds(orderState: OrderState) =
        isForPendingPayment || orderState == OrderState.AWAITING_FUNDS

    private fun configureButtons(state: SimpleBuyState) {
        val isOrderAwaitingFunds = state.orderState == OrderState.AWAITING_FUNDS

        with(binding) {
            buttonAction.apply {
                if (!isForPendingPayment && !isOrderAwaitingFunds) {
                    text = getString(R.string.buy_now_1, state.orderValue?.toStringWithSymbol())
                    setOnClickListener {
                        model.process(SimpleBuyIntent.ConfirmOrder)
                        analytics.logEvent(
                            eventWithPaymentMethod(
                                SimpleBuyAnalytics.CHECKOUT_SUMMARY_CONFIRMED,
                                state.selectedPaymentMethod?.paymentMethodType?.toAnalyticsString().orEmpty()
                            )
                        )
                    }
                } else {
                    text = if (isOrderAwaitingFunds && !isForPendingPayment) {
                        getString(R.string.complete_payment)
                    } else {
                        getString(R.string.ok_cap)
                    }
                    setOnClickListener {
                        if (isForPendingPayment) {
                            navigator().exitSimpleBuyFlow()
                        } else {
                            navigator().goToPaymentScreen()
                        }
                    }
                }
                visibleIf { !showOnlyOrderData }
            }

            buttonAction.isEnabled = !state.isLoading
            buttonCancel.visibleIf {
                isOrderAwaitingFunds && state.selectedPaymentMethod?.isBank() == true && isForPendingPayment ||
                    !isForPendingPayment
            }
            buttonCancel.setOnClickListenerDebounced {
                analytics.logEvent(SimpleBuyAnalytics.CHECKOUT_SUMMARY_PRESS_CANCEL)
                showBottomSheet(SimpleBuyCancelOrderBottomSheet.newInstance())
            }
        }
    }

    private fun paymentMethodLabel(
        selectedPaymentMethod: SelectedPaymentMethod,
        fiatCurrency: String
    ): String =
        when (selectedPaymentMethod.paymentMethodType) {
            PaymentMethodType.FUNDS -> getString(R.string.fiat_currency_funds_wallet_name_1, fiatCurrency)
            else -> selectedPaymentMethod.label.orEmpty()
        }

    private fun showErrorState(errorState: ErrorState) {
        when (errorState) {
            ErrorState.DailyLimitExceeded -> showBottomSheet(
                ErrorSlidingBottomDialog.newInstance(
                    ErrorDialogData(
                        getString(R.string.sb_checkout_daily_limit_title),
                        getString(R.string.sb_checkout_daily_limit_blurb),
                        getString(R.string.common_ok)
                    )
                )
            )
            ErrorState.WeeklyLimitExceeded -> showBottomSheet(
                ErrorSlidingBottomDialog.newInstance(
                    ErrorDialogData(
                        getString(R.string.sb_checkout_weekly_limit_title),
                        getString(R.string.sb_checkout_weekly_limit_blurb),
                        getString(R.string.common_ok)
                    )
                )
            )
            ErrorState.YearlyLimitExceeded -> showBottomSheet(
                ErrorSlidingBottomDialog.newInstance(
                    ErrorDialogData(
                        getString(R.string.sb_checkout_yearly_limit_title),
                        getString(R.string.sb_checkout_yearly_limit_blurb),
                        getString(R.string.common_ok)
                    )
                )
            )
            ErrorState.ExistingPendingOrder -> showBottomSheet(
                ErrorSlidingBottomDialog.newInstance(
                    ErrorDialogData(
                        getString(R.string.sb_checkout_pending_order_title),
                        getString(R.string.sb_checkout_pending_order_blurb),
                        getString(R.string.common_ok)
                    )
                )
            )
            else -> showBottomSheet(ErrorSlidingBottomDialog.newInstance(activity))
        }
    }

    override fun cancelOrderConfirmAction(cancelOrder: Boolean, orderId: String?) {
        if (cancelOrder) {
            model.process(SimpleBuyIntent.CancelOrder)
            analytics.logEvent(
                eventWithPaymentMethod(
                    SimpleBuyAnalytics.CHECKOUT_SUMMARY_CANCELLATION_CONFIRMED,
                    lastState?.selectedPaymentMethod?.paymentMethodType?.toAnalyticsString().orEmpty()
                )
            )
        } else {
            analytics.logEvent(SimpleBuyAnalytics.CHECKOUT_SUMMARY_CANCELLATION_GO_BACK)
        }
    }

    override fun onPause() {
        super.onPause()
        model.process(SimpleBuyIntent.NavigationHandled)
    }

    override fun onSheetClosed() {
        // do nothing
    }

    companion object {
        private const val PENDING_PAYMENT_ORDER_KEY = "PENDING_PAYMENT_KEY"
        private const val SHOW_ONLY_ORDER_DATA = "SHOW_ONLY_ORDER_DATA"

        fun newInstance(
            isForPending: Boolean = false,
            showOnlyOrderData: Boolean = false
        ): SimpleBuyCheckoutFragment {
            val fragment = SimpleBuyCheckoutFragment()
            fragment.arguments = Bundle().apply {
                putBoolean(PENDING_PAYMENT_ORDER_KEY, isForPending)
                putBoolean(SHOW_ONLY_ORDER_DATA, showOnlyOrderData)
            }
            return fragment
        }
    }
}