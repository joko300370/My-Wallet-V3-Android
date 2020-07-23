package piuk.blockchain.android.coincore.erc20.pax

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.erc20.Erc20Address
import piuk.blockchain.android.coincore.erc20.Erc20TokensBase
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

internal class PaxAsset(
    private val paxAccount: Erc20Account,
    custodialManager: CustodialWalletManager,
    exchangeRates: ExchangeRateDataManager,
    historicRates: ChartsDataManager,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    pitLinking: PitLinking,
    crashLogger: CrashLogger
) : Erc20TokensBase(
    paxAccount,
    custodialManager,
    exchangeRates,
    historicRates,
    currencyPrefs,
    labels,
    pitLinking,
    crashLogger
) {

    override val asset = CryptoCurrency.PAX

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.just(listOf(getNonCustodialPaxAccount()))

    private fun getNonCustodialPaxAccount(): CryptoAccount {
        val paxAddress = paxAccount.ethDataManager.getEthWallet()?.account?.address
            ?: throw Exception("No ether wallet found")

        return PaxCryptoWalletAccount(
            labels.getDefaultNonCustodialWalletLabel(CryptoCurrency.PAX), paxAddress, erc20Account,
            exchangeRates)
    }

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        Single.just(emptyList())

    override fun parseAddress(address: String): CryptoAddress? =
        if (isValidAddress(address)) {
            PaxAddress(address)
        } else {
            null
        }

    private fun isValidAddress(address: String): Boolean =
        FormatsUtil.isValidEthereumAddress(address)
}

internal class PaxAddress(
    address: String,
    label: String = address
) : Erc20Address(CryptoCurrency.PAX, address, label)