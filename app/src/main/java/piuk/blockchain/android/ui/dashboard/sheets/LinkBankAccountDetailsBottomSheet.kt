package piuk.blockchain.android.ui.dashboard.sheets

import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.simplebuy.linkBankEventWithCurrency
import piuk.blockchain.android.simplebuy.linkBankFieldCopied
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.ui.urllinks.MODULAR_TERMS_AND_CONDITIONS
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.databinding.DialogSheetLinkBankAccountBinding
import piuk.blockchain.android.simplebuy.BankDetailField
import piuk.blockchain.android.simplebuy.CopyFieldListener
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible

class LinkBankAccountDetailsBottomSheet : SlidingModalBottomDialog<DialogSheetLinkBankAccountBinding>() {

    private val compositeDisposable = CompositeDisposable()
    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val stringUtils: StringUtils by inject()
    private val currencyPrefs: CurrencyPrefs by scopedInject()

    private val fiatCurrency: String by unsafeLazy {
        arguments?.getString(FIAT_CURRENCY) ?: currencyPrefs.selectedFiatCurrency
    }

    private val isForLink: Boolean by unsafeLazy {
        arguments?.getBoolean(IS_FOR_LINK) ?: false
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetLinkBankAccountBinding =
        DialogSheetLinkBankAccountBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogSheetLinkBankAccountBinding) {
        compositeDisposable += custodialWalletManager.getBankAccountDetails(fiatCurrency)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { bankAccount ->
                    binding.bankDetails.initWithBankDetailsAndAmount(
                        bankAccount.details.map {
                            BankDetailField(it.title, it.value, it.isCopyable)
                        },
                        copyListener
                    )
                    configureUi(fiatCurrency)

                    analytics.logEvent(
                        linkBankEventWithCurrency(
                            SimpleBuyAnalytics.LINK_BANK_SCREEN_SHOWN,
                            fiatCurrency
                        )
                    )
                },
                onError = {
                    renderErrorUi()
                    analytics.logEvent(
                        linkBankEventWithCurrency(
                            SimpleBuyAnalytics.LINK_BANK_LOADING_ERROR,
                            fiatCurrency
                        )
                    )
                }
            )
    }

    private fun renderErrorUi() {
        with(binding) {
            bankDetailsError.errorContainer.visible()
            bankDetailsError.errorButton.setOnClickListener {
                dismiss()
            }
            title.gone()
            subtitle.gone()
            bankDetails.gone()
            bankTransferOnly.gone()
            processingTime.gone()
            bankDepositInstruction.gone()
        }
    }

    private fun configureUi(fiatCurrency: String) {
        with(binding) {
            if (fiatCurrency == "GBP") {
                val linksMap = mapOf<String, Uri>(
                    "modular_terms_and_conditions" to Uri.parse(MODULAR_TERMS_AND_CONDITIONS)
                )
                bankDepositInstruction.text =
                    stringUtils.getStringWithMappedAnnotations(
                        R.string.by_depositing_funds_terms_and_conds,
                        linksMap,
                        requireActivity()
                    )
                bankDepositInstruction.movementMethod = LinkMovementMethod.getInstance()
            } else {
                bankDepositInstruction.gone()
            }

            processingTime.updateSubtitle(
                if (fiatCurrency == "GBP") getString(R.string.processing_time_subtitle_gbp)
                else getString(R.string.processing_time_subtitle_eur)
            )
            title.text = if (isForLink) getString(R.string.add_bank_with_currency, fiatCurrency) else
                getString(R.string.deposit_currency, fiatCurrency)
            subtitle.text = getString(R.string.bank_transfer)

            bankTransferOnly.visible()
            processingTime.visible()
        }
    }

    override fun dismiss() {
        super.dismiss()
        compositeDisposable.dispose()
    }

    private val copyListener = object : CopyFieldListener {
        override fun onFieldCopied(field: String) {
            analytics.logEvent(linkBankFieldCopied(field, fiatCurrency))
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

        fun newInstance(fiatAccount: FiatAccount) =
            LinkBankAccountDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(FIAT_CURRENCY, fiatAccount.fiatCurrency)
                    putBoolean(IS_FOR_LINK, !fiatAccount.isFunded)
                }
            }

        fun newInstance() = LinkBankAccountDetailsBottomSheet()

        fun newInstance(fiatCurrency: String) =
            LinkBankAccountDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(FIAT_CURRENCY, fiatCurrency)
                }
            }
    }
}