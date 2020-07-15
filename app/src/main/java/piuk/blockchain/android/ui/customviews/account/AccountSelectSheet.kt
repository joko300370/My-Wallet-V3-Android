package piuk.blockchain.android.ui.customviews.account

import android.view.View
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.activityShown
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.dialog_account_selector_sheet.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class AccountSelectSheet : SlidingModalBottomDialog() {

    interface Host : SlidingModalBottomDialog.Host {
        fun onAccountSelected(account: BlockchainAccount)
    }

    override val host: Host by lazy {
        super.host as? Host
            ?: throw IllegalStateException("Host fragment is not a AccountSelectSheet.Host")
    }

    override val layoutResource: Int
        get() = R.layout.dialog_account_selector_sheet

    private val coincore: Coincore by scopedInject()
    private val disposables = CompositeDisposable()

    private fun doOnAccountSelected(account: BlockchainAccount) {
        analytics.logEvent(activityShown(account.label))
        host.onAccountSelected(account)
        dismiss()
    }

    private fun doOnLoadError(t: Throwable) {
        dismiss()
    }

    private fun doOnEmptyList() {
        dismiss()
    }

    override fun initControls(view: View) {
        with(view.account_list) {

            val itemList = mutableListOf<BlockchainAccount>()

            itemList.add(coincore.allWallets)
            itemList.addAll(coincore.allWallets.accounts)

            onAccountSelected = ::doOnAccountSelected
            onEmptyList = ::doOnEmptyList
            onLoadError = ::doOnLoadError

            val allAccounts = listOf(coincore.allWallets) + coincore.allWallets.accounts
            initialise(
                Single.just(allAccounts.filter { a -> a.hasTransactions })
            )
        }
    }

    override fun onSheetHidden() {
        super.onSheetHidden()
        disposables.dispose()
    }

    companion object {
        fun newInstance(): AccountSelectSheet =
            AccountSelectSheet()
    }
}
