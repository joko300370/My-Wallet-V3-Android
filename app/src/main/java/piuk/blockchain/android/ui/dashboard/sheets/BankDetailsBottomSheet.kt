package piuk.blockchain.android.ui.dashboard.sheets

import android.content.Context
import android.view.View
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.PendingTransactionShown
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import com.blockchain.notifications.analytics.bankFieldName
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_simple_buy_bank_details.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.BankDetailField
import piuk.blockchain.android.simplebuy.CopyFieldListener
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.setOnClickListenerDebounced
import timber.log.Timber

class BankDetailsBottomSheet : SlidingModalBottomDialog() {

    interface Host : SlidingModalBottomDialog.Host {
        fun startWarnCancelSimpleBuyOrder()
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException("Host fragment is not a BankDetailsBottomSheet.Host")
    }

    private val stateFactory: SimpleBuySyncFactory by scopedInject()
    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val environmentConfig: EnvironmentConfig by inject()
    private val disposables = CompositeDisposable()

    override val layoutResource = R.layout.dialog_simple_buy_bank_details

    override fun initControls(view: View) {
        stateFactory.currentState()?.let {
            renderState(view, it)
        } ?: closeBecauseError("State not found")
    }

    override fun onSheetHidden() {
        super.onSheetHidden()
        dialog?.let {
            onCancel(it)
        }
    }

    private val parentContext: Context
        get() = parentFragment?.context ?: activity ?: throw IllegalStateException("No parent")

    private fun closeBecauseError(logMsg: String) {
        Timber.d("Cannot open bank details sheet: $logMsg")
        onCtaOKClick()
        if (environmentConfig.isRunningOnDebugMode()) {
            ToastCustom.makeText(
                parentContext,
                "Cannot open bank details sheet: $logMsg",
                ToastCustom.LENGTH_LONG,
                ToastCustom.TYPE_ERROR
            )
        }
    }

    private fun onCtaOKClick() {
        disposables.dispose()
        dismiss()
    }

    private fun onCtaCancelClick(orderId: String) {
        with(dialogView) {
            cta_button_ok.isEnabled = false
            cta_button_cancel.isEnabled = false
        }
        isCancelable = false

        onCtaOKClick()
        analytics.logEvent(SimpleBuyAnalytics.PENDING_TRANSFER_MODAL_CANCEL_CLICKED)
        host.startWarnCancelSimpleBuyOrder()
    }

    private fun renderState(view: View, state: SimpleBuyState) {
        analytics.logEvent(PendingTransactionShown(state.fiatCurrency))
        with(view) {
            val amount = state.order.amount
            if (amount != null) {

                transfer_msg.text = state.selectedCryptoCurrency?.let {
                    getString(
                        R.string.simple_buy_bank_account_sheet_instructions,
                        amount.currencyCode,
                        it.displayTicker
                    ) ?: ""
                }

                if (state.bankAccount != null) {
                    bank_details.initWithBankDetailsAndAmount(
                        state.bankAccount.details.map {
                            BankDetailField(it.title, it.value, it.isCopyable)
                        }.toMutableList().apply {
                            add(BankDetailField(
                                getString(R.string.simple_buy_amount_to_send),
                                amount.toStringWithSymbol(),
                                false
                            )
                            )
                        },
                        copyListener
                    )
                } else {
                    disposables += custodialWalletManager.getBankAccountDetails(state.fiatCurrency)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(
                            onSuccess = {
                                bank_details.initWithBankDetailsAndAmount(
                                    it.details.map { bankDetails ->
                                        BankDetailField(bankDetails.title, bankDetails.value, bankDetails.isCopyable)
                                    }.toMutableList().apply {
                                        add(
                                            BankDetailField(getString(R.string.simple_buy_amount_to_send),
                                                amount.toStringWithSymbol(),
                                                false
                                            )
                                        )
                                    },
                                    copyListener
                                )
                            },
                            onError = {
                                closeBecauseError("Cannot get bank details: ${it.message}")
                            }
                        )
                }
            } else {
                closeBecauseError("Invalid amount in SB state")
            }

            title.text = state.selectedCryptoCurrency?.let {
                getString(R.string.simple_buy_pending_buy_sheet_title, it.displayTicker)
            } ?: ""

            cta_button_ok.setOnClickListenerDebounced { onCtaOKClick() }
            cta_button_cancel.setOnClickListenerDebounced {
                state.id?.let {
                    onCtaCancelClick(it)
                }
            }
        }
    }

    private val copyListener = object : CopyFieldListener {
        override fun onFieldCopied(field: String) {
            analytics.logEvent(bankFieldName(field))
            ToastCustom.makeText(
                requireContext(),
                resources.getString(R.string.simple_buy_copied_to_clipboard, field),
                ToastCustom.LENGTH_SHORT,
                ToastCustom.TYPE_OK
            )
        }
    }

    companion object {
        fun newInstance(): BankDetailsBottomSheet =
            BankDetailsBottomSheet()
    }
}
