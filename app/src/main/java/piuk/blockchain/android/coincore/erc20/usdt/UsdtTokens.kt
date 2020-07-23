package piuk.blockchain.android.coincore.erc20.usdt

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.erc20.Erc20Address
import piuk.blockchain.android.coincore.erc20.Erc20TokensBase
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

internal class UsdtAsset(
    override val asset: CryptoCurrency = CryptoCurrency.USDT,
    usdtAccount: Erc20Account,
    custodialManager: CustodialWalletManager,
    exchangeRates: ExchangeRateDataManager,
    historicRates: ChartsDataManager,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    crashLogger: CrashLogger,
    pitLinking: PitLinking
) : Erc20TokensBase(
    usdtAccount,
    custodialManager,
    exchangeRates,
    historicRates,
    currencyPrefs,
    labels,
    pitLinking,
    crashLogger
) {
    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.just(listOf(getNonCustodialUsdtAccount()))

    private fun getNonCustodialUsdtAccount(): CryptoAccount {
        val usdtAddress = erc20Account.ethDataManager.getEthWallet()?.account?.address
            ?: throw Exception("No USDT wallet found")

        return UsdtCryptoWalletAccount(labels.getDefaultNonCustodialWalletLabel(asset), usdtAddress,
            erc20Account, exchangeRates)
    }

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        Single.just(emptyList())

    override fun parseAddress(address: String): CryptoAddress? =
        if (isValidAddress(address)) {
            UsdtAddress(address)
        } else {
            null
        }

    private fun isValidAddress(address: String): Boolean =
        FormatsUtil.isValidEthereumAddress(address)
}

internal class UsdtAddress(
    address: String,
    label: String = address
) : Erc20Address(CryptoCurrency.PAX, address, label)
