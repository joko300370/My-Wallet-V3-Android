package piuk.blockchain.android.ui.dashboard.assetdetails

import com.jakewharton.rxrelay2.BehaviorRelay
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.toFiat
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AssetTokens
import piuk.blockchain.androidcore.data.charts.TimeSpan
import java.util.Locale

typealias BalancePair = Pair<CryptoValue, FiatValue>
typealias BalanceMap = Map<AssetFilter, BalancePair>

class AssetDetailsCalculator(locale: Locale) {
    // input
    val token = BehaviorRelay.create<AssetTokens>()
    val timeSpan = BehaviorRelay.createDefault<TimeSpan>(TimeSpan.MONTH)

    private val _chartLoading: BehaviorRelay<Boolean> = BehaviorRelay.createDefault<Boolean>(false)

    val chartLoading: Observable<Boolean>
        get() = _chartLoading

    val exchangeRate: Observable<String> = token.flatMapSingle {
        it.exchangeRate()
    }.map {
        it.toStringWithSymbol(locale)
    }.subscribeOn(Schedulers.io())

    val historicPrices: Observable<List<PriceDatum>> =
        (timeSpan.distinctUntilChanged().withLatestFrom(token).doOnNext { _chartLoading.accept(true) })
            .switchMapSingle { (timeSpan, token) ->
                token.historicRateSeries(timeSpan, TimeInterval.FIFTEEN_MINUTES)
                    .onErrorResumeNext(Single.just(emptyList()))
            }
            .doOnNext { _chartLoading.accept(false) }
            .subscribeOn(Schedulers.io())

    // output
    val balanceMap: Observable<BalanceMap> =
        token.flatMapSingle {
            getBalances(it)
        }.subscribeOn(Schedulers.computation())

    private fun getBalances(assetTokens: AssetTokens): Single<BalanceMap> {
        return Singles.zip(
            assetTokens.exchangeRate(),
            assetTokens.totalBalance(AssetFilter.Total),
            assetTokens.totalBalance(AssetFilter.Wallet),
            assetTokens.totalBalance(AssetFilter.Custodial)
        ) { fiatPrice, totalBalance, walletBalance, custodialBalance ->
            val totalFiat = totalBalance.toFiat(fiatPrice)
            val walletFiat = walletBalance.toFiat(fiatPrice)
            val custodialFiat = custodialBalance.toFiat(fiatPrice)

            mutableMapOf(
                AssetFilter.Total to BalancePair(totalBalance, totalFiat),
                AssetFilter.Wallet to BalancePair(walletBalance, walletFiat)
            ).apply {
                if (assetTokens.hasActiveWallet(AssetFilter.Custodial)) {
                    put(AssetFilter.Custodial, BalancePair(custodialBalance, custodialFiat))
                }
            }
        }
    }
}
