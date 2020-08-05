package piuk.blockchain.android.ui.customviews.account

import android.view.View
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.activityShown
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.dialog_account_selector_sheet.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
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
    private var assetFilter: AssetFilter = AssetFilter.All
    private lateinit var cryptoCurrency: CryptoCurrency

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

            val accounts = when (assetFilter) {
                AssetFilter.All,
                AssetFilter.NonCustodial,
                AssetFilter.Custodial -> {
                    showAllUi(view)
                    defaultAllWallets()
                }
                AssetFilter.Interest -> {
                    showDepositUi(view)
                    allowedInterestWallets()
                }
            }

            initialise(accounts)
        }
    }

    private fun showAllUi(view: View) {
        view.account_list_title.text = getString(R.string.select_account_sheet_title)
    }

    private fun showDepositUi(view: View) {
        view.account_list_title.text = getString(R.string.select_deposit_source_title)
        view.account_list_back.visible()
        view.account_list_back.setOnClickListener {
            host.onAccountSelectorBack()
        }
    }

    private fun allowedInterestWallets() : Single<List<BlockchainAccount>> =
        coincore[cryptoCurrency].accountGroup(AssetFilter.NonCustodial) // TODO add custodial here later
            .map { it.accounts }
            .map { it.filter { a -> a.isFunded } }

    private fun defaultAllWallets() =
        coincore.allWallets()
            .map { listOf(it) + it.accounts }
            .map { it.filter { a -> a.hasTransactions } }

    override fun onSheetHidden() {
        super.onSheetHidden()
        disposables.dispose()
    }

    companion object {
        fun newInstance(host: Host): AccountSelectSheet =
            AccountSelectSheet(host)

        fun newInstance(assetFilter: AssetFilter,
                        cryptoCurrency: CryptoCurrency,
                        host: Host): AccountSelectSheet =
            AccountSelectSheet(host).apply {
                this.assetFilter = assetFilter
                this.cryptoCurrency = cryptoCurrency
            }
    }
}
