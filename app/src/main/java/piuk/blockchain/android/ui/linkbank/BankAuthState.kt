package piuk.blockchain.android.ui.linkbank

import com.blockchain.nabu.models.data.LinkBankTransfer
import com.blockchain.nabu.models.data.LinkedBank
import com.google.gson.Gson
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.simplebuy.ErrorState
import piuk.blockchain.android.simplebuy.SelectedPaymentMethod
import piuk.blockchain.android.ui.base.mvi.MviState
import java.io.Serializable

data class BankAuthState(
    val id: String? = null,
    val linkedBank: LinkedBank? = null,
    val linkBankTransfer: LinkBankTransfer? = null,
    val linkBankUrl: String? = null,
    val bankLinkingProcessState: BankLinkingProcessState = BankLinkingProcessState.NONE,
    val errorState: ErrorState? = null,
    val selectedPaymentMethod: SelectedPaymentMethod? = null,
    val authorisePaymentUrl: String? = null
) : MviState

enum class BankLinkingProcessState {
    LINKING,
    IN_EXTERNAL_FLOW,
    APPROVAL,
    APPROVAL_WAIT,
    ACTIVATING,
    LINKING_SUCCESS,
    CANCELED,
    NONE
}

data class BankLinkingInfo(
    val linkingId: String,
    val bankAuthSource: BankAuthSource
) : Serializable

enum class BankAuthSource {
    SIMPLE_BUY,
    SETTINGS,
    DEPOSIT,
    WITHDRAW
}

data class BankPaymentApproval(
    val paymentId: String,
    val authorisationUrl: String,
    val linkedBank: LinkedBank,
    val orderValue: FiatValue
) : Serializable

data class BankAuthDeepLinkState(
    val bankAuthFlow: BankAuthFlowState = BankAuthFlowState.NONE,
    val bankPaymentData: BankPaymentApproval? = null,
    val bankLinkingInfo: BankLinkingInfo? = null
)

fun BankAuthDeepLinkState.toPreferencesValue(): String =
    Gson().toJson(this, BankAuthDeepLinkState::class.java)

internal fun String.fromPreferencesValue(): BankAuthDeepLinkState =
    Gson().fromJson(this, BankAuthDeepLinkState::class.java)

enum class BankAuthFlowState {
    BANK_LINK_PENDING,
    BANK_LINK_COMPLETE,
    BANK_APPROVAL_PENDING,
    BANK_APPROVAL_COMPLETE,
    NONE
}

enum class FiatTransactionState {
    SUCCESS,
    ERROR,
    PENDING
}