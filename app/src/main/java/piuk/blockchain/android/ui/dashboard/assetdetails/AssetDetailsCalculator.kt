package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.remoteconfig.FeatureFlag
import com.jakewharton.rxrelay2.BehaviorRelay
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.prices.data.PriceDatum
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
    val interestRate: Double = AssetDetailsCalculator.NOT_USED
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

    private data class Details(
        val account: BlockchainAccount,
        val balance: Money,
        val actions: AvailableActions,
        val shouldShow: Boolean
    )

    private fun Single<AccountGroup>.mapDetails(): Single<Details> =
        this.flatMap { grp ->
            grp.balance.map { balance ->
                Details(
                    grp,
                    balance,
                    grp.actions,
                    grp.accounts.isNotEmpty()
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
            AssetFilter.All to AssetDisplayInfo(total.account, total.balance, totalFiat, total.actions)
        ).apply {
            if (nonCustodial.shouldShow) {
                put(
                    AssetFilter.NonCustodial,
                    AssetDisplayInfo(nonCustodial.account, nonCustodial.balance, walletFiat, nonCustodial.actions)
                )
            }

            if (custodial.shouldShow) {
                put(
                    AssetFilter.Custodial,
                    AssetDisplayInfo(custodial.account, custodial.balance, custodialFiat, custodial.actions)
                )
            }

            if (interest.shouldShow && interestEnabled) {
                put(
                    AssetFilter.Interest,
                    AssetDisplayInfo(interest.account, interest.balance, interestFiat, interest.actions, interestRate)
                )
            }
        }
    }

    companion object {
        const val NOT_USED: Double = -99.0
    }
}
