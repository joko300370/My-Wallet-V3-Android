package piuk.blockchain.android.ui.customviews.account

import android.view.View
import androidx.annotation.StringRes
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.activityShown
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.dialog_account_selector_sheet.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible

class AccountSelectSheet(
    override val host: Host
) : SlidingModalBottomDialog() {

    interface Host : SlidingModalBottomDialog.Host {
        fun onAccountSelected(account: BlockchainAccount)
        fun onAccountSelectorBack()
    }

    override val layoutResource: Int
        get() = R.layout.dialog_account_selector_sheet

    private val coincore: Coincore by scopedInject()
    private val disposables = CompositeDisposable()

    private var accountFilter: Single<List<BlockchainAccount>> =
        coincore.allWallets()
            .map { listOf(it) + it.accounts }
            .map { it.filter { a -> a.hasTransactions } }

    private var sheetTitle: Int = R.string.select_account_sheet_title
    private var assetFilter: AssetFilter = AssetFilter.All

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

            onAccountSelected = ::doOnAccountSelected
            onEmptyList = ::doOnEmptyList
            onLoadError = ::doOnLoadError

            view.account_list_title.text = getString(sheetTitle)

            when (assetFilter) {
                AssetFilter.All,
                AssetFilter.NonCustodial,
                AssetFilter.Custodial -> {
                    view.account_list_back.gone()
                }
                AssetFilter.Interest -> {
                    showBackArrow(view)
                }
            }

            initialise(accountFilter)
        }
    }

    private fun showBackArrow(view: View) {
        view.account_list_back.visible()
        view.account_list_back.setOnClickListener {
            host.onAccountSelectorBack()
        }
    }

    override fun onSheetHidden() {
        super.onSheetHidden()
        disposables.dispose()
    }

    companion object {
        fun newInstance(host: Host): AccountSelectSheet = AccountSelectSheet(host)

        fun newInstance(
            host: Host,
            accountFilter: Single<List<BlockchainAccount>>,
            @StringRes sheetTitle: Int,
            assetFilter: AssetFilter
        ): AccountSelectSheet =
            AccountSelectSheet(host).apply {
                this.accountFilter = accountFilter
                this.sheetTitle = sheetTitle
                this.assetFilter = assetFilter
            }
    }
}
