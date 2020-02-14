package piuk.blockchain.android.ui.dashboard

import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.coincore.AssetTokenLookup
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.androidcore.data.charts.TimeSpan
import timber.log.Timber

class DashboardInteractor(
    private val tokens: AssetTokenLookup,
    private val payloadManager: PayloadManager
) {
    // We have a problem here, in that pax init depends on ETH init
    // Ultimately, we want to init metadata straight after decrypting (or creating) the wallet
    // but we can't move that somewhere sensible yet, because 2nd password. When we remove that -
    // which is on the radar - then we can clean up the entire app init sequence.
    // But for now, we'll catch any pax init failure here, unless ETH has initialised OK. And when we
    // get a valid ETH balance, will try for a PX balance. Yeah, this is a nasty hack TODO: Fix this
    fun refreshBalances(model: DashboardModel, balanceFilter: AssetFilter): Disposable {
        val cd = CompositeDisposable()
        CryptoCurrency.activeCurrencies()
            .filter { it != CryptoCurrency.PAX }
            .forEach {
                cd += tokens[it].totalBalance(balanceFilter)
                    .doOnSuccess { value ->
                        if (value.currency == CryptoCurrency.ETHER) {
                            cd += tokens[CryptoCurrency.PAX].totalBalance(balanceFilter)
                                .subscribeBy(
                                    onSuccess = { balance ->
                                        Timber.d("*****> Got balance for PAX")
                                        model.process(BalanceUpdate(CryptoCurrency.PAX, balance))
                                    },
                                    onError = { e ->
                                        Timber.e("Failed getting balance for PAX: $e")
                                        model.process(BalanceUpdateError(CryptoCurrency.PAX))
                                    }
                                )
                        }
                    }
                    .doOnError { _ ->
                        if (it == CryptoCurrency.ETHER) {
                            // If we can't get ETH, then we can't get PAX... so...
                            model.process(BalanceUpdateError(CryptoCurrency.PAX))
                        }
                    }
                    .subscribeBy(
                        onSuccess = { balance ->
                            Timber.d("*****> Got balance for $it")
                            model.process(BalanceUpdate(it, balance))
                        },
                        onError = { e ->
                            Timber.e("Failed getting balance for $it: $e")
                            model.process(BalanceUpdateError(it))
                        }
                    )
            }
        return cd
    }

    fun refreshPrices(model: DashboardModel, crypto: CryptoCurrency): Disposable {
        val oneDayAgo = (System.currentTimeMillis() / 1000) - ONE_DAY

        return Singles.zip(
            tokens[crypto].exchangeRate(),
            tokens[crypto].historicRate(oneDayAgo)
        ) { rate, day -> PriceUpdate(crypto, rate, day) }
            .subscribeBy(
                onSuccess = { model.process(it) },
                onError = { Timber.e(it) }
            )
    }

    fun refreshPriceHistory(model: DashboardModel, crypto: CryptoCurrency): Disposable =
        if (crypto.hasFeature(CryptoCurrency.PRICE_CHARTING)) {
            tokens[crypto].historicRateSeries(TimeSpan.DAY, TimeInterval.ONE_HOUR)
        } else {
            Single.just(FLATLINE_CHART)
        }
        .map { PriceHistoryUpdate(crypto, it) }
        .subscribeBy(
            onSuccess = { model.process(it) },
            onError = { Timber.e(it) }
        )

    fun checkForCustodialBalance(model: DashboardModel, crypto: CryptoCurrency): Disposable? {
        return tokens[crypto].totalBalance(AssetFilter.Custodial)
            .subscribeBy(
                onSuccess = { model.process(UpdateHasCustodialBalanceIntent(crypto, !it.isZero)) },
                onError = { Timber.e(it) }
            )
    }

    fun hasUserBackedUp(model: DashboardModel): Disposable? {
        return Single.just(payloadManager.isWalletBackedUp)
            .subscribeBy(
                onSuccess = { model.process(BackupStatusUpdate(it)) },
                onError = { Timber.e(it) }
            )
    }

    companion object {
        private const val ONE_DAY = 24 * 60 * 60L
        private val FLATLINE_CHART = listOf(
            PriceDatum(price = 1.0, timestamp = 0),
            PriceDatum(price = 1.0, timestamp = System.currentTimeMillis() / 1000)
        )
    }
}
