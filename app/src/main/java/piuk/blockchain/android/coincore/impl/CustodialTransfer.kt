package piuk.blockchain.android.coincore.impl

import info.blockchain.balance.CryptoValue
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.SendTransaction

class CustodialTransfer(
    override val sendingAccount: CryptoSingleAccount,
    override val address: CryptoAddress
) : SendTransaction {

    init {
        require(sendingAccount.asset == address.asset)
    }

    override var amount: CryptoValue
        get() = TODO("not implemented")
        set(value) {}

    override var feeLevel: FeeLevel
        get() = TODO("not implemented")
        set(value) {}

    override var notes: String
        get() = TODO("not implemented")
        set(value) {}

    override val feeOptions: Set<FeeLevel>
        get() = TODO("not implemented")

    override val availableBalance: Single<CryptoValue>
        get() = TODO("not implemented")

    override val absoluteFee: Single<CryptoValue>
        get() = TODO("not implemented")

    override fun validate(): Completable {
        TODO("not implemented")
    }

    override fun execute(secondPassword: String): Single<String> {
        TODO("not implemented")
    }
}
