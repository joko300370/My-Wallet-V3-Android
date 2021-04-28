package piuk.blockchain.android.deeplink

import android.net.Uri
import piuk.blockchain.android.kyc.ignoreFragment

class OpenBankingDeepLinkParser {
    fun mapUri(uri: Uri): LinkState.OpenBankingLink? {
        val fragment = uri.encodedFragment?.let { Uri.parse(it) } ?: return null
        val consentToken = uri.ignoreFragment().getQueryParameter("one-time-token") ?: ""

        return LinkState.OpenBankingLink(
            when (fragment.path) {
                "/open/ob-bank-link" -> {
                    OpenBankingLinkType.LINK_BANK
                }
                "/open/ob-bank-approval" -> {
                    OpenBankingLinkType.PAYMENT_APPROVAL
                }
                else -> {
                    OpenBankingLinkType.UNKNOWN
                }
            }, consentToken
        )
    }
}

enum class OpenBankingLinkType {
    LINK_BANK,
    PAYMENT_APPROVAL,
    UNKNOWN
}