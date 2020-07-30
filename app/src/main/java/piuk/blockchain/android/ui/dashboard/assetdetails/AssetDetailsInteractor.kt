package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.remoteconfig.FeatureFlag
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.androidcore.data.charts.TimeSpan

class AssetDetailsInteractor(
    private val interestFeatureFlag: FeatureFlag
) {

    fun loadAssetDetails(asset: CryptoAsset) =
        getAssetDisplayDetails(asset)

    fun loadExchangeRate(asset: CryptoAsset) =
        asset.exchangeRate().map {
            it.price().toStringWithSymbol()
        }

    fun loadHistoricPrices(asset: CryptoAsset, timeSpan: TimeSpan) =
        asset.historicRateSeries(timeSpan, TimeInterval.FIFTEEN_MINUTES)
            .onErrorResumeNext(Single.just(emptyList()))

    private data class Details(
        val account: BlockchainAccount,
        val balance: Money,
        val actions: AvailableActions,
        val shouldShow: Boolean
    )

    private fun Single<AccountGroup>.mapDetails(
        showUnfunded: Boolean = false
    ): Single<Details> =
        this.flatMap { grp ->
            grp.balance.map { balance ->
                Details(
                    grp,
                    balance,
                    grp.actions,
                    grp.accounts.isNotEmpty() && (showUnfunded || grp.isFunded)
                )
            }
        }

    private fun getAssetDisplayDetails(asset: CryptoAsset): Single<AssetDisplayMap> {
        return Singles.zip(
            asset.exchangeRate(),
            asset.accountGroup(AssetFilter.All).mapDetails(),
            asset.accountGroup(AssetFilter.NonCustodial).mapDetails(),
            asset.accountGroup(AssetFilter.Custodial).mapDetails(),
            asset.accountGroup(AssetFilter.Interest).mapDetails(),
            asset.interestRate(),
            interestFeatureFlag.enabled
        ) { fiatRate, total, nonCustodial, custodial, interest, interestRate, interestEnabled ->
            makeAssetDisplayMap(
                fiatRate, total, nonCustodial, custodial, interest, interestRate, interestEnabled
            )
        }
    }

    private fun makeAssetDisplayMap(
        fiatRate: ExchangeRate,
        total: Details,
        nonCustodial: Details,
        custodial: Details,
        interest: Details,
        interestRate: Double,
        interestEnabled: Boolean
    ): AssetDisplayMap {
        val totalFiat = fiatRate.convert(total.balance)
        val walletFiat = fiatRate.convert(nonCustodial.balance)
        val custodialFiat = fiatRate.convert(custodial.balance)
        val interestFiat = fiatRate.convert(interest.balance)

        return mutableMapOf(
            AssetFilter.All to AssetDisplayInfo(total.account, total.balance, totalFiat,
                total.actions)
        ).apply {
            if (nonCustodial.shouldShow) {
                put(
                    AssetFilter.NonCustodial,
                    AssetDisplayInfo(nonCustodial.account, nonCustodial.balance, walletFiat,
                        nonCustodial.actions)
                )
            }

            if (custodial.shouldShow) {
                put(
                    AssetFilter.Custodial,
                    AssetDisplayInfo(custodial.account, custodial.balance, custodialFiat,
                        custodial.actions)
                )
            }

            if (interest.shouldShow && interestEnabled) {
                put(
                    AssetFilter.Interest,
                    AssetDisplayInfo(interest.account, interest.balance, interestFiat,
                        interest.actions, interestRate)
                )
            }
        }
    }
}
