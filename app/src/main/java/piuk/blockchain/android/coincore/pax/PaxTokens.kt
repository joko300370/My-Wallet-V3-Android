package piuk.blockchain.android.coincore.pax

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.CryptoSingleAccountList
import piuk.blockchain.android.coincore.impl.AssetTokensBase
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus

internal class PaxTokens(
    private val paxAccount: Erc20Account,
    private val stringUtils: StringUtils,
    custodialManager: CustodialWalletManager,
    exchangeRates: ExchangeRateDataManager,
    historicRates: ChartsDataManager,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    crashLogger: CrashLogger,
    rxBus: RxBus
) : AssetTokensBase(
    exchangeRates,
    historicRates,
    currencyPrefs,
    labels,
    custodialManager,
    crashLogger,
    rxBus
) {

    override val asset = CryptoCurrency.PAX

    override fun initToken(): Completable =
        paxAccount.fetchErc20Address().ignoreElements()

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<CryptoSingleAccountList> =
        Single.just(listOf(getNonCustodialPaxAccount()))

    private fun getNonCustodialPaxAccount(): CryptoSingleAccount {
        val paxAddress = paxAccount.ethDataManager.getEthWallet()?.account?.address
            ?: throw Exception("No ether wallet found")

        val label = stringUtils.getString(R.string.pax_default_account_label_1)

        return PaxCryptoWalletAccount(label, paxAddress, paxAccount, exchangeRates)
    }

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        Single.just(emptyList())
}