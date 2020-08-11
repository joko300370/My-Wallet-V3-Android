package piuk.blockchain.android.coincore.impl

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionProcessor
import piuk.blockchain.android.coincore.TransactionValidationError
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

class SendError(msg: String) : Exception(msg)

abstract class TransactionProcessorBase(
    protected val exchangeRates: ExchangeRateDataManager
) : TransactionProcessor {

    protected abstract var pendingTx: PendingTx

    override fun createPendingTx(): Single<PendingTx> =
        Single.just(pendingTx)

    final override fun setOption(newOption: TxOptionValue): Single<PendingTx> {
        val pendingTx = this.pendingTx
        return if (pendingTx.hasOption(newOption.option)) {
            val opts = pendingTx.options.toMutableSet()
            val old = opts.find { it.option == newOption.option }
            opts.remove(old)
            opts.add(newOption)
            this.pendingTx = pendingTx.copy(options = opts)
            Single.just(this.pendingTx)
        } else {
            Single.error(TransactionValidationError(TransactionValidationError.UNSUPPORTED_OPTION))
        }
    }

    final override fun userExchangeRate(userFiat: String): Observable<ExchangeRate> =
        Observable.just(
            exchangeRates.getLastPrice(sendingAccount.asset, userFiat)
        ).map { rate ->
            ExchangeRate.CryptoToFiat(
                sendingAccount.asset,
                userFiat,
                rate.toBigDecimal()
            )
        }

    final override fun targetExchangeRate(): Observable<ExchangeRate> =
        Observable.empty()
}

abstract class OnChainSendProcessorBase(
    exchangeRates: ExchangeRateDataManager,
    final override val sendingAccount: CryptoAccount,
    final override val sendTarget: CryptoAddress,
    private val requireSecondPassword: Boolean
) : TransactionProcessorBase(exchangeRates) {

    protected abstract val asset: CryptoCurrency

    init {
        require(sendTarget.address.isNotEmpty())
        require(sendingAccount.asset == sendTarget.asset)
    }

    final override fun execute(secondPassword: String): Completable =
        if (requireSecondPassword && secondPassword.isEmpty()) {
            Completable.error(SendError("Second password not supplied"))
        } else {
            executeTransaction(secondPassword)
                .ignoreElement()
        }

    protected abstract fun executeTransaction(
        secondPassword: String
    ): Single<String>
}
