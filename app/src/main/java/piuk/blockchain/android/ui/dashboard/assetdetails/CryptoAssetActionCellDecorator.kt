package piuk.blockchain.android.ui.dashboard.assetdetails

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import io.reactivex.Maybe
import io.reactivex.Single
import kotlinx.android.synthetic.main.status_line_info.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.accounts.CellDecorator
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.TxSourceState

class CryptoAssetActionCellDecorator(private val account: CryptoAccount) : CellDecorator {
    override fun view(context: Context): Maybe<View> =
        account.sourceState.flatMapMaybe {
            when (it) {
                TxSourceState.FUNDS_LOCKED ->
                    viewWithText(context.getString(R.string.send_state_locked_funds_1), context)
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
        it == TxSourceState.CAN_TRANSACT
    }
}