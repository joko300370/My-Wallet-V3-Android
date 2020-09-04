package piuk.blockchain.android.coincore.eth

import com.blockchain.annotations.CommonCode
import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.service.TierService
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.android.coincore.AddressParseError
import piuk.blockchain.android.coincore.AddressParseError.Error.ETH_UNEXPECTED_CONTRACT_ADDRESS
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.impl.CryptoAssetBase
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

internal class EthAsset(
    payloadManager: PayloadDataManager,
    private val ethDataManager: EthDataManager,
    private val feeDataManager: FeeDataManager,
    custodialManager: CustodialWalletManager,
    exchangeRates: ExchangeRateDataManager,
    historicRates: ChartsDataManager,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    pitLinking: PitLinking,
    crashLogger: CrashLogger,
    tiersService: TierService
) : CryptoAssetBase(
    payloadManager,
    exchangeRates,
    historicRates,
    currencyPrefs,
    labels,
    custodialManager,
    pitLinking,
    crashLogger,
    tiersService
) {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.ETHER

    override fun initToken(): Completable =
        ethDataManager.initEthereumWallet(
            labels.getDefaultNonCustodialWalletLabel(CryptoCurrency.ETHER),
            labels.getDefaultNonCustodialWalletLabel(CryptoCurrency.PAX),
            labels.getDefaultNonCustodialWalletLabel(CryptoCurrency.USDT)
        )

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.just(
            listOf(
                EthCryptoWalletAccount(
                    payloadManager,
                    ethDataManager,
                    feeDataManager,
                    ethDataManager.getEthWallet()?.account ?: throw Exception("No ether wallet found"),
                    exchangeRates
                )
            )
        )

    @CommonCode("Exists in UsdtAsset and PaxAsset")
    override fun parseAddress(address: String): Maybe<ReceiveAddress> =
        Single.just(isValidAddress(address)).flatMapMaybe { isValid ->
            if (isValid) {
                ethDataManager.isContractAddress(address).flatMapMaybe { isContract ->
                    if (isContract) {
                        throw AddressParseError(ETH_UNEXPECTED_CONTRACT_ADDRESS)
                    } else {
                        Maybe.just(EthAddress(address))
                    }
                }
            } else {
                Maybe.empty<ReceiveAddress>()
            }
        }

    private fun isValidAddress(address: String): Boolean =
        FormatsUtil.isValidEthereumAddress(address)
}

internal class EthAddress(
    override val address: String,
    override val label: String = address
) : CryptoAddress {
    override val asset: CryptoCurrency = CryptoCurrency.ETHER
}
