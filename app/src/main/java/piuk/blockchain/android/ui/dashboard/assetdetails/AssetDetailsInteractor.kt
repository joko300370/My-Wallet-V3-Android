package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.preferences.DashboardPrefs
import com.blockchain.remoteconfig.FeatureFlag
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.androidcore.data.charts.TimeSpan

typealias AssetDisplayMap = Map<AssetFilter, AssetDisplayInfo>
data class AssetDisplayInfo(
    val account: BlockchainAccount,
    val amount: Money,
    val fiatValue: Money,
    val actions: Set<AssetAction>,
    val interestRate: Double = Double.NaN
)

class AssetDetailsInteractor(
    private val interestFeatureFlag: FeatureFlag,
    private val dashboardPrefs: DashboardPrefs,
    private val coincore: Coincore
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

    fun shouldShowCustody(cryptoCurrency: CryptoCurrency): Single<Boolean> {
        return coincore[cryptoCurrency].accountGroup(AssetFilter.Custodial)
            .flatMapSingle { it.accountBalance }
            .map {
                !dashboardPrefs.isCustodialIntroSeen && !it.isZero
            }
    }

    private sealed class Details {
        object NoDetails : Details()
        class DetailsItem(
            val account: BlockchainAccount,
            val balance: Money,
            val actions: AvailableActions
        ) : Details()
    }

    private fun Maybe<AccountGroup>.mapDetails(): Single<Details> =
        this.flatMap { grp ->
            grp.accountBalance.toMaybe().map { balance ->
                Details.DetailsItem(
                    grp,
                    balance,
                    grp.actions
                ) as Details
            }
        }.toSingle(Details.NoDetails)

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
    ): AssetDisplayMap = mutableMapOf<AssetFilter, AssetDisplayInfo>().apply {
            addToDisplayMap(this, AssetFilter.All, total, fiatRate)
            addToDisplayMap(this, AssetFilter.NonCustodial, nonCustodial, fiatRate)
            addToDisplayMap(this, AssetFilter.Custodial, custodial, fiatRate)
            if (interestEnabled) {
                addToDisplayMap(this, AssetFilter.Interest, interest, fiatRate, interestRate)
            }
    }

    private fun addToDisplayMap(
        map: MutableMap<AssetFilter, AssetDisplayInfo>,
        filter: AssetFilter,
        item: Details,
        fiatRate: ExchangeRate,
        interestRate: Double = Double.NaN
    ) {
        (item as? Details.DetailsItem)?.let {
            val fiat = fiatRate.convert(it.balance)
            map.put(
                filter,
                AssetDisplayInfo(
                    it.account,
                    it.balance,
                    fiat,
                    it.actions,
                    interestRate
                )
            )
        }
    }
}
