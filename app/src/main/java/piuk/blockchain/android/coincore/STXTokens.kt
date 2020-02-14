package piuk.blockchain.android.coincore

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.stx.STXAccount
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.rxjava.RxBus

class STXTokens(
    rxBus: RxBus,
    private val payloadManager: PayloadManager,
    private val currencyPrefs: CurrencyPrefs,
    private val custodialWalletManager: CustodialWalletManager
) : AssetTokensBase(rxBus) {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.STX

    override fun defaultAccount(): Single<AccountReference> =
        Single.just(getDefaultStxAccountRef())

    private fun getDefaultStxAccountRef(): AccountReference {
        val hdWallets = payloadManager.payload?.hdWallets
            ?: throw IllegalStateException("Wallet not available")

        return hdWallets[0].stxAccount.toAccountReference()
    }

    override fun custodialBalanceMaybe(): Maybe<CryptoValue> {
        TODO("not implemented")
    }

    override fun noncustodialBalance(): Single<CryptoValue> {
        TODO("not implemented")
    }

    override fun balance(account: AccountReference): Single<CryptoValue> {
        TODO("not implemented")
    }

    override fun exchangeRate(): Single<FiatValue> {
        TODO("not implemented")
    }

    override fun historicRate(epochWhen: Long): Single<FiatValue> {
        TODO("not implemented")
    }

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> {
        TODO("not implemented")
    }

    // No supported actions at this time
    override val noncustodialActions = emptySet<AssetAction>()
    override val custodialActions = emptySet<AssetAction>()
}

private fun STXAccount.toAccountReference(): AccountReference.Stx =
    AccountReference.Stx(
        _label = "STX Account",
        address = bitcoinSerializedBase58Address
    )
