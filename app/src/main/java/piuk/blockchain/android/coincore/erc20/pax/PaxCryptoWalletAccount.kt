package piuk.blockchain.android.coincore.erc20.pax

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.Single
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.erc20.Erc20NonCustodialAccount
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import java.util.concurrent.atomic.AtomicBoolean

class PaxCryptoWalletAccount(
    payloadManager: PayloadDataManager,
    label: String,
    private val address: String,
    override val erc20Account: Erc20Account,
    fees: FeeDataManager,
    exchangeRates: ExchangeRateDataManager
) : Erc20NonCustodialAccount(
    payloadManager,
    CryptoCurrency.PAX,
    fees,
    label,
    exchangeRates
) {
    private val hasFunds = AtomicBoolean(false)

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val accountBalance: Single<Money>
        get() = erc20Account.getBalance()
            .map { CryptoValue.fromMinor(asset, it) }
            .doOnSuccess {
                hasFunds.set(it > CryptoValue.ZeroPax)
            }.map {
                it
            }

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(
            PaxAddress(address, label)
        )
}
