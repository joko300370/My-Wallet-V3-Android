package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.remoteconfig.FeatureFlag
import com.jakewharton.rxrelay2.BehaviorRelay
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.androidcore.data.charts.TimeSpan

data class AssetDisplayInfo(
    val account: BlockchainAccount,
    val amount: Money,
    val fiatValue: Money,
    val actions: Set<AssetAction>,
    val interestRate: Double = Double.NaN
)

typealias AssetDisplayMap = Map<AssetFilter, AssetDisplayInfo>

class AssetDetailsCalculator(private val interestFeatureFlag: FeatureFlag) {
    // input
    val token = BehaviorRelay.create<CryptoAsset>()
    val timeSpan = BehaviorRelay.createDefault<TimeSpan>(TimeSpan.DAY)

    private val _chartLoading: BehaviorRelay<Boolean> = BehaviorRelay.createDefault<Boolean>(false)

    val chartLoading: Observable<Boolean>
        get() = _chartLoading

    val exchangeRate: Observable<String> = token.flatMapSingle {
        it.exchangeRate()
    }.map {
        it.price().toStringWithSymbol()
    }.subscribeOn(Schedulers.io())

    val historicPrices: Observable<List<PriceDatum>> =
        (timeSpan.distinctUntilChanged().withLatestFrom(token)
            .doOnNext { _chartLoading.accept(true) })
            .switchMapSingle { (timeSpan, token) ->
                token.historicRateSeries(timeSpan, TimeInterval.FIFTEEN_MINUTES)
                    .onErrorResumeNext(Single.just(emptyList()))
            }
            .doOnNext { _chartLoading.accept(false) }
            .subscribeOn(Schedulers.io())

    // output
    val assetDisplayDetails: Observable<AssetDisplayMap> =
        token.flatMapSingle {
            getAssetDisplayDetails(it)
        }.subscribeOn(Schedulers.computation())

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
            grp.balance.toMaybe().map { balance ->
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
    ): AssetDisplayMap {
        return mutableMapOf<AssetFilter, AssetDisplayInfo>().apply {
            addToDisplayMap(this, AssetFilter.All, total, fiatRate)
            addToDisplayMap(this, AssetFilter.NonCustodial, nonCustodial, fiatRate)
            addToDisplayMap(this, AssetFilter.Custodial, custodial, fiatRate)
            if (interestEnabled) {
                addToDisplayMap(this, AssetFilter.Interest, interest, fiatRate, interestRate)
            }
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
