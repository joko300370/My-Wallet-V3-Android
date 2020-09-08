package piuk.blockchain.android.util

import android.content.Context
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.SendState
import piuk.blockchain.android.ui.customviews.account.AccountDecorator

fun BlockchainAccount.statusDecorator(context: Context): Single<AccountDecorator> =
    if (this is CryptoAccount) {
        this.sendState
            .map { sendState ->
                object : AccountDecorator {
                    override val enabled: Boolean
                        get() = sendState == SendState.CAN_SEND
                    override val status: String
                        get() = when (sendState) {
                            SendState.NO_FUNDS -> context.getString(R.string.send_state_no_funds)
                            SendState.NOT_SUPPORTED -> context.getString(R.string.send_state_not_supported)
                            SendState.FUNDS_LOCKED -> context.getString(R.string.send_state_locked_funds)
                            SendState.NOT_ENOUGH_GAS -> context.getString(R.string.send_state_not_enough_gas)
                            SendState.SEND_IN_FLIGHT -> context.getString(R.string.send_state_send_in_flight)
                            SendState.CAN_SEND -> ""
                        }
                }
            }
    } else {
        Single.just(object : AccountDecorator {
            override val enabled: Boolean
                get() = true
            override val status: String
                get() = ""
        })
    }