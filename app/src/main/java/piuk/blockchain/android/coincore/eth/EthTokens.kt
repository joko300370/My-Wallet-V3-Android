package piuk.blockchain.android.coincore.eth

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AddressList
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.CryptoSingleAccountList
import piuk.blockchain.android.coincore.impl.AssetTokensBase
import piuk.blockchain.android.coincore.isCustodial
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus

internal class EthTokens(
    private val ethDataManager: EthDataManager,
    private val stringUtils: StringUtils,
    custodialManager: CustodialWalletManager,
    exchangeRates: ExchangeRateDataManager,
    historicRates: ChartsDataManager,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    pitLinking: PitLinking,
    crashLogger: CrashLogger,
    rxBus: RxBus
) : AssetTokensBase(
    exchangeRates,
    historicRates,
    currencyPrefs,
    labels,
    custodialManager,
    pitLinking,
    crashLogger,
    rxBus
) {

    override val asset: CryptoCurrency
        get() = CryptoCurrency.ETHER

    override fun initToken(): Completable =
        ethDataManager.initEthereumWallet(
            stringUtils.getString(R.string.eth_default_account_label),
            stringUtils.getString(R.string.pax_default_account_label_1)
        )

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<CryptoSingleAccountList> =
        Single.just(
            listOf(
                EthCryptoWalletAccount(
                    ethDataManager,
                    ethDataManager.getEthWallet()?.account ?: throw Exception("No ether wallet found"),
                    exchangeRates
                )
            )
        )

    override fun onLogoutSignal(event: AuthEvent) {
        if (event != AuthEvent.LOGIN) {
            ethDataManager.clearEthAccountDetails()
        }
    }

    override fun canTransferTo(account: CryptoSingleAccount): Single<AddressList> {
        require(account.cryptoCurrencies.contains(CryptoCurrency.ETHER))

        // ETH Only supports transfer between non-custodial and exchange account at this time
        if (account.isCustodial()) {
            return Single.just(emptyList())
        }

        return getPitLinkingAddress()
            .map { listOf(it) }
            .toSingle(emptyList())
    }

    override fun isValidAddress(address: String): Boolean =
        FormatsUtil.isValidEthereumAddress(address)
}
