package piuk.blockchain.android.deeplink

import android.net.Uri

class OpenBankingDeepLinkParser {
    fun mapUri(uri: Uri): OpenBankingLinkType? {
        val fragment = uri.encodedFragment?.let { Uri.parse(it) } ?: return null

        return when (fragment.path) {
            "/open/ob-bank-link" -> {
                OpenBankingLinkType.LINK_BANK
            }
            "/open/ob-bank-approval" -> {
                OpenBankingLinkType.PAYMENT_APPROVAL
            }
            else -> {
                OpenBankingLinkType.UNKNOWN
            }
        }
    }
}

enum class OpenBankingLinkType {
    LINK_BANK,
    PAYMENT_APPROVAL,
    UNKNOWN
}