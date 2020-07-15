package piuk.blockchain.android.ui.transfer.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.blockchain.koin.scopedInject
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_transfer.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.SendState
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.transfer.send.flow.SendFlow
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible
import timber.log.Timber

typealias AccountListFilterFn = (BlockchainAccount) -> Boolean

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
        { account -> (account is CryptoAccount) && account.isFunded }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        flow = SendFlow(
            fragmentManager = childFragmentManager,
            listener = this,
            disposables = disposables
        )

        account_list.onLoadError = ::doOnLoadError
        account_list.onEmptyList = ::doOnEmptyList
        account_list.onAccountSelected = ::doOnAccountSelected

        account_list.initialise(
            Single.just(
                coincore.allWallets
                    .accounts
                    .filter(filterFn)
            ),
            status = ::statusDecorator
        )
    }

    private fun statusDecorator(account: BlockchainAccount): Single<String> =
        if (account is CryptoAccount) {
            account.sendState
                .map { sendState ->
                    when (sendState) {
                        SendState.NO_FUNDS -> getString(R.string.send_state_no_funds)
                        SendState.NOT_SUPPORTED -> getString(R.string.send_state_not_supported)
                        SendState.NOT_ENOUGH_GAS -> getString(R.string.send_state_not_enough_gas)
                        SendState.SEND_IN_FLIGHT -> getString(R.string.send_state_send_in_flight)
                        SendState.CAN_SEND -> ""
                    }
                }
        } else {
            Single.just("")
        }

    private fun doOnEmptyList() {
        account_list.gone()
        send_blurb.gone()
        empty_view.visible()
        button_buy_crypto.setOnClickListener {
            startActivity(SimpleBuyActivity.newInstance(requireContext()))
        }
    }

    private fun doOnLoadError(t: Throwable) {
        ToastCustom.makeText(
            requireContext(),
            getString(R.string.transfer_wallets_load_error),
            ToastCustom.LENGTH_SHORT,
            ToastCustom.TYPE_ERROR
        )
        doOnEmptyList()
    }

    private fun doOnAccountSelected(account: BlockchainAccount) {
        if (account is CryptoAccount) {
            disposables += coincore.requireSecondPassword().observeOn(uiScheduler)
                .subscribeBy(onSuccess = { secondPassword ->
                    flow.startFlow(account, secondPassword)
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
