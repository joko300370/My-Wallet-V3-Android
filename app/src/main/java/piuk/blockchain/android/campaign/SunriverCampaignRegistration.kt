package piuk.blockchain.android.campaign

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.CampaignData
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.RegisterCampaignRequest
import com.blockchain.kyc.models.nabu.UserState
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.models.NabuOfflineTokenResponse
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.sunriver.XlmDataManager
import info.blockchain.balance.AccountReference
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import io.reactivex.schedulers.Schedulers

private const val SunRiverCampaign = "SUNRIVER"

class SunriverCampaignRegistration(
    private val featureFlag: FeatureFlag,
    private val nabuDataManager: NabuDataManager,
    private val nabuToken: NabuToken,
    private val kycStatusHelper: KycStatusHelper,
    private val xlmDataManager: XlmDataManager
) : CampaignRegistration {

    private fun defaultAccount(): Single<AccountReference.Xlm> = xlmDataManager.defaultAccount()

    fun getCampaignCardType(): Single<SunriverCardType> =
        featureFlag.enabled
            .flatMap { enabled -> if (enabled) getCardsForUserState() else Single.just(
                SunriverCardType.None
            ) }

    private fun getCardsForUserState(): Single<SunriverCardType> =
        Singles.zip(
            kycStatusHelper.getUserState(),
            kycStatusHelper.getKycStatus(),
            userIsInCampaign()
        ).map { (userState, kycState, inSunRiverCampaign) ->
            if (kycState == KycState.Verified && inSunRiverCampaign) {
                SunriverCardType.Complete
            } else if (kycState != KycState.Verified &&
                userState == UserState.Created &&
                inSunRiverCampaign
            ) {
                SunriverCardType.FinishSignUp
            } else {
                SunriverCardType.JoinWaitList
            }
        }

    override fun registerCampaign(): Completable =
        registerCampaign(CampaignData(SunRiverCampaign, false))

    override fun registerCampaign(campaignData: CampaignData): Completable =
        defaultAccount().flatMapCompletable { xlmAccount ->
            nabuToken.fetchNabuToken()
                .flatMapCompletable {
                    doRegisterCampaign(it, xlmAccount, campaignData)
                }
        }

    private fun doRegisterCampaign(
        token: NabuOfflineTokenResponse,
        xlmAccount: AccountReference.Xlm,
        campaignData: CampaignData
    ): Completable =
        nabuDataManager.registerCampaign(
            token,
            RegisterCampaignRequest.registerSunriver(
                xlmAccount.accountId,
                campaignData.newUser
            ),
            campaignData.campaignName
        ).subscribeOn(Schedulers.io())

    override fun userIsInCampaign(): Single<Boolean> =
        getCampaignList().map { it.contains(SunRiverCampaign) }

    private fun getCampaignList(): Single<List<String>> =
        nabuToken.fetchNabuToken().flatMap {
            nabuDataManager.getCampaignList(it)
        }.onErrorReturn { emptyList() }
}

sealed class SunriverCardType {
    object None : SunriverCardType()
    object JoinWaitList : SunriverCardType()
    object FinishSignUp : SunriverCardType()
    object Complete : SunriverCardType()
}
