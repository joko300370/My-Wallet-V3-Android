package piuk.blockchain.android.ui.kyc.sunriver
//
//import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
//import com.blockchain.kyc.models.nabu.CampaignData
//import com.blockchain.kyc.models.nabu.KycState
//import com.blockchain.kyc.models.nabu.RegisterCampaignRequest
//import com.blockchain.kyc.models.nabu.UserState
//import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
//import com.blockchain.swap.nabu.NabuToken
//import com.blockchain.swap.nabu.models.NabuOfflineTokenResponse
//import com.blockchain.remoteconfig.FeatureFlag
//import com.blockchain.sunriver.SunriverCampaignSignUp
//import info.blockchain.balance.AccountReference
//import io.reactivex.Completable
//import io.reactivex.Single
//import io.reactivex.rxkotlin.Singles
//import io.reactivex.schedulers.Schedulers
//
//private const val SunRiverCampaign = "SUNRIVER"
//
//class SunriverCampaignHelper(
//    private val featureFlag: FeatureFlag,
//    private val nabuDataManager: NabuDataManager,
//    private val nabuToken: NabuToken,
//    private val kycStatusHelper: KycStatusHelper,
//    private val xlmAccountProvider: XlmAccountProvider
//) : SunriverCampaignSignUp {
//
//    interface XlmAccountProvider {
//        fun defaultAccount(): Single<AccountReference.Xlm>
//    }
//
//    fun getCampaignCardType(): Single<SunriverCardType> =
//        featureFlag.enabled
//            .flatMap { enabled -> if (enabled) getCardsForUserState() else Single.just(
//                SunriverCardType.None
//            ) }
//
//    private fun getCardsForUserState(): Single<SunriverCardType> =
//        Singles.zip(
//            kycStatusHelper.getUserState(),
//            kycStatusHelper.getKycStatus(),
//            userIsInSunRiverCampaign()
//        ).map { (userState, kycState, inSunRiverCampaign) ->
//            if (kycState == KycState.Verified && inSunRiverCampaign) {
//                SunriverCardType.Complete
//            } else if (kycState != KycState.Verified &&
//                userState == UserState.Created &&
//                inSunRiverCampaign
//            ) {
//                SunriverCardType.FinishSignUp
//            } else {
//                SunriverCardType.JoinWaitList
//            }
//        }
//
//    override fun registerSunRiverCampaign(): Completable =
//        xlmAccountProvider.defaultAccount().flatMapCompletable { xlmAccount ->
//            nabuToken.fetchNabuToken()
//                .flatMapCompletable {
//                    Completable.complete()
//                    registerSunriverCampaign(it, xlmAccount, CampaignData(SunRiverCampaign, false))
//                }
//        }
//
//    fun registerCampaignAndSignUpIfNeeded(xlmAccount: AccountReference.Xlm, campaignData: CampaignData): Completable =
//        nabuToken.fetchNabuToken()
//            .flatMapCompletable {
//                registerSunriverCampaign(it, xlmAccount, campaignData)
//            }
//
//    private fun registerSunriverCampaign(
//        token: NabuOfflineTokenResponse,
//        xlmAccount: AccountReference.Xlm,
//        campaignData: CampaignData
//    ): Completable =
//        nabuDataManager.registerCampaign(
//            token,
//            RegisterCampaignRequest.registerSunriver(
//                xlmAccount.accountId,
//                campaignData.newUser
//            ),
//            campaignData.campaignName
//        ).subscribeOn(Schedulers.io())
//
//    override fun userIsInSunRiverCampaign(): Single<Boolean> =
//        getCampaignList().map { it.contains(SunRiverCampaign) }
//
//    private fun getCampaignList(): Single<List<String>> =
//        nabuToken.fetchNabuToken().flatMap {
//            nabuDataManager.getCampaignList(it)
//        }.onErrorReturn { emptyList() }
//}
//
//sealed class SunriverCardType {
//
//    object None : SunriverCardType()
//    object JoinWaitList : SunriverCardType()
//    object FinishSignUp : SunriverCardType()
//    object Complete : SunriverCardType()
//}
