package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import com.blockchain.swap.nabu.models.nabu.Scope
import com.blockchain.swap.nabu.models.nabu.goldTierComplete
import com.blockchain.swap.nabu.models.nabu.kycVerified
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import com.blockchain.swap.nabu.models.nabu.Kyc2TierState
import com.blockchain.swap.nabu.models.nabu.UserCampaignState
import com.blockchain.swap.nabu.service.TierService
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.zipWith
import piuk.blockchain.android.campaign.blockstackCampaignName
import piuk.blockchain.android.simplebuy.SimpleBuyState
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class AnnouncementQueries(
    private val nabuToken: NabuToken,
    private val settings: SettingsDataManager,
    private val nabu: NabuDataManager,
    private val tierService: TierService,
    private val sbStateFactory: SimpleBuySyncFactory
) {
    // Attempt to figure out if KYC/swap etc is allowed based on location...
    fun canKyc(): Single<Boolean> {

        return Singles.zip(
            settings.getSettings()
                .map { it.countryCode }
                .singleOrError(),
            nabu.getCountriesList(Scope.None)
        ).map { (country, list) ->
            list.any { it.code == country && it.isKycAllowed }
        }.onErrorReturn { false }
    }

    // Have we moved past kyc tier 1 - silver?
    fun isKycGoldStartedOrComplete(): Single<Boolean> {
        return nabuToken.fetchNabuToken()
            .flatMap { token -> nabu.getUser(token) }
            .map { it.tierInProgressOrCurrentTier == 2 }
            .onErrorReturn { false }
    }

    // Have we been through the Gold KYC process? ie are we Tier2InReview, Tier2Approved or Tier2Failed (cf TierJson)
    fun isGoldComplete(): Single<Boolean> =
        tierService.tiers()
            .map { it.combinedState in goldTierComplete }

    fun isTier1Or2Verified(): Single<Boolean> =
        tierService.tiers().map { it.combinedState in kycVerified }

    fun isRegistedForStxAirdrop(): Single<Boolean> {
        return nabuToken.fetchNabuToken()
            .flatMap { token -> nabu.getUser(token) }
            .map { it.isStxAirdropRegistered }
            .onErrorReturn { false }
    }

    fun hasReceivedStxAirdrop(): Single<Boolean> {
        return nabuToken.fetchNabuToken()
            .flatMap { token -> nabu.getAirdropCampaignStatus(token) }
            .map { it[blockstackCampaignName]?.userState == UserCampaignState.RewardReceived }
    }

    fun isSimpleBuyKycInProgress(): Single<Boolean> {
        // If we have a local simple buy in progress and it has the kyc unfinished state set
        return Single.defer {
            sbStateFactory.currentState()?.let {
                Single.just(it.kycStartedButNotCompleted && !it.kycDataSubmitted())
            } ?: Single.just(false)
        }
    }

    fun isSimpleBuyTransactionPending(): Single<Boolean> {
        return Single.just(
            sbStateFactory.currentState()?.order?.orderState == OrderState.AWAITING_FUNDS &&
                    sbStateFactory.currentState()?.selectedPaymentMethod?.isBank() == true
        )
    }

    private fun hasSelectedToAddNewCard(): Single<Boolean> =
        Single.defer {
            sbStateFactory.currentState()?.let {
                Single.just(it.selectedPaymentMethod?.id == PaymentMethod.UNDEFINED_CARD_PAYMENT_ID)
            } ?: Single.just(false)
        }

    fun isKycGoldVerifiedAndHasPendingCardToAdd(): Single<Boolean> =
        tierService.tiers().map { it.combinedState == Kyc2TierState.Tier2Approved }.zipWith(
            hasSelectedToAddNewCard()) { isGold, addNewCard ->
            isGold && addNewCard
        }
}

private fun SimpleBuyState?.kycDataSubmitted(): Boolean =
    this?.kycVerificationState?.docsSubmitted() ?: false
