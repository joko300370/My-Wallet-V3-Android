package piuk.blockchain.android.ui.transfer.send

import android.os.Bundle
import android.view.View
import com.blockchain.koin.scopedInject
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.SendState
import piuk.blockchain.android.ui.transfer.AccountSelectorFragment
import piuk.blockchain.android.ui.transfer.send.flow.DialogFlow
import piuk.blockchain.android.ui.transfer.send.flow.SendFlow

class TransferSendFragment :
    AccountSelectorFragment(),
    DialogFlow.FlowHost {

    private val coincore: Coincore by scopedInject()
    private var flow: SendFlow? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setBlurbText(getString(R.string.transfer_send_crypto))
        initialiseAccountSelector(
            statusDecorator = ::statusDecorator,
            onAccountSelected = ::doOnAccountSelected
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

    override fun onFlowFinished() {
        flow = null
    }

    companion object {
        fun newInstance() = TransferSendFragment()
    }
}
