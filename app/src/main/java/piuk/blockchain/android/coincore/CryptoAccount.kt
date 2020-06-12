package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Single
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

interface CryptoAccount {
    val label: String

    val cryptoCurrencies: Set<CryptoCurrency>

    val balance: Single<CryptoValue>

    val activity: Single<ActivitySummaryList>

    val actions: AvailableActions

    val isFunded: Boolean

    val hasTransactions: Boolean

    fun fiatBalance(fiat: String, exchangeRates: ExchangeRateDataManager): Single<FiatValue>

    fun includes(cryptoAccount: CryptoSingleAccount): Boolean
}

interface CryptoSingleAccount : CryptoAccount {
    val receiveAddress: Single<ReceiveAddress>
    val isDefault: Boolean
    val asset: CryptoCurrency

    fun createSendProcessor(address: ReceiveAddress): Single<SendProcessor>
}

interface CryptoAccountGroup : CryptoAccount {
    val accounts: List<CryptoAccount>
}

typealias CryptoSingleAccountList = List<CryptoSingleAccount>

internal fun CryptoAccount.isCustodial(): Boolean =
    this is CustodialTradingAccount

// Stub invalid account; use as an initialiser to avoid nulls.
object NullAccount : CryptoSingleAccount {
    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(NullAddress)

    override val isDefault: Boolean
        get() = false
    override val asset: CryptoCurrency
        get() = CryptoCurrency.BTC

    override fun createSendProcessor(address: ReceiveAddress): Single<SendProcessor> =
        Single.error(NotImplementedError("Dummy Account"))

    override val label: String = ""

    override val cryptoCurrencies: Set<CryptoCurrency>
        get() = setOf(asset)

    override val balance: Single<CryptoValue>
        get() = Single.just(CryptoValue.ZeroBtc)

    override val activity: Single<ActivitySummaryList>
        get() = Single.just(emptyList())

    override val actions: AvailableActions = emptySet()
    override val isFunded: Boolean = false
    override val hasTransactions: Boolean = false

    override fun fiatBalance(
        fiat: String,
        exchangeRates: ExchangeRateDataManager
    ): Single<FiatValue> =
        Single.just(FiatValue.zero(fiat))

    override fun includes(cryptoAccount: CryptoSingleAccount): Boolean {
        return cryptoAccount == this
    }
}
