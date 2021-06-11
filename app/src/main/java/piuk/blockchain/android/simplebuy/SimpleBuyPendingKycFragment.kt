package piuk.blockchain.android.simplebuy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.preferences.CurrencyPrefs
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.cards.CardDetailsActivity
import piuk.blockchain.android.databinding.FragmentSimpleBuyKycPendingBinding
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf

class SimpleBuyPendingKycFragment :
    MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState, FragmentSimpleBuyKycPendingBinding>(),
    SimpleBuyScreen {

    override val model: SimpleBuyModel by scopedInject()
    private val currencyPrefs: CurrencyPrefs by inject()

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSimpleBuyKycPendingBinding =
        FragmentSimpleBuyKycPendingBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.process(SimpleBuyIntent.FetchKycState)
        model.process(SimpleBuyIntent.FlowCurrentScreen(FlowScreen.KYC_VERIFICATION))
        binding.continueToWallet.setOnClickListener {
            navigator().exitSimpleBuyFlow()
        }
    }

    override fun render(newState: SimpleBuyState) {
        with(binding) {
            kycProgress.visibleIf { newState.kycVerificationState == KycState.PENDING }
            kycIcon.visibleIf {
                newState.kycVerificationState == KycState.FAILED ||
                    newState.kycVerificationState == KycState.IN_REVIEW ||
                    newState.kycVerificationState == KycState.UNDECIDED ||
                    newState.kycVerificationState == KycState.VERIFIED_BUT_NOT_ELIGIBLE
            }

            verifText.text = when (newState.kycVerificationState) {
                KycState.PENDING -> resources.getString(R.string.kyc_verifying_info)
                KycState.IN_REVIEW, KycState.FAILED -> resources.getString(R.string.kyc_manual_review_required)
                KycState.UNDECIDED -> resources.getString(R.string.kyc_pending_review)
                KycState.VERIFIED_BUT_NOT_ELIGIBLE -> resources.getString(R.string.kyc_veriff_but_not_eligible_review)
                else -> ""
            }

            verifTime.text = when (newState.kycVerificationState) {
                KycState.PENDING -> resources.getString(R.string.kyc_verifying_time_info)
                KycState.FAILED,
                KycState.IN_REVIEW,
                KycState.UNDECIDED -> resources.getString(R.string.kyc_verifying_manual_review_required_info)
                KycState.VERIFIED_BUT_NOT_ELIGIBLE -> resources.getString(
                    R.string.kyc_veriff_but_not_eligible_review_info
                )
                else -> ""
            }

            continueToWallet.visibleIf {
                newState.kycVerificationState == KycState.FAILED ||
                    newState.kycVerificationState == KycState.UNDECIDED ||
                    newState.kycVerificationState == KycState.VERIFIED_BUT_NOT_ELIGIBLE
            }

            kycIcon.setImageResource(
                when (newState.kycVerificationState) {
                    KycState.IN_REVIEW,
                    KycState.FAILED -> R.drawable.ic_kyc_failed_warning
                    KycState.VERIFIED_BUT_NOT_ELIGIBLE -> R.drawable.ic_kyc_approved
                    else -> R.drawable.ic_kyc_pending
                }
            )

            newState.kycVerificationState?.takeIf { it != latestKycState }?.let {
                sendStateAnalytics(it)
            }

            newState.linkBankTransfer?.let {
                model.process(SimpleBuyIntent.ResetLinkBankTransfer)
                startActivityForResult(
                    BankAuthActivity.newInstance(
                        it, BankAuthSource.SIMPLE_BUY, requireContext()
                    ),
                    BankAuthActivity.LINK_BANK_REQUEST_CODE
                )
            }
            if (
                newState.kycVerificationState == KycState.VERIFIED_AND_ELIGIBLE &&
                latestKycState != newState.kycVerificationState
            ) {
                when (newState.selectedPaymentMethod?.id) {
                    PaymentMethod.UNDEFINED_CARD_PAYMENT_ID -> {
                        addCard()
                    }
                    PaymentMethod.UNDEFINED_BANK_TRANSFER_PAYMENT_ID -> {
                        tryToLinkABank()
                    }
                    else -> {
                        navigator().pop()
                    }
                }
                latestKycState = newState.kycVerificationState
            }

            // Case when user is not eligible for a payment method after kyc is done
            // (Can happen only for bank at this state)
            if (newState.errorState == ErrorState.LinkedBankNotSupported) {
                kycIcon.setImageResource(R.drawable.ic_bank_details_big)
                kycIcon.visible()
                verifText.text = getString(R.string.common_oops)
                verifTime.text = getString(R.string.please_try_linking_your_bank_again)
                continueToWallet.visible()
                // Case when user is trying to link a payment method, after successful kyc
            } else if (newState.isLoading) {
                kycIcon.setImageResource(R.drawable.ic_bank_details_big)
                kycIcon.visible()
            }

            bankLinkedFailed.visibleIf { newState.errorState == ErrorState.LinkedBankNotSupported }
            progress.visibleIf { newState.isLoading }
        }
    }

    private fun tryToLinkABank() {
        model.process(SimpleBuyIntent.TryToLinkABankTransfer)
    }

    private fun addCard() {
        val intent = Intent(activity, CardDetailsActivity::class.java)
        startActivityForResult(intent, CardDetailsActivity.ADD_CARD_REQUEST_CODE)
    }

    private fun sendStateAnalytics(state: KycState) {
        when (state) {
            KycState.VERIFIED_BUT_NOT_ELIGIBLE -> analytics.logEvent(SimpleBuyAnalytics.KYC_NOT_ELIGIBLE)
            KycState.PENDING -> analytics.logEvent(SimpleBuyAnalytics.KYC_VERIFYING)
            KycState.IN_REVIEW -> analytics.logEvent(SimpleBuyAnalytics.KYC_MANUAL)
            KycState.UNDECIDED -> analytics.logEvent(SimpleBuyAnalytics.KYC_PENDING)
            else -> {
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CardDetailsActivity.ADD_CARD_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val card = (data?.extras?.getSerializable(CardDetailsActivity.CARD_KEY) as?
                    PaymentMethod.Card) ?: return
                val cardId = card.cardId
                val cardLabel = card.uiLabel()
                val cardPartner = card.partner

                model.process(
                    SimpleBuyIntent.UpdateSelectedPaymentCard(
                        id = cardId,
                        label = cardLabel,
                        partner = cardPartner,
                        isEligible = true
                    )
                )
                navigator().goToCheckOutScreen()
            } else {
                model.process(SimpleBuyIntent.ClearState)
                navigator().exitSimpleBuyFlow()
            }
        } else if (requestCode == BankAuthActivity.LINK_BANK_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            navigator().pop()
        }
    }

    private var latestKycState: KycState? = null

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator) ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true
}