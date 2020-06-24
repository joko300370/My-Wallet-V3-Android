package piuk.blockchain.android.ui.transfer.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.koin.scopedInject
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_transfer.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.transfer.send.adapter.AccountsAdapter
import piuk.blockchain.android.ui.transfer.send.flow.SendFlow
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible
import timber.log.Timber

typealias AccountListFilterFn = (CryptoAccount) -> Boolean

class TransferSendFragment :
    Fragment(),
    SlidingModalBottomDialog.Host,
    SendFlow.Listener {

    private val disposables = CompositeDisposable()
    private val coincore: Coincore by scopedInject()
    private val uiScheduler = AndroidSchedulers.mainThread()

    private lateinit var flow: SendFlow

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_transfer)

    private val filterFn: AccountListFilterFn =
        { account -> (account is CryptoSingleAccount) && account.isFunded }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        flow = SendFlow(
            fragmentManager = childFragmentManager,
            listener = this,
            disposables = disposables
        )

        with(account_list) {
            val itemList = mutableListOf<CryptoAccount>()
            val accountAdapter = AccountsAdapter(itemList, ::onAccountSelected)

            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )

            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            )

            adapter = accountAdapter

            disposables += Single.merge(
                CryptoCurrency.activeCurrencies().map { ac ->
                    coincore[ac].accounts()
                }.toList()
            )
            .observeOn(uiScheduler)
            .subscribeBy(
                onNext = {
                    itemList.addAll(it.accounts
                        .filter(filterFn)
                    )
                    accountAdapter.notifyDataSetChanged()
                },
                onError = {
                    ToastCustom.makeText(
                        requireContext(),
                        getString(R.string.transfer_wallets_load_error),
                        ToastCustom.LENGTH_SHORT,
                        ToastCustom.TYPE_ERROR
                    )
                },
                onComplete = {
                    if (itemList.isEmpty()) {
                        showEmptyState()
                    }
                }
            )
        }
    }

    private fun showEmptyState() {
        account_list.gone()
        send_blurb.gone()
        empty_view.visible()
        button_buy_crypto.setOnClickListener {
            startActivity(SimpleBuyActivity.newInstance(requireContext()))
        }
    }

    private fun onAccountSelected(cryptoAccount: CryptoAccount) {
        if (cryptoAccount is CryptoSingleAccount) {
            disposables += coincore.requireSecondPassword().observeOn(uiScheduler)
                .subscribeBy(onSuccess = { secondPassword ->
                    flow.startFlow(cryptoAccount, secondPassword)
                }, onError = {
                    Timber.e("Unable to configure send flow, aborting. e == $it")
                    activity?.finish()
                })
        }
    }

    override fun onSendFlowFinished() {
        disposables.clear()
    }

    companion object {
        fun newInstance() = TransferSendFragment()
    }

    override fun onSheetClosed() {
        flow.finishFlow()
    }
}
