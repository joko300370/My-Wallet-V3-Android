package piuk.blockchain.android.ui.transactionflow.flow

import android.view.View
import io.reactivex.Single
import kotlinx.android.synthetic.main.select_target_account_sheet_layout.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState

class SelectTargetAccountSheet(host: SlidingModalBottomDialog.Host) : TransactionFlowSheet(host) {

    private val customiser: TransactionFlowCustomiser by inject()

    override fun render(newState: TransactionState) {
        with(dialogView) {
            accounts_list.initialise(
                source = Single.just(newState.availableTargets.map { it as SingleAccount })
            )
            title.text = customiser.selectTargetAccountTitle(newState)
            description.text = customiser.selectTargetAccountDescription(newState)
        }
    }

    override val layoutResource: Int
        get() = R.layout.select_target_account_sheet_layout

    override fun initControls(view: View) {
        view.accounts_list.onAccountSelected = {
            require(it is SingleAccount)
            model.process(TransactionIntent.TargetAccountSelected(it))
        }
    }
}