package com.blockchain.nabu.datamanagers.featureflags

import android.os.Build
import com.blockchain.nabu.models.data.BankPartner
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.Single

interface BankLinkingEnabledProvider {
    fun supportedBankPartners(): Single<List<BankPartner>>
    fun bankLinkingEnabled(fiatCurrency: String): Single<Boolean>
}

class BankLinkingEnabledProviderImpl(
    private val obFF: FeatureFlag
) : BankLinkingEnabledProvider {
    override fun supportedBankPartners(): Single<List<BankPartner>> =
        obFF.enabled.map { ob ->
            val supportedPartners = mutableListOf<BankPartner>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                supportedPartners.add(BankPartner.YODLEE)
            }
            if (ob) {
                supportedPartners.add(BankPartner.YAPILY)
            }
            supportedPartners
        }

    override fun bankLinkingEnabled(fiatCurrency: String): Single<Boolean> =
        if (fiatCurrency == "EUR" || fiatCurrency == "GBP") {
            obFF.enabled
        } else {
            Single.just(true)
        }
}