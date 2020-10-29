package piuk.blockchain.android.ui.transactionflow.flow

import android.view.View
import io.reactivex.Single
import kotlinx.android.synthetic.main.dialog_account_selector_sheet.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.accounts.CellDecorator
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.swap.SwapAccountSelectSheetFeeDecorator
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.androidcoreui.utils.extensions.visible

class SelectTargetAccountSheet(host: SlidingModalBottomDialog.Host) : TransactionFlowSheet(host) {

    private val customiser: TransactionFlowCustomiser by inject()

    override fun render(newState: TransactionState) {
        with(dialogView) {
            account_list.initialise(
                source = Single.just(newState.availableTargets.map { it as SingleAccount }),
                status = ::statusDecorator
            )
            account_list_title.text = customiser.selectTargetAccountTitle(newState)
            account_list_subtitle.text = customiser.selectTargetAccountDescription(newState)
            account_list_subtitle.visible()
        }
    }

    private fun statusDecorator(account: BlockchainAccount): CellDecorator =
        SwapAccountSelectSheetFeeDecorator(account)

    override val layoutResource: Int
        get() = R.layout.dialog_account_selector_sheet

    override fun initControls(view: View) {
        view.apply {
            account_list.onAccountSelected = {
                require(it is SingleAccount)
                model.process(TransactionIntent.TargetAccountSelected(it))
            }
            account_list_back.visible()
            account_list_back.setOnClickListener {
                model.process(TransactionIntent.ReturnToPreviousStep)
            }
        }
    }
}