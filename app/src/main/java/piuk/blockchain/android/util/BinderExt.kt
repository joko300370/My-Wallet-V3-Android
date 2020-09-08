package piuk.blockchain.android.util

import android.content.Intent
import android.os.Binder
import android.os.Bundle
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.SendTarget

fun Bundle.putAccount(key: String, account: BlockchainAccount) =
    putBinder(key, ParamBinder(account))

@Suppress("UNCHECKED_CAST")
fun Bundle.getAccount(key: String): BlockchainAccount? =
    (getBinder(key) as? ParamBinder<BlockchainAccount>)?.account

fun Intent.putAccount(key: String, account: BlockchainAccount) =
    this.putExtra(key, Bundle().apply { putBinder(key, ParamBinder(account)) })

@Suppress("UNCHECKED_CAST")
fun Intent.getAccount(key: String): BlockchainAccount? =
    (getBundleExtra(key).getBinder(key) as? ParamBinder<BlockchainAccount>)?.account

fun Intent.putSendTarget(key: String, target: SendTarget) =
    this.putExtra(key, Bundle().apply { putBinder(key, ParamBinder(target)) })

@Suppress("UNCHECKED_CAST")
fun Intent.getSendTarget(key: String): SendTarget? =
    (getBundleExtra(key)?.getBinder(key) as? ParamBinder<SendTarget>)?.account

private class ParamBinder<T>(
    val account: T
) : Binder()