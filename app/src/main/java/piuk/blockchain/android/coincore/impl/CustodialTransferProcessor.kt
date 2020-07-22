package piuk.blockchain.android.coincore.impl

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingSendTx
import piuk.blockchain.android.coincore.SendProcessor
import piuk.blockchain.android.coincore.SendValidationError

class CustodialTransferProcessor(
    override val sendingAccount: CryptoAccount,
    override val address: CryptoAddress,
    private val walletManager: CustodialWalletManager
) : SendProcessor {

    init {
        require(sendingAccount.asset == address.asset)
    }

    override val feeOptions = setOf(FeeLevel.None)

    override fun availableBalance(pendingTx: PendingSendTx): Single<Money> =
        sendingAccount.balance

    override fun absoluteFee(pendingTx: PendingSendTx): Single<Money> =
        Single.just(CryptoValue.zero(sendingAccount.asset))

    override fun validate(pendingTx: PendingSendTx): Completable =
        availableBalance(pendingTx)
            .flatMapCompletable { max ->
                if (max >= pendingTx.amount) {
                    Completable.complete()
                } else {
                    Completable.error(
                        SendValidationError(SendValidationError.INSUFFICIENT_FUNDS)
                    )
                }
            }

    override fun execute(pendingTx: PendingSendTx, secondPassword: String): Completable =
        walletManager.transferFundsToWallet(pendingTx.amount as CryptoValue, address.address)

    override fun isNoteSupported(): Single<Boolean> =
        when (sendingAccount.asset) {
            CryptoCurrency.BTC,
            CryptoCurrency.ETHER,
            CryptoCurrency.USDT,
            CryptoCurrency.PAX -> Single.just(true)
            else -> Single.just(false)
        }
}
