package piuk.blockchain.android.ui.swapold

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import io.reactivex.Maybe
import piuk.blockchain.android.R
import piuk.blockchain.android.accounts.CellDecorator
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.TradingAccount

class SwapSourceAccountSelectSheetDecorator(private val account: BlockchainAccount) : CellDecorator {

    override fun view(context: Context): Maybe<View> =
        if (account is TradingAccount) {
            Maybe.just(lowFeesView(context))
        } else
            Maybe.empty()

    private fun lowFeesView(context: Context) =
        LayoutInflater.from(context).inflate(
            R.layout.low_fees_layout,
            null,
            false
        )
}