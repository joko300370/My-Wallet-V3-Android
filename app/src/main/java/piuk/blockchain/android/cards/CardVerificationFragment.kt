package piuk.blockchain.android.cards

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentCardVerificationBinding
import piuk.blockchain.android.ui.base.mvi.MviFragment

class CardVerificationFragment : MviFragment<CardModel, CardIntent, CardState, FragmentCardVerificationBinding>(),
    AddCardFlowFragment {

    override val model: CardModel by scopedInject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentCardVerificationBinding =
        FragmentCardVerificationBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.okBtn.setOnClickListener {
            navigator.exitWithError()
        }
    }

    override fun render(newState: CardState) {
        if (newState.addCard) {
            cardDetailsPersistence.getCardData()?.let {
                model.process(CardIntent.CardAddRequested)
                model.process(CardIntent.AddNewCard(it))
            }
        }

        newState.cardRequestStatus?.let {
            when (it) {
                is CardRequestStatus.Loading -> renderLoadingState()
                is CardRequestStatus.Error -> renderErrorState(it.type)
                is CardRequestStatus.Success -> navigator.exitWithSuccess(it.card)
            }
        }

        if (newState.authoriseEverypayCard != null) {
            openWebView(
                newState.authoriseEverypayCard.paymentLink,
                newState.authoriseEverypayCard.exitLink
            )
            model.process(CardIntent.ResetEveryPayAuth)
            binding.progress.visibility = View.GONE
        }
    }

    private fun openWebView(paymentLink: String, exitLink: String) {
        CardAuthoriseWebViewActivity.start(fragment = this, link = paymentLink, exitLink = exitLink)
    }

    private fun renderLoadingState() {
        with(binding) {
            progress.visibility = View.VISIBLE
            icon.visibility = View.GONE
            okBtn.visibility = View.GONE
            title.text = getString(R.string.linking_card_title)
            subtitle.text = getString(R.string.linking_card_subtitle)
        }
    }

    private fun renderErrorState(error: CardError) {
        with(binding) {
            progress.visibility = View.GONE
            icon.visibility = View.VISIBLE
            okBtn.visibility = View.VISIBLE
            title.text = getString(R.string.linking_card_error_title)
            subtitle.text = when (error) {
                CardError.CREATION_FAILED -> getString(R.string.could_not_save_card)
                CardError.ACTIVATION_FAIL -> getString(R.string.could_not_activate_card)
                CardError.PENDING_AFTER_POLL -> getString(R.string.card_still_pending)
                CardError.LINK_FAILED -> getString(R.string.card_link_failed)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EVERYPAY_AUTH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                model.process(CardIntent.CheckCardStatus)
                analytics.logEvent(SimpleBuyAnalytics.CARD_3DS_COMPLETED)
            }
        }
    }

    override val navigator: AddCardNavigator
        get() = (activity as? AddCardNavigator)
            ?: throw IllegalStateException("Parent must implement AddCardNavigator")

    override val cardDetailsPersistence: CardDetailsPersistence
        get() = (activity as? CardDetailsPersistence)
            ?: throw IllegalStateException("Parent must implement CardDetailsPersistence")

    override fun onBackPressed(): Boolean = true

    companion object {
        const val EVERYPAY_AUTH_REQUEST_CODE = 324
    }
}