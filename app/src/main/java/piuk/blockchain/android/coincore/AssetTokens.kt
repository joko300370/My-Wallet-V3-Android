package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Single
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan

enum class AssetFilter {
    Total,
    Wallet,
    Custodial,
    Interest
}

enum class AssetAction {
    ViewActivity,
    Send,
    Receive,
    Swap
}

typealias AvailableActions = Set<AssetAction>

// TODO: For account fetching/default accounts look steal the code from xxxAccountListAdapter in core

interface AssetTokens {
    val asset: CryptoCurrency

    fun defaultAccount(): Single<CryptoSingleAccount>
    fun accounts(filter: AssetFilter = AssetFilter.Total): Single<CryptoAccountGroup>
    fun interestRate(): Single<Double>

    fun exchangeRate(): Single<FiatValue>
    fun historicRate(epochWhen: Long): Single<FiatValue>
    fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries>
}
