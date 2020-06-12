package piuk.blockchain.android.coincore.impl

import info.blockchain.balance.CryptoValue
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingSendTx
import piuk.blockchain.android.coincore.SendProcessor

class CustodialTransfer(
    override val sendingAccount: CryptoSingleAccount,
    override val address: CryptoAddress
) : SendProcessor {

    init {
        require(sendingAccount.asset == address.asset)
    }

    override val feeOptions: Set<FeeLevel>
        get() = TODO("not implemented")

    override fun availableBalance(pendingTx: PendingSendTx): Single<CryptoValue> =
        TODO("not implemented")

    override fun absoluteFee(pendingTx: PendingSendTx): Single<CryptoValue> =
        TODO("not implemented")

    override fun validate(pendingTx: PendingSendTx): Completable =
        TODO("not implemented")

    override fun execute(pendingTx: PendingSendTx, secondPassword: String): Single<String> =
        TODO("not implemented")
}
