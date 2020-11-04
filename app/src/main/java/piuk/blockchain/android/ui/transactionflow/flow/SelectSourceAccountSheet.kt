package piuk.blockchain.android.ui.transactionflow.flow

import android.view.View
import io.reactivex.Single
import kotlinx.android.synthetic.main.dialog_account_selector_sheet.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf

class SelectSourceAccountSheet(host: SlidingModalBottomDialog.Host) : TransactionFlowSheet(host) {

    private val customiser: TransactionFlowCustomiser by inject()

    override fun render(newState: TransactionState) {
        with(dialogView) {
            account_list.initialise(
                source = Single.just(newState.availableSources.map { it }),
                status = customiser.sourceAccountSelectionStatusDecorator(newState)
            )
            account_list_title.text = customiser.selectSourceAccountTitle(newState)
            account_list_subtitle.text = customiser.selectSourceAccountSubtitle(newState)
            account_list_subtitle.visible()
            account_list_back.visibleIf { newState.canGoBack }
            account_list.onEmptyList = {
                account_list_empty.visible()
            }
        }
    }

    override val layoutResource: Int
        get() = R.layout.dialog_account_selector_sheet

    override fun initControls(view: View) {
        view.apply {
            account_list.onAccountSelected = {
                require(it is CryptoAccount)
                model.process(TransactionIntent.SourceAccountSelected(it))
            }
            account_list_back.setOnClickListener {
                model.process(TransactionIntent.ReturnToPreviousStep)
            }
        }
    }
}