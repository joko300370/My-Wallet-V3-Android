package piuk.blockchain.android.ui.transfer.receive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.blockchain.koin.scopedInject
import io.reactivex.Single
import kotlinx.android.synthetic.main.fragment_transfer_receive.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.SendState
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible

typealias AccountListFilterFn = (BlockchainAccount) -> Boolean

class TransferReceiveFragment : Fragment() {

    private val coincore: Coincore by scopedInject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_transfer_receive, container, false)

    private val filterFn: AccountListFilterFn =
        { account -> (account is CryptoAccount) && account.isFunded }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        receive_account_list.onLoadError = ::doOnLoadError
        receive_account_list.onEmptyList = ::doOnEmptyList
        receive_account_list.onAccountSelected = ::doOnAccountSelected

        receive_account_list.initialise(
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
        receive_account_list.gone()
        receive_blurb.gone()
        receive_empty_view.visible()
        receive_button_buy_crypto.setOnClickListener {
            startActivity(SimpleBuyActivity.newInstance(requireContext()))
        }
    }

    private fun doOnAccountSelected(account: BlockchainAccount) {
        if (account is CryptoAccount) {
            Toast.makeText(requireContext(), "TODO: Implement on account selected",
                Toast.LENGTH_SHORT).show()
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

    companion object {
        fun newInstance() = TransferReceiveFragment()
    }
}
