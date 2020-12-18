package piuk.blockchain.android.ui.dashboard.assetdetails

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import io.reactivex.Maybe
import io.reactivex.Single
import kotlinx.android.synthetic.main.status_line_info.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.TxSourceState
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount
import piuk.blockchain.android.ui.customviews.account.CellDecorator

class CryptoAssetActionCellDecorator(private val account: CryptoAccount, private val action: AssetAction) :
    CellDecorator {
    override fun view(context: Context): Maybe<View> =
        account.sourceState.flatMapMaybe {
            when (it) {
                TxSourceState.FUNDS_LOCKED -> {
                    if (account.canExecuteWithLockedFunds(action)) {
                        Maybe.empty()
                    } else {
                        viewWithText(context.getString(R.string.send_state_locked_funds_1), context)
                    }
                }
                TxSourceState.TRANSACTION_IN_FLIGHT ->
                    viewWithText(context.getString(R.string.send_state_send_in_flight), context)
                else -> Maybe.empty()
            }
        }

    private fun viewWithText(text: String, context: Context): Maybe<View> {
        val view = LayoutInflater.from(context).inflate(
            R.layout.status_line_info,
            null,
            false
        )
        view.message.text = text
        return Maybe.just(view)
    }

    override fun isEnabled(): Single<Boolean> = account.sourceState.map {
        account.canExecuteWithLockedFunds(action) || it == TxSourceState.CAN_TRANSACT
    }

    private fun CryptoAccount.canExecuteWithLockedFunds(action: AssetAction) =
        this is CustodialTradingAccount && (action == AssetAction.Swap || action == AssetAction.Sell)
}