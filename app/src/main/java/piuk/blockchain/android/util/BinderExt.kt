package piuk.blockchain.android.util

import android.os.Binder
import android.os.Bundle
import piuk.blockchain.android.coincore.BlockchainAccount

fun Bundle.putAccount(paramName: String, account: BlockchainAccount) =
    putBinder(paramName, AccountBinder(account))

fun Bundle.getAccount(paramName: String): BlockchainAccount? =
    (getBinder(paramName) as? AccountBinder)?.account

private class AccountBinder(
    val account: BlockchainAccount
) : Binder()