package piuk.blockchain.android.ui.transfer.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.blockchain.koin.scopedInject
import io.reactivex.Single
import kotlinx.android.synthetic.main.fragment_transfer.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.SendState
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.transfer.send.flow.DialogFlow
import piuk.blockchain.android.ui.transfer.send.flow.SendFlow
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible

typealias AccountListFilterFn = (BlockchainAccount) -> Boolean

class TransferSendFragment :
    Fragment(),
    DialogFlow.FlowHost {

    private val coincore: Coincore by scopedInject()

    private var flow: SendFlow? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_transfer)

    private val filterFn: AccountListFilterFn =
        { account -> (account is CryptoAccount) && account.isFunded }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        account_list.onLoadError = ::doOnLoadError
        account_list.onEmptyList = ::doOnEmptyList
        account_list.onAccountSelected = ::doOnAccountSelected

        account_list.initialise(
            coincore.allWallets().map { it.accounts.filter(filterFn) },
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

    private fun doOnAccountSelected(account: BlockchainAccount) {
        if (account is CryptoAccount) {
            flow = SendFlow(
                fromAccount = account,
                action = AssetAction.NewSend
            ).apply {
                startFlow(
                    fragmentManager = childFragmentManager,
                    host = this@TransferSendFragment
                )
            }
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

    override fun onFlowFinished() {
        flow = null
    }

    companion object {
        fun newInstance() = TransferSendFragment()
    }
}
