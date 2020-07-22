package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRates
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.Single
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount

interface BlockchainAccount {
    val label: String

    val balance: Single<Money>

    val activity: Single<ActivitySummaryList>

    val actions: AvailableActions

    val isFunded: Boolean

    val hasTransactions: Boolean

    fun fiatBalance(fiatCurrency: String, exchangeRates: ExchangeRates): Single<Money>
}

interface SingleAccount : BlockchainAccount {
    val receiveAddress: Single<ReceiveAddress>
    val isDefault: Boolean

    val sendState: Single<SendState>
    fun createSendProcessor(address: ReceiveAddress): Single<SendProcessor>
}

enum class SendState {
    CAN_SEND,
    NO_FUNDS,
    NOT_ENOUGH_GAS,
    SEND_IN_FLIGHT,
    NOT_SUPPORTED
}

typealias SingleAccountList = List<SingleAccount>

interface CryptoAccount : SingleAccount {
    val asset: CryptoCurrency
    val feeAsset: CryptoCurrency
}

interface FiatAccount : SingleAccount {
    val fiatCurrency: String
}

interface AccountGroup : BlockchainAccount {
    val accounts: SingleAccountList

    fun includes(account: BlockchainAccount): Boolean
}

internal fun BlockchainAccount.isCustodial(): Boolean =
    this is CustodialTradingAccount

// Stub invalid accounts; use as an initialisers to avoid nulls.
object NullCryptoAccount : CryptoAccount {
    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(NullAddress)

    override val isDefault: Boolean
        get() = false

    override val asset: CryptoCurrency
        get() = CryptoCurrency.BTC

    override fun createSendProcessor(address: ReceiveAddress): Single<SendProcessor> =
        Single.error(NotImplementedError("Dummy Account"))

    override val sendState: Single<SendState>
        get() = Single.just(SendState.NOT_SUPPORTED)

    override val label: String = ""

    override val balance: Single<Money>
        get() = Single.just(CryptoValue.ZeroBtc)

    override val activity: Single<ActivitySummaryList>
        get() = Single.just(emptyList())

    override val actions: AvailableActions = emptySet()
    override val isFunded: Boolean = false
    override val hasTransactions: Boolean = false

    override val feeAsset: CryptoCurrency
        get() = CryptoCurrency.BTC

    override fun fiatBalance(
        fiatCurrency: String,
        exchangeRates: ExchangeRates
    ): Single<Money> =
        Single.just(FiatValue.zero(fiatCurrency))
}

object NullFiatAccount : FiatAccount {
    override val fiatCurrency: String = "NULL"

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(NullAddress)

    override val isDefault: Boolean
        get() = false

    override fun createSendProcessor(address: ReceiveAddress): Single<SendProcessor> =
        Single.error(NotImplementedError("Dummy Account"))

    override val sendState: Single<SendState>
        get() = Single.just(SendState.NOT_SUPPORTED)

    override val label: String = ""

    override val balance: Single<Money>
        get() = Single.just(CryptoValue.ZeroBtc)

    override val activity: Single<ActivitySummaryList>
        get() = Single.just(emptyList())

    override val actions: AvailableActions = emptySet()
    override val isFunded: Boolean = false
    override val hasTransactions: Boolean = false

    override fun fiatBalance(
        fiatCurrency: String,
        exchangeRates: ExchangeRates
    ): Single<Money> =
        Single.just(FiatValue.zero(fiatCurrency))
}
