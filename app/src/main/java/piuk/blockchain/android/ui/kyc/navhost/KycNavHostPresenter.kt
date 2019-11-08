package piuk.blockchain.android.ui.kyc.navhost

import piuk.blockchain.android.ui.kyc.BaseKycPresenter
import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.UserState
import com.blockchain.kyc.services.nabu.TierUpdater
import piuk.blockchain.android.ui.kyc.logging.KycResumedEvent
import piuk.blockchain.android.ui.kyc.profile.models.ProfileModel
import piuk.blockchain.android.ui.kyc.reentry.KycNavigator
import piuk.blockchain.android.ui.kyc.reentry.ReentryDecision
import com.blockchain.swap.nabu.NabuToken
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.BlockstackCampaignRegistration
import piuk.blockchain.android.campaign.CampaignRegistration
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.campaign.SunriverCampaignRegistration
import piuk.blockchain.androidcoreui.utils.logging.Logging
import timber.log.Timber

class KycNavHostPresenter(
    nabuToken: NabuToken,
    private val nabuDataManager: NabuDataManager,
    private val sunriverCampaign: SunriverCampaignRegistration,
    private val blockstackCampaign: BlockstackCampaignRegistration,
    private val reentryDecision: ReentryDecision,
    private val kycNavigator: KycNavigator,
    private val tierUpdater: TierUpdater
) : BaseKycPresenter<KycNavHostView>(nabuToken) {

    override fun onViewReady() {
        compositeDisposable +=
            fetchOfflineToken.flatMap {
                nabuDataManager.getUser(it)
                    .subscribeOn(Schedulers.io())
            }.observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view.displayLoading(true) }
                .subscribeBy(
                    onSuccess = {
                        registerForCampaignsIfNeeded()
                        updateTier2SelectedTierIfNeeded()
                        redirectUserFlow(it)
                    },
                    onError = {
                        Timber.e(it)
                        if (it is MetadataNotFoundException) {
                            // No user, hide loading and start full KYC flow
                            view.displayLoading(false)
                        } else {
                            view.showErrorToastAndFinish(R.string.kyc_status_error)
                        }
                    }
                )
    }

    /**
     * Registers the user to the various campaigns if they are not yet registered with them, on completion of Gold
     */
    private fun registerForCampaignsIfNeeded() {

        if (view.campaignType == CampaignType.Sunriver) {
            checkAndRegisterForCampaign(sunriverCampaign)
        }

        // Attempt to register for the Blockstack airdrop campaign
        checkAndRegisterForCampaign(blockstackCampaign)
    }

    private fun checkAndRegisterForCampaign(campaign: CampaignRegistration) {
        compositeDisposable += campaign.userIsInCampaign()
            .flatMapCompletable { isInCampaign ->
                if (isInCampaign) {
                    Completable.complete()
                } else {
                    campaign.registerCampaign()
                }
            }
            .subscribeOn(Schedulers.io())
            .doOnError(Timber::e)
            .subscribe()
    }

    private fun updateTier2SelectedTierIfNeeded() {
        if (view.campaignType != CampaignType.Sunriver || view.campaignType != CampaignType.BuySell) {
            return
        }

        compositeDisposable += tierUpdater
            .setUserTier(2)
            .doOnError(Timber::e)
            .subscribe()
    }

    private fun redirectUserFlow(user: NabuUser) {
        if (view.campaignType == CampaignType.BuySell) {
            view.navigateToKycSplash()
        } else if (view.campaignType == CampaignType.Resubmission || user.isMarkedForResubmission) {
            view.navigateToResubmissionSplash()
        } else if (user.state != UserState.None && user.kycState == KycState.None && !view.isFromSettingsLimits) {
            val current = user.tiers?.current
            if (current == null || current == 0) {
                val reentryPoint = reentryDecision.findReentryPoint(user)
                val directions = kycNavigator.userAndReentryPointToDirections(user, reentryPoint)
                view.navigate(directions)
                Logging.logCustom(KycResumedEvent(reentryPoint))
            }
        } else if (view.campaignType == CampaignType.Sunriver) {
            view.navigateToKycSplash()
        }

        // If no other methods are triggered, this will start KYC from scratch. If others have been called,
        // this will make the host fragment visible.
        view.displayLoading(false)
    }
}

internal fun NabuUser.toProfileModel(): ProfileModel = ProfileModel(
    firstName ?: throw IllegalStateException("First Name is null"),
    lastName ?: throw IllegalStateException("Last Name is null"),
    address?.countryCode ?: throw IllegalStateException("Country Code is null")
)