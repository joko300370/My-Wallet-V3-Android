package piuk.blockchain.android.coincore.impl

import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.FeeSchedule
import piuk.blockchain.android.coincore.SendTransaction
import piuk.blockchain.android.coincore.ValidationState

class CustodialTransfer(
    override val sendingAccount: CryptoSingleAccount,
    override val address: CryptoAddress
) : SendTransaction {

    init {
        require(sendingAccount.asset == address.asset)
    }

    override var amount: CryptoValue
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}

    override var fees: FeeSchedule = FeeSchedule.None

    override var notes: String
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}

    override fun canExecute(): Single<ValidationState> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun absoluteFee(): Single<CryptoValue> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun execute(secondPassword: String): Single<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}