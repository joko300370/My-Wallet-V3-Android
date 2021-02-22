package piuk.blockchain.android.coincore.fiat

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.models.responses.interest.DisabledReason
import info.blockchain.balance.ExchangeRates
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.BankAccount
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.TxSourceState

class LinkedBankAccount(
    override val label: String,
    val accountNumber: String,
    val accountId: String,
    val accountType: String,
    val currency: String,
    val custodialWalletManager: CustodialWalletManager
) : FiatAccount, BankAccount {

    override val accountBalance: Single<Money>
        get() = Single.just(FiatValue.fromMinor(currency, 0L))

    override val fiatCurrency: String
        get() = currency

    override val pendingBalance: Single<Money>
        get() = Single.just(FiatValue.fromMinor(currency, 0L))

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(BankAccountAddress(accountId, label))

    override val isDefault: Boolean
        get() = false

    override val actionableBalance: Single<Money>
        get() = Single.just(FiatValue.fromMinor(currency, 0L))

    override val sourceState: Single<TxSourceState>
        get() = Single.just(TxSourceState.CAN_TRANSACT)

    override val activity: Single<ActivitySummaryList>
        get() = Single.just(emptyList())

    override val actions: Single<AvailableActions>
        get() = Single.just(emptySet())

    override val isFunded: Boolean
        get() = false

    override val hasTransactions: Boolean
        get() = false

    override val isEnabled: Single<Boolean>
        get() = Single.just(true)

    override val disabledReason: Single<DisabledReason>
        get() = Single.just(DisabledReason.NONE)

    override fun canWithdrawFunds(): Single<Boolean> = Single.just(false)

    override fun fiatBalance(fiatCurrency: String, exchangeRates: ExchangeRates): Single<Money> =
        Single.just(FiatValue.zero(fiatCurrency))

    internal class BankAccountAddress(
        override val address: String,
        override val label: String = address
    ) : ReceiveAddress
}