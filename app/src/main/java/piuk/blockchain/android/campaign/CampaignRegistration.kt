package piuk.blockchain.android.campaign

import com.blockchain.swap.nabu.models.responses.nabu.CampaignData
import io.reactivex.Completable
import io.reactivex.Single

interface CampaignRegistration {
    fun registerCampaign(): Completable
    fun registerCampaign(campaignData: CampaignData): Completable
    fun userIsInCampaign(): Single<Boolean>
}