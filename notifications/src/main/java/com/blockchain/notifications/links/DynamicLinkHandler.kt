package com.blockchain.notifications.links

import android.content.Intent
import android.net.Uri
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import io.reactivex.Maybe

internal class DynamicLinkHandler internal constructor(
    private val dynamicLinks: FirebaseDynamicLinks
) : PendingLink {

    override fun getPendingLinks(intent: Intent): Maybe<Uri> = Maybe.create { emitter ->
        dynamicLinks.getDynamicLink(intent)
            .addOnSuccessListener { linkData ->
                if (!emitter.isDisposed) {
                    linkData?.link?.let {
                        emitter.onSuccess(extractOpenBankingConsentTokenIfNeeded(it, intent))
                    } ?: emitter.onComplete()
                }
            }
            .addOnFailureListener { if (!emitter.isDisposed) emitter.onError(it) }
    }

    private fun extractOpenBankingConsentTokenIfNeeded(it: Uri, intent: Intent): Uri =
        if (it.fragment?.contains(OPEN_BANKING_APPROVAL) == true ||
            it.fragment?.contains(OPEN_BANKING_LINK) == true
        ) {
            Uri.parse(
                "$it$OPEN_BANKING_CONSENT_QUERY${
                    intent.data?.getQueryParameter(
                        OPEN_BANKING_CONSENT_VALUE
                    )
                }"
            )
        } else {
            it
        }

    companion object {
        private const val OPEN_BANKING_LINK = "ob-bank-link"
        private const val OPEN_BANKING_APPROVAL = "ob-bank-approval"
        private const val OPEN_BANKING_CONSENT_QUERY = "?one-time-token="
        private const val OPEN_BANKING_CONSENT_VALUE = "one-time-token"
    }
}