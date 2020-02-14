package piuk.blockchain.android.ui.dashboard.sheets

import android.view.View
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_simple_buy_bank_details.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.CopyFieldListener
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.setOnClickListenerDebounced
import timber.log.Timber

class BankDetailsBottomSheet : SlidingModalBottomDialog() {

    private val prefs: SimpleBuyPrefs by inject()
    private val stateFactory: SimpleBuySyncFactory by inject()
    private val custodialWalletManager: CustodialWalletManager by inject()
    private val environmentConfig: EnvironmentConfig by inject()
    private val disposables = CompositeDisposable()

    override val layoutResource = R.layout.dialog_simple_buy_bank_details

    override fun initControls(view: View) =
        stateFactory.currentState()?.let {
            renderState(view, it)
        } ?: closeBecauseError("State not found")

    override fun onSheetHidden() {
        super.onSheetHidden()
        dialog?.let {
            onCancel(it)
        }
    }

    private fun closeBecauseError(logMsg: String) {
        Timber.d("Cannot open bank details sheet: $logMsg")
        onCtaOKClick()
        if (environmentConfig.shouldShowDebugMenu()) {
            ToastCustom.makeText(
                requireContext(),
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

        disposables += custodialWalletManager.deleteBuyOrder(orderId)
            .subscribeBy(
                onComplete = {
                    prefs.clearState()
                    onCtaOKClick()
                },
                onError = {
                    closeBecauseError("Cancel order failed: $it")
                }
            )
    }

    private fun renderState(view: View, state: SimpleBuyState) {
        with(view) {
            val amount = state.order.amount
            if (amount != null) {

                transfer_msg.text = getString(
                    R.string.simple_buy_bank_account_sheet_instructions,
                    amount.currencyCode,
                    state.selectedCryptoCurrency
                )

                if (state.bankAccount != null) {
                    bank_details.initWithBankDetailsAndAmount(
                        state.bankAccount.details,
                        amount,
                        copyListeener
                    )
                } else {
                    disposables += custodialWalletManager.getBankAccountDetails(state.currency)
                        .subscribeBy(
                            onSuccess = {
                                bank_details.initWithBankDetailsAndAmount(
                                    it.details,
                                    amount,
                                    copyListeener
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

            title.text = getString(R.string.simple_buy_pending_buy_sheet_title, state.selectedCryptoCurrency!!)
            cta_button_ok.setOnClickListenerDebounced { onCtaOKClick() }
            cta_button_cancel.setOnClickListenerDebounced { onCtaCancelClick(state.id!!) }
        }
    }

    private val copyListeener = object : CopyFieldListener {
        override fun onFieldCopied(field: String) {
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
