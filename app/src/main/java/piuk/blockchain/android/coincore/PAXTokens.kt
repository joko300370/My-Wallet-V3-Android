package piuk.blockchain.android.coincore

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.R
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus

class PAXTokens(
    rxBus: RxBus,
    private val erc20Account: Erc20Account,
    private val exchangeRates: ExchangeRateDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val stringUtils: StringUtils,
    private val custodialWalletManager: CustodialWalletManager
) : AssetTokensBase(rxBus) {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.PAX

    override fun defaultAccount(): Single<AccountReference> =
        Single.just(getDefaultPaxAccountRef())

    private fun getDefaultPaxAccountRef(): AccountReference {
        val paxAddress = erc20Account.ethDataManager.getEthWallet()?.account?.address
            ?: throw Exception("No ether wallet found")

        val label = stringUtils.getString(R.string.pax_default_account_label)

        return AccountReference.Pax(label, paxAddress, "")
    }

    override fun noncustodialBalance(): Single<CryptoValue> =
        erc20Account.getBalance()
            .map { CryptoValue.usdPaxFromMinor(it) }

    override fun custodialBalanceMaybe(): Maybe<CryptoValue> =
        custodialWalletManager.getBalanceForAsset(CryptoCurrency.PAX)

    override fun balance(account: AccountReference): Single<CryptoValue> {
        TODO("not implemented")
    }

    override fun exchangeRate(): Single<FiatValue> =
        exchangeRates.fetchLastPrice(CryptoCurrency.PAX, currencyPrefs.selectedFiatCurrency)

    override fun historicRate(epochWhen: Long): Single<FiatValue> =
        exchangeRates.getHistoricPrice(CryptoCurrency.PAX, currencyPrefs.selectedFiatCurrency, epochWhen)

    // PAX does not support historic prices, so return an empty list
    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        Single.just(emptyList())
}
