package piuk.blockchain.android.coincore

import info.blockchain.balance.AccountReference
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.stx.STXAccount
import io.reactivex.Single

class STXTokens(
    private val payloadManager: PayloadManager
) : Coin {

    override fun defaultAccount(): Single<AccountReference> {
        val hdWallets = payloadManager.payload?.hdWallets
            ?: return Single.error(IllegalStateException("Wallet not available"))
        return Single.just(hdWallets[0].stxAccount.toAccountReference())
    }
}

private fun STXAccount.toAccountReference(): AccountReference.Stx =
    AccountReference.Stx(
        _label = "STX Account",
        address = bitcoinSerializedBase58Address
    )
