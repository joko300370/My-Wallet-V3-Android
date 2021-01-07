package piuk.blockchain.android.simplebuy.yodlee

import com.blockchain.nabu.models.data.LinkBankTransfer
import piuk.blockchain.android.simplebuy.ErrorState

interface YodleeActions {
    fun launchYodleeSplash(fastLinkUrl: String, accessToken: String, configName: String)
    fun launchYodleeWebview(fastLinkUrl: String, accessToken: String, configName: String)
    fun linkBankWithPartner(bankTransfer: LinkBankTransfer)
    fun launchBankLinking(accountProviderId: String, accountId: String)
    fun launchBankLinkingWithError(errorState: ErrorState)
}