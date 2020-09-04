package piuk.blockchain.android.withdraw

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import com.blockchain.notifications.analytics.withdrawEventWithCurrency
import com.blockchain.swap.nabu.datamanagers.LinkedBank
import info.blockchain.balance.FiatValue
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_withdraw_enter_amount.*
import kotlinx.android.synthetic.main.fragment_withdraw_enter_amount.input_amount
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.ErrorState
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.ui.customviews.SingleInputViewConfiguration
import piuk.blockchain.android.ui.dashboard.sheets.LinkBankAccountDetailsBottomSheet
import piuk.blockchain.android.withdraw.mvi.WithdrawIntent
import piuk.blockchain.android.withdraw.mvi.WithdrawModel
import piuk.blockchain.android.withdraw.mvi.WithdrawState
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf

class WithdrawEnterAmountFragment : MviFragment<WithdrawModel, WithdrawIntent, WithdrawState>(), WithdrawScreen,
    BankChooserHost {
    override val model: WithdrawModel by scopedInject()
    private val compositeDisposable = CompositeDisposable()
    private lateinit var confirmAmountEvent: () -> AnalyticsEvent

    private val currency: String by unsafeLazy {
        arguments?.getString(CURRENCY_KEY)
            ?: throw java.lang.IllegalStateException("No currency provided")
    }

    override fun navigator(): WithdrawNavigator =
        (activity as? WithdrawNavigator)
            ?: throw IllegalStateException("Parent must implement WithdrawNavigator")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.setupToolbar(R.string.withdraw_title)
        model.process(WithdrawIntent.UpdateCurrency(currency))
        compositeDisposable += input_amount.amount.subscribe {
            (it as? FiatValue)?.let {
                model.process(WithdrawIntent.AmountUpdated(it))
            }
        }

        analytics.logEvent(SimpleBuyAnalytics.WITHDRAWAL_FORM_SHOWN)

        btn_continue.setOnClickListener {
            navigator().goToCheckout()
            analytics.logEvent(confirmAmountEvent())
        }
    }

    override fun onBackPressed(): Boolean = true

    override fun render(newState: WithdrawState) {
        if (newState.errorState != null) {
            showErrorState(newState.errorState)
        }
        newState.availableForWithdraw?.let {
            fiat_balance.text = it.toStringWithSymbol()
        }

        newState.amountError?.let {
            input_amount.showError(getString(R.string.max_with_value,
                newState.availableForWithdraw?.toStringWithSymbol()))
        } ?: input_amount.hideError()

        confirmAmountEvent = {
            withdrawEventWithCurrency(
                analytics = SimpleBuyAnalytics.WITHDRAWAL_CONFIRM_AMOUNT,
                amount = newState.amount?.toBigInteger().toString(),
                currency = newState.currency ?: ""
            )
        }

        newState.currency?.let {
            currency_symbol.setIcon(it)
            withdraw_title.text = getString(R.string.withdraw_currency, newState.currency)
            if (!input_amount.isConfigured) {
                input_amount.configuration = SingleInputViewConfiguration.Fiat(
                    fiatCurrency = it
                )
            }
        }
        bank_details_root.visibleIf { newState.linkedBanks != null }
        newState.linkedBanks?.let {
            if (it.isEmpty()) {
                renderUiForNoBanks(newState.currency ?: return)
            }
        }
        newState.selectedBank?.let {
            renderUiForBank(it, newState.linkedBanks ?: return, newState.currency ?: return)
        }

        btn_continue.isEnabled = newState.selectedBank != null && newState.amountIsValid()
    }

    private fun showErrorState(errorState: ErrorState) {
        showBottomSheet(ErrorSlidingBottomDialog.newInstance(activity))
    }

    private fun renderUiForNoBanks(fiatCurrency: String) {
        bank_icon.setImageResource(R.drawable.ic_add_payment_method)
        undefined_bank_text.visible()
        bank_title.gone()
        banks_arrow.gone()
        bank_details_root.setOnClickListener {
            addBankWithCurrency(fiatCurrency)
        }
    }

    override fun onNewBankSelected(bank: LinkedBank) {
        model.process(WithdrawIntent.SelectedBankUpdated(bank))
    }

    override fun onSheetClosed() {}

    override fun addBankWithCurrency(fiatCurrency: String) {
        showBottomSheet(LinkBankAccountDetailsBottomSheet.newInstance(
            fiatCurrency
        ))
    }

    private fun renderUiForBank(bank: LinkedBank, banks: List<LinkedBank>, currency: String) {
        bank_icon.setImageResource(R.drawable.ic_bank_transfer)
        bank_title.text = bank.title.plus(" ${bank.account}")
        bank_details_root.setOnClickListener {
            openBankChooserBottomSheet(banks, currency)
        }
        undefined_bank_text.gone()
        bank_title.visible()
    }

    private fun openBankChooserBottomSheet(linkedBanks: List<LinkedBank>, currency: String) {
        showBottomSheet(BankChooserBottomSheet.newInstance(
            linkedBanks,
            currency
        ))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_withdraw_enter_amount)

    companion object {
        private const val CURRENCY_KEY = "CURRENCY_KEY"

        fun newInstance(fiatCurrency: String) =
            WithdrawEnterAmountFragment().apply {
                arguments =
                    Bundle().apply { putString(CURRENCY_KEY, fiatCurrency) }
            }
    }
}

interface BankChooserHost : SlidingModalBottomDialog.Host {
    fun onNewBankSelected(bank: LinkedBank)
    fun addBankWithCurrency(currency: String)
}