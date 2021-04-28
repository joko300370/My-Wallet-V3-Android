package piuk.blockchain.android.deeplink

import android.content.Intent
import android.net.Uri
import com.blockchain.notifications.links.PendingLink
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.android.kyc.KycDeepLinkHelper
import piuk.blockchain.android.kyc.KycLinkState
import piuk.blockchain.android.sunriver.CampaignLinkState
import piuk.blockchain.android.sunriver.SunriverDeepLinkHelper
import piuk.blockchain.android.thepit.ThePitDeepLinkParser

internal class DeepLinkProcessor(
    private val linkHandler: PendingLink,
    private val emailVerifiedLinkHelper: EmailVerificationDeepLinkHelper,
    private val kycDeepLinkHelper: KycDeepLinkHelper,
    private val sunriverDeepLinkHelper: SunriverDeepLinkHelper,
    private val thePitDeepLinkParser: ThePitDeepLinkParser,
    private val openBankingDeepLinkParser: OpenBankingDeepLinkParser
) {
    fun getLink(intent: Intent): Single<LinkState> =
        linkHandler.getPendingLinks(intent).switchIfEmpty(Single.never()).flatMap {
            urlProcessor(it)
        }

    fun getLink(link: String): Single<LinkState> =
        urlProcessor(Uri.parse(link))

    private fun urlProcessor(uri: Uri): Single<LinkState> =
        Maybe.fromCallable {
            val emailVerifiedUri = emailVerifiedLinkHelper.mapUri(uri)
            if (emailVerifiedUri != EmailVerifiedLinkState.NoUri) {
                return@fromCallable LinkState.EmailVerifiedDeepLink(emailVerifiedUri)
            }
            val sr = sunriverDeepLinkHelper.mapUri(uri)
            if (sr !is CampaignLinkState.NoUri) {
                return@fromCallable LinkState.SunriverDeepLink(sr)
            }
            val kyc = kycDeepLinkHelper.mapUri(uri)
            if (kyc != KycLinkState.NoUri) {
                return@fromCallable LinkState.KycDeepLink(kyc)
            }
            val linkId = thePitDeepLinkParser.mapUri(uri)
            if (linkId != null) {
                return@fromCallable LinkState.ThePitDeepLink(linkId)
            }
            val openBankingDeepLink = openBankingDeepLinkParser.mapUri(uri)
            if (openBankingDeepLink != null && openBankingDeepLink.type != OpenBankingLinkType.UNKNOWN) {
                return@fromCallable openBankingDeepLink
            }
            LinkState.NoUri
        }
            .switchIfEmpty(Maybe.just(LinkState.NoUri))
            .toSingle()
            .onErrorResumeNext { Single.just(LinkState.NoUri) }
}

sealed class LinkState {
    data class EmailVerifiedDeepLink(val link: EmailVerifiedLinkState) : LinkState()
    data class SunriverDeepLink(val link: CampaignLinkState) : LinkState()
    data class KycDeepLink(val link: KycLinkState) : LinkState()
    data class ThePitDeepLink(val linkId: String) : LinkState()
    data class OpenBankingLink(val type: OpenBankingLinkType, val consentToken: String) : LinkState()

    object NoUri : LinkState()
}
