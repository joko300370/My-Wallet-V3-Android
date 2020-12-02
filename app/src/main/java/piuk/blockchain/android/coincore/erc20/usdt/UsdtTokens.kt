package piuk.blockchain.android.coincore.erc20.usdt

import com.blockchain.annotations.CommonCode
import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.EligibilityProvider
import com.blockchain.swap.nabu.service.TierService
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.android.coincore.AddressParseError
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.erc20.Erc20Address
import piuk.blockchain.android.coincore.erc20.Erc20TokensBase
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateService
import piuk.blockchain.androidcore.data.exchangerate.PriceSeries
import piuk.blockchain.androidcore.data.exchangerate.TimeSpan
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

internal class UsdtAsset(
    payloadManager: PayloadDataManager,
    usdtAccount: Erc20Account,
    feeDataManager: FeeDataManager,
    custodialManager: CustodialWalletManager,
    exchangeRates: ExchangeRateDataManager,
    historicRates: ExchangeRateService,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    crashLogger: CrashLogger,
    pitLinking: PitLinking,
    tierService: TierService,
    environmentConfig: EnvironmentConfig,
    private val walletPreferences: WalletStatus,
    eligibilityProvider: EligibilityProvider
) : Erc20TokensBase(
    payloadManager,
    usdtAccount,
    feeDataManager,
    custodialManager,
    exchangeRates,
    historicRates,
    currencyPrefs,
    labels,
    pitLinking,
    crashLogger,
    tierService,
    environmentConfig,
    eligibilityProvider
) {
    override val asset: CryptoCurrency = CryptoCurrency.USDT

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.just(listOf(getNonCustodialUsdtAccount()))

    private fun getNonCustodialUsdtAccount(): CryptoAccount {
        val usdtAddress = erc20Account.ethDataManager.getEthWallet()?.account?.address
            ?: throw Exception("No USDT wallet found")

        return UsdtCryptoWalletAccount(
            payloadManager,
            labels.getDefaultNonCustodialWalletLabel(asset),
            usdtAddress,
            erc20Account,
            feeDataManager,
            exchangeRates,
            walletPreferences,
            custodialManager
        )
    }

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        Single.just(emptyList())

    @CommonCode("Exists in EthAsset and PaxAsset")
    override fun parseAddress(address: String): Maybe<ReceiveAddress> =
        Single.just(isValidAddress(address)).flatMapMaybe { isValid ->
            if (isValid) {
                erc20Account.ethDataManager.isContractAddress(address).flatMapMaybe { isContract ->
                    if (isContract) {
                        throw AddressParseError(AddressParseError.Error.ETH_UNEXPECTED_CONTRACT_ADDRESS)
                    } else {
                        Maybe.just(UsdtAddress(address))
                    }
                }
            } else {
                Maybe.empty<ReceiveAddress>()
            }
        }

    override fun isValidAddress(address: String): Boolean =
        FormatsUtil.isValidEthereumAddress(address)
}

internal class UsdtAddress(
    address: String,
    label: String = address
) : Erc20Address(CryptoCurrency.USDT, address, label)
