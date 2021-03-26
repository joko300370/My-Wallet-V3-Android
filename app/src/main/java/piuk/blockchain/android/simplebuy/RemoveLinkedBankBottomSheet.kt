package piuk.blockchain.android.simplebuy

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.Bank
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.RemoveBankBottomSheetBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class RemoveLinkedBankBottomSheet : SlidingModalBottomDialog<RemoveBankBottomSheetBinding>() {

    private val compositeDisposable = CompositeDisposable()
    private val custodialWalletManager: CustodialWalletManager by scopedInject()

    private val bank: Bank by unsafeLazy {
        arguments?.getSerializable(BANK_KEY) as Bank
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): RemoveBankBottomSheetBinding =
        RemoveBankBottomSheetBinding.inflate(inflater, container, false)

    override fun initControls(binding: RemoveBankBottomSheetBinding) {
        with(binding) {
            title.text = resources.getString(R.string.common_spaced_strings, bank.name, bank.currency)
            endDigits.text = resources.getString(R.string.dotted_suffixed_string, bank.account)
            accountInfo.text = getString(R.string.payment_method_type_account_info, bank.toHumanReadableAccount(), "")
            rmvBankBtn.setOnClickListener {
                compositeDisposable += custodialWalletManager.removeBank(bank)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe {
                        updateUi(true)
                    }
                    .doFinally {
                        updateUi(false)
                    }
                    .subscribeBy(onComplete = {
                        analytics.logEvent(SimpleBuyAnalytics.REMOVE_BANK)
                        (parentFragment as? RemovePaymentMethodBottomSheetHost)?.onLinkedBankRemoved(bank.id)
                        dismiss()
                    }, onError = {})
            }
        }
    }

    private fun updateUi(isLoading: Boolean) {
        binding.progress.visibleIf { isLoading }
        binding.icon.visibleIf { !isLoading }
        binding.rmvBankBtn.isEnabled = !isLoading
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        compositeDisposable.dispose()
    }

    companion object {
        private const val BANK_KEY = "BANK_KEY"

        fun newInstance(bank: Bank) =
            RemoveLinkedBankBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(BANK_KEY, bank)
                }
            }
    }
}