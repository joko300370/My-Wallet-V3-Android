package piuk.blockchain.android.ui.linkbank

import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.LinkBankTransfer
import com.blockchain.nabu.models.data.LinkedBank
import piuk.blockchain.android.simplebuy.ErrorState
import piuk.blockchain.android.simplebuy.SelectedPaymentMethod
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class BankAuthIntent : MviIntent<BankAuthState> {

    object ProviderAccountIdUpdateError : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState = oldState.copy(
            errorState = ErrorState.BankLinkingUpdateFailed,
            bankLinkingProcessState = BankLinkingProcessState.NONE
        )
    }

    class LinkedBankStateSuccess(private val linkedBank: LinkedBank) : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                bankLinkingProcessState = BankLinkingProcessState.LINKING_SUCCESS,
                linkedBank = linkedBank,
                selectedPaymentMethod = SelectedPaymentMethod(
                    id = linkedBank.id,
                    paymentMethodType = PaymentMethodType.BANK_TRANSFER,
                    label = linkedBank.accountName,
                    isEligible = true
                )
            )

        override fun isValidFor(oldState: BankAuthState): Boolean = true
    }

    class BankAuthErrorState(val state: ErrorState) : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                errorState = state,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
    }

    class GetLinkedBankState(val linkingBankId: String, private val isFromDeepLink: Boolean = false) :
        BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                bankLinkingProcessState = if (isFromDeepLink) {
                    BankLinkingProcessState.ACTIVATING
                } else {
                    BankLinkingProcessState.IN_EXTERNAL_FLOW
                }
            )
    }

    class UpdateAccountProvider(
        val accountProviderId: String,
        val accountId: String,
        val linkingBankId: String,
        val linkBankTransfer: LinkBankTransfer,
        val authSource: BankAuthSource
    ) : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState = oldState.copy(
            linkBankTransfer = linkBankTransfer,
            bankLinkingProcessState = BankLinkingProcessState.LINKING
        )
    }

    object ClearBankLinkingUrl : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                linkBankUrl = null,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
    }

    object StartBankLinking : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                bankLinkingProcessState = BankLinkingProcessState.IN_EXTERNAL_FLOW
            )
    }

    object StartBankApproval : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                bankLinkingProcessState = BankLinkingProcessState.APPROVAL_WAIT
            )
    }

    object ResetBankLinking : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                linkBankUrl = null,
                bankLinkingProcessState = BankLinkingProcessState.NONE
            )
    }

    data class StartPollingForLinkStatus(val bankId: String) : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(bankLinkingProcessState = BankLinkingProcessState.LINKING)
    }

    data class UpdateLinkingUrl(val authUrl: String) : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                linkBankUrl = authUrl
            )
    }

    data class UpdateForApproval(val authorisationUrl: String) : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                linkBankUrl = authorisationUrl,
                bankLinkingProcessState = BankLinkingProcessState.APPROVAL
            )
    }

    object ClearApprovalState : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                linkedBank = null,
                authorisePaymentUrl = null
            )
    }

    object CancelOrder : BankAuthIntent() {
        override fun isValidFor(oldState: BankAuthState) = true
        override fun reduce(oldState: BankAuthState): BankAuthState = oldState
    }

    object CancelOrderAndResetAuthorisation : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(
                authorisePaymentUrl = null,
                linkedBank = null
            )
    }

    object OrderCanceled : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            BankAuthState(bankLinkingProcessState = BankLinkingProcessState.CANCELED)
    }

    class ErrorIntent(private val error: ErrorState = ErrorState.GenericError) : BankAuthIntent() {
        override fun reduce(oldState: BankAuthState): BankAuthState =
            oldState.copy(errorState = error)

        override fun isValidFor(oldState: BankAuthState): Boolean = true
    }
}
