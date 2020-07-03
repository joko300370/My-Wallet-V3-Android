package piuk.blockchain.android.ui.dashboard.sheets

import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.bankFieldName
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.ui.urllinks.MODULAR_TERMS_AND_CONDITIONS
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.bank_details_error_layout.view.*
import kotlinx.android.synthetic.main.link_bank_account_layout.view.*
import kotlinx.android.synthetic.main.link_bank_account_layout.view.bank_deposit_instruction
import kotlinx.android.synthetic.main.link_bank_account_layout.view.title
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.BankDetailField
import piuk.blockchain.android.simplebuy.CopyFieldListener
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible
import java.lang.IllegalStateException

class LinkBankAccountDetailsBottomSheet : SlidingModalBottomDialog() {

    private val compositeDisposable = CompositeDisposable()
    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val stringUtils: StringUtils by inject()

    private val fiatCurrency: String by unsafeLazy {
        arguments?.getString(FIAT_CURRENCY) ?: throw IllegalStateException("Fiat currency is missing")
    }

    private val isForLink: Boolean by unsafeLazy {
        arguments?.getBoolean(IS_FOR_LINK) ?: false
    }

    override val layoutResource = R.layout.link_bank_account_layout

    override fun initControls(view: View) {
        compositeDisposable += custodialWalletManager.getBankAccountDetails(fiatCurrency)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { bankAccount ->
                    view.bank_details.initWithBankDetailsAndAmount(
                        bankAccount.details.map {
                            BankDetailField(it.title, it.value, it.isCopyable)
                        },
                        copyListener
                    )
                    configureUi(fiatCurrency, view)
                },
                onError = {
                    renderErrorUi(view)
                }
            )
    }

    private fun renderErrorUi(view: View) {
        with(view) {
            bank_details_error.visible()
            bank_details_error.error_button.setOnClickListener {
                dismiss()
            }
            title.gone()
            subtitle.gone()
            instructions.gone()
            bank_details.gone()
            bank_transfer_only.gone()
            processing_time.gone()
            bank_deposit_instruction.gone()
        }
    }

    private fun configureUi(fiatCurrency: String, view: View) {
        with(view) {
            if (fiatCurrency == "GBP") {
                val linksMap = mapOf<String, Uri>(
                    "modular_terms_and_conditions" to Uri.parse(MODULAR_TERMS_AND_CONDITIONS)
                )
                bank_deposit_instruction.text =
                    stringUtils.getStringWithMappedLinks(
                        R.string.recipient_name_must_match_gbp,
                        linksMap,
                        requireActivity()
                    )
                bank_deposit_instruction.movementMethod = LinkMovementMethod.getInstance()
            } else {
                bank_deposit_instruction.text = getString(R.string.recipient_name_must_match_eur)
            }

            processing_time.updateSubtitle(
                if (fiatCurrency == "GBP") getString(R.string.processing_time_subtitle_gbp)
                else getString(R.string.processing_time_subtitle_eur)
            )
            title.text = if (isForLink) getString(R.string.add_bank) else
                getString(R.string.deposit_currency, fiatCurrency)
            subtitle.text = if (isForLink) getString(R.string.currency_bank_transfer, fiatCurrency) else
                getString(R.string.bank_transfer)
            instructions.text = getString(R.string.link_transfer_instructions, fiatCurrency)

            bank_transfer_only.visible()
            processing_time.visible()
        }
    }

    override fun dismiss() {
        super.dismiss()
        compositeDisposable.dispose()
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
        private const val FIAT_CURRENCY = "FIAT_CURRENCY_KEY"
        private const val IS_FOR_LINK = "IS_FOR_LINK"

        fun newInstance(fiatCurrency: String, isForLink: Boolean = true):
                LinkBankAccountDetailsBottomSheet =
            LinkBankAccountDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(FIAT_CURRENCY, fiatCurrency)
                    putBoolean(IS_FOR_LINK, isForLink)
                }
            }
    }
}