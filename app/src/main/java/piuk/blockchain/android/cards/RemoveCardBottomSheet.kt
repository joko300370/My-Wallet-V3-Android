package piuk.blockchain.android.cards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.PaymentMethod
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.databinding.RemoveCardBottomSheetBinding
import piuk.blockchain.android.simplebuy.RemovePaymentMethodBottomSheetHost
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class RemoveCardBottomSheet : SlidingModalBottomDialog<RemoveCardBottomSheetBinding>() {

    private val custodialWalletManager: CustodialWalletManager by scopedInject()

    private val card: PaymentMethod.Card by unsafeLazy {
        arguments?.getSerializable(CARD_KEY) as? PaymentMethod.Card
            ?: throw IllegalStateException("No card provided")
    }

    private val compositeDisposable = CompositeDisposable()

    override fun initControls(binding: RemoveCardBottomSheetBinding) {
        with(binding) {
            title.text = card.uiLabel()
            endDigits.text = card.dottedEndDigits()
            icon.setImageResource(card.cardType.icon())
            rmvCardBtn.setOnClickListener {
                compositeDisposable += custodialWalletManager.deleteCard(card.id)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe {
                        updateUi(true)
                    }
                    .doFinally {
                        updateUi(false)
                    }
                    .subscribeBy(onComplete = {
                        (parentFragment as? RemovePaymentMethodBottomSheetHost)?.onCardRemoved(card.cardId)
                        dismiss()
                        analytics.logEvent(SimpleBuyAnalytics.REMOVE_CARD)
                    }, onError = {})
            }
        }
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    private fun updateUi(isLoading: Boolean) {
        with(binding) {
            progress.visibleIf { isLoading }
            icon.visibleIf { !isLoading }
            rmvCardBtn.isEnabled = !isLoading
        }
    }

    companion object {
        private const val CARD_KEY = "CARD_KEY"

        fun newInstance(card: PaymentMethod.Card) =
            RemoveCardBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(CARD_KEY, card)
                }
            }
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): RemoveCardBottomSheetBinding =
        RemoveCardBottomSheetBinding.inflate(inflater, container, false)
}