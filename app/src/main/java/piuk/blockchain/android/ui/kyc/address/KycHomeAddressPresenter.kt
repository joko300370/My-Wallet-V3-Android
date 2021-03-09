package piuk.blockchain.android.ui.kyc.address

import piuk.blockchain.android.ui.kyc.BaseKycPresenter
import com.blockchain.nabu.models.responses.nabu.Scope
import piuk.blockchain.android.ui.kyc.address.models.AddressModel
import piuk.blockchain.android.campaign.CampaignType
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.notifications.analytics.Analytics
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.networking.PollService
import piuk.blockchain.android.sdd.SDDAnalytics
import piuk.blockchain.androidcore.utils.extensions.thenSingle
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber
import java.util.SortedMap

interface KycNextStepDecision {

    enum class NextStep {
        Tier1Complete,
        SDDComplete,
        Tier2ContinueTier1NeedsMoreInfo,
        Tier2Continue
    }

    fun nextStep(): Single<NextStep>
}

class KycHomeAddressPresenter(
    nabuToken: NabuToken,
    private val nabuDataManager: NabuDataManager,
    private val kycNextStepDecision: KycNextStepDecision,
    private val custodialWalletManager: CustodialWalletManager,
    private val analytics: Analytics
) : BaseKycPresenter<KycHomeAddressView>(nabuToken) {

    val countryCodeSingle: Single<SortedMap<String, String>> by unsafeLazy {
        fetchOfflineToken
            .flatMap {
                nabuDataManager.getCountriesList(Scope.None)
                    .subscribeOn(Schedulers.io())
            }
            .map { list ->
                list.associateBy({ it.name }, { it.code })
                    .toSortedMap()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .cache()
    }

    override fun onViewReady() {
        compositeDisposable += view.address
            .subscribeBy(
                onNext = { enableButtonIfComplete(it) },
                onError = {
                    Timber.e(it)
                    // This is fatal - back out and allow the user to try again
                    view.finishPage()
                }
            )

        restoreDataIfPresent()
    }

    private fun restoreDataIfPresent() {
        compositeDisposable +=
            view.address
                .firstElement()
                .flatMap { addressModel ->
                    // Don't attempt to restore state if data is already present
                    if (addressModel.containsData()) {
                        Maybe.empty()
                    } else {
                        fetchOfflineToken
                            .flatMapMaybe { tokenResponse ->
                                nabuDataManager.getUser(tokenResponse)
                                    .subscribeOn(Schedulers.io())
                                    .flatMapMaybe { user ->
                                        user.address?.let { address ->
                                            Maybe.just(address)
                                                .flatMap { getCountryName(address.countryCode!!) }
                                                .map { it to address }
                                        } ?: Maybe.empty()
                                    }
                            }
                            .observeOn(AndroidSchedulers.mainThread())
                    }
                }
                .subscribeBy(
                    onSuccess = { (countryName, address) ->
                        view.restoreUiState(
                            address.line1!!,
                            address.line2,
                            address.city!!,
                            address.state,
                            address.postCode,
                            countryName
                        )
                    },
                    onError = {
                        // Silently fail
                        Timber.e(it)
                    }
                )
    }

    private data class State(
        val progressToKycNextStep: KycNextStepDecision.NextStep,
        val countryCode: String
    )

    internal fun onContinueClicked(campaignType: CampaignType? = null) {
        compositeDisposable += view.address
            .firstOrError()
            .flatMap { address ->
                addAddress(address).toSingle { address.country }
            }
            .flatMap { countryCode ->
                updateNabuData().thenSingle { Single.just(countryCode) }
            }
            .map { countryCode ->
                State(
                    progressToKycNextStep = KycNextStepDecision.NextStep.Tier1Complete,
                    countryCode = countryCode
                )
            }
            .zipWith(kycNextStepDecision.nextStep())
            .map { (x, progress) -> x.copy(progressToKycNextStep = progress) }
            .flatMap { state ->
                if (campaignType?.shouldCheckForSddVerification() == true) {
                    tryToVerifyUserForSdd(state, campaignType)
                } else Single.just(state)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view.showProgressDialog() }
            .doOnEvent { _, _ -> view.dismissProgressDialog() }
            .doOnError(Timber::e)
            .subscribeBy(
                onSuccess = {
                    when (it.progressToKycNextStep) {
                        KycNextStepDecision.NextStep.Tier1Complete -> view.tier1Complete()
                        KycNextStepDecision.NextStep.Tier2ContinueTier1NeedsMoreInfo ->
                            view.continueToTier2MoreInfoNeeded(it.countryCode)
                        KycNextStepDecision.NextStep.Tier2Continue -> view.continueToVeriffSplash(it.countryCode)
                        KycNextStepDecision.NextStep.SDDComplete -> view.onSddVerified()
                    }
                },
                onError = { view.showErrorToast(R.string.kyc_address_error_saving) }
            )
    }

    private fun tryToVerifyUserForSdd(state: State, campaignType: CampaignType): Single<State> {
        return custodialWalletManager.isSDDEligible().doOnSuccess {
            if (it) {
                analytics.logEventOnce(SDDAnalytics.SDD_ELIGIBLE)
            }
        }.flatMap {
            if (!it) {
                Single.just(state)
            } else {
                PollService(custodialWalletManager.fetchSDDUserState()) { sddState ->
                    sddState.stateFinalised
                }.start(timerInSec = 1, retries = 10).map { sddState ->
                    if (sddState.value.isVerified) {
                        if (shouldNotContinueToNextKycTier(state, campaignType))
                            state.copy(progressToKycNextStep = KycNextStepDecision.NextStep.SDDComplete)
                        else state
                    } else
                        state
                }
            }
        }
    }

    private fun shouldNotContinueToNextKycTier(
        state: State,
        campaignType: CampaignType
    ): Boolean {
        return state.progressToKycNextStep < KycNextStepDecision.NextStep.SDDComplete ||
            campaignType == CampaignType.SimpleBuy
    }

    private fun addAddress(address: AddressModel): Completable = fetchOfflineToken.flatMapCompletable {
        nabuDataManager.addAddress(
            it,
            address.firstLine,
            address.secondLine,
            address.city,
            address.state,
            address.postCode,
            address.country
        ).subscribeOn(Schedulers.io())
    }

    private fun updateNabuData(): Completable =
        nabuDataManager.requestJwt()
            .subscribeOn(Schedulers.io())
            .flatMap { jwt ->
                fetchOfflineToken.flatMap {
                    nabuDataManager.updateUserWalletInfo(it, jwt)
                        .subscribeOn(Schedulers.io())
                }
            }
            .ignoreElement()

    private fun getCountryName(countryCode: String): Maybe<String> = countryCodeSingle
        .map { it.entries.first { (_, value) -> value == countryCode }.key }
        .toMaybe()

    private fun enableButtonIfComplete(addressModel: AddressModel) {
        if (addressModel.country.equals("US", ignoreCase = true)) {
            view.setButtonEnabled(
                addressModel.firstLine.isNotEmpty() &&
                    addressModel.city.isNotEmpty() &&
                    addressModel.state.isNotEmpty() &&
                    addressModel.postCode.isNotEmpty()
            )
        } else {
            view.setButtonEnabled(
                addressModel.firstLine.isNotEmpty() &&
                    addressModel.city.isNotEmpty() &&
                    addressModel.postCode.isNotEmpty()
            )
        }
    }

    internal fun onProgressCancelled() {
        compositeDisposable.clear()
    }

    private fun AddressModel.containsData(): Boolean =
        firstLine.isNotEmpty() ||
            !secondLine.isNullOrEmpty() ||
            city.isNotEmpty() ||
            state.isNotEmpty() ||
            postCode.isNotEmpty()
}

private fun CampaignType.shouldCheckForSddVerification(): Boolean =
    this == CampaignType.SimpleBuy || this == CampaignType.None
