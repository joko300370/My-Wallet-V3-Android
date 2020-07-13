package piuk.blockchain.android.ui.accounts

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.activityShown
import info.blockchain.balance.CryptoCurrency
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_account_selector_sheet.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.fiat.FiatAsset
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
    private val uiScheduler = AndroidSchedulers.mainThread()

    private val theAdapter: AccountsDelegateAdapter by lazy {
        AccountsDelegateAdapter(
            onAccountClicked = { onAccountSelected(it) }
        )
    }

    private val cryptoCurrency: CryptoCurrency? by lazy {
        arguments?.getSerializable(ARG_CRYPTO_CURRENCY) as? CryptoCurrency
    }

    private fun onAccountSelected(account: BlockchainAccount) {
        analytics.logEvent(activityShown(account.label))
        host.onAccountSelected(account)
        dismiss()
    }

    override fun initControls(view: View) {
        with(view.account_list) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = theAdapter

            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

            val itemList = mutableListOf<BlockchainAccount>()
            theAdapter.items = itemList

            itemList.add(coincore.allWallets)

            CryptoCurrency.activeCurrencies().forEach { cc ->
                disposables += coincore[cc].accountGroup()
                    .observeOn(uiScheduler)
                    .subscribeBy(
                        onSuccess = {
                            itemList.addAll(
                                it.accounts
                                    .filterIsInstance<CryptoAccount>()
                                    .filter { a -> a.hasTransactions }
                            )
                            theAdapter.notifyDataSetChanged()
                        },
                        onError = {
                            onCannotLoadAccounts()
                        }
                    )
            }

            itemList.addAll(coincore.assets.filterIsInstance<FiatAsset>().map {
                it.accounts()
            }.flatten())
        }
    }

    private fun onCannotLoadAccounts() {
        dismiss()
    }

    override fun onSheetHidden() {
        super.onSheetHidden()
        disposables.dispose()
    }

    companion object {
        private const val ARG_CRYPTO_CURRENCY = "crypto"

        fun newInstance(
            cryptoCurrency: CryptoCurrency,
            assetFilter: AssetFilter
        ): AccountSelectSheet =
            AccountSelectSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CRYPTO_CURRENCY, cryptoCurrency)
                }
            }

        // Create sheet for all accounts
        fun newInstance(): AccountSelectSheet =
            AccountSelectSheet()
    }
}
