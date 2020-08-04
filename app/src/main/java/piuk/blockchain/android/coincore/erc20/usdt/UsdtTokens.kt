package piuk.blockchain.android.coincore.erc20.usdt

import com.blockchain.annotations.CommonCode
import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.coincore.AddressParseError
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.erc20.Erc20Address
import piuk.blockchain.android.coincore.erc20.Erc20TokensBase
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager

internal class UsdtAsset(
    usdtAccount: Erc20Account,
    feeDataManager: FeeDataManager,
    custodialManager: CustodialWalletManager,
    exchangeRates: ExchangeRateDataManager,
    historicRates: ChartsDataManager,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    crashLogger: CrashLogger,
    pitLinking: PitLinking
) : Erc20TokensBase(
    usdtAccount,
    feeDataManager,
    custodialManager,
    exchangeRates,
    historicRates,
    currencyPrefs,
    labels,
    pitLinking,
    crashLogger
) {
    override val asset: CryptoCurrency = CryptoCurrency.USDT

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.just(listOf(getNonCustodialUsdtAccount()))

    private fun getNonCustodialUsdtAccount(): CryptoAccount {
        val usdtAddress = erc20Account.ethDataManager.getEthWallet()?.account?.address
            ?: throw Exception("No USDT wallet found")

        return UsdtCryptoWalletAccount(
            labels.getDefaultNonCustodialWalletLabel(asset),
            usdtAddress,
            erc20Account,
            feeDataManager,
            exchangeRates
        )
    }

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        Single.just(emptyList())

    @CommonCode("Exists in EthAsset and PaxAsset")
    override fun parseAddress(address: String): Maybe<ReceiveAddress> =
        Singles.zip(
            Single.just(isValidAddress(address)),
            erc20Account.ethDataManager.isContractAddress(address)
        ) { isValid, isContract ->
            when {
                isContract -> throw AddressParseError(AddressParseError.Error.ETH_UNEXPECTED_CONTRACT_ADDRESS)
                isValid -> address
                else -> ""
            }
        }.flatMapMaybe { a ->
            if (a.isEmpty()) {
                Maybe.empty<ReceiveAddress>()
            } else {
                Maybe.just(UsdtAddress(address))
            }
        }

    private fun isValidAddress(address: String): Boolean =
        FormatsUtil.isValidEthereumAddress(address)
}

internal class UsdtAddress(
    address: String,
    label: String = address
) : Erc20Address(CryptoCurrency.USDT, address, label)
