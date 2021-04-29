package piuk.blockchain.android.deeplink

import android.net.Uri
import piuk.blockchain.android.kyc.ignoreFragment

class OpenBankingDeepLinkParser {
    fun mapUri(uri: Uri): LinkState.OpenBankingLink? {
        val fragment = uri.encodedFragment?.let { Uri.parse(it) } ?: return null
        val consentToken = uri.ignoreFragment().getQueryParameter(OTT).orEmpty()

        return LinkState.OpenBankingLink(
            when (fragment.path) {
                BANK_LINK -> {
                    OpenBankingLinkType.LINK_BANK
                }
                BANK_APPROVAL -> {
                    OpenBankingLinkType.PAYMENT_APPROVAL
                }
                else -> {
                    OpenBankingLinkType.UNKNOWN
                }
            }, consentToken
        )
    }

    companion object {
        private const val OTT = "one-time-token"
        private const val BANK_LINK = "/open/ob-bank-link"
        private const val BANK_APPROVAL = "/open/ob-bank-approval"
    }
}

enum class OpenBankingLinkType {
    LINK_BANK,
    PAYMENT_APPROVAL,
    UNKNOWN
}