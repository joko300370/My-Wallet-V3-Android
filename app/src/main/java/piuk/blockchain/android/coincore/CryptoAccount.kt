package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
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
    val receiveAddress: Single<String>
    val isDefault: Boolean
    val asset: CryptoCurrency

    fun createPendingSend(address: ReceiveAddress): Single<SendTransaction>
}

interface CryptoAccountGroup : CryptoAccount {
    val accounts: List<CryptoAccount>
}

typealias CryptoSingleAccountList = List<CryptoSingleAccount>

internal fun CryptoAccount.isCustodial(): Boolean =
    this is CustodialTradingAccount