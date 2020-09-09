package piuk.blockchain.android.ui.transfer.send

import android.os.Bundle
import android.view.View
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.SendState
import piuk.blockchain.android.ui.customviews.account.AccountDecorator
import piuk.blockchain.android.ui.transfer.AccountListFilterFn
import piuk.blockchain.android.ui.transfer.AccountSelectorFragment
import piuk.blockchain.android.ui.transfer.send.activity.SendActivity
import piuk.blockchain.android.ui.transfer.send.flow.DialogFlow
import piuk.blockchain.android.ui.transfer.send.flow.SendFlow

class TransferSendFragment :
    AccountSelectorFragment(),
    DialogFlow.FlowHost {

    private var flow: SendFlow? = null

    override val filterFn: AccountListFilterFn = { account ->
        (account is CryptoAccount) &&
            account.isFunded &&
            account.actions.intersect(
                listOf(AssetAction.NewSend, AssetAction.Send)
            ).isNotEmpty()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHeaderDetails(
            R.string.transfer_send_crypto_title,
            R.string.transfer_send_crypto_label,
            R.drawable.ic_send_blue_circle
        )

        initialiseAccountSelector(
            statusDecorator = ::statusDecorator,
            onAccountSelected = ::doOnAccountSelected
        )
    }

    private fun statusDecorator(account: BlockchainAccount): Single<AccountDecorator> =
        if (account is CryptoAccount) {
            account.sendState
                .map { sendState ->
                    object : AccountDecorator {
                        override val enabled: Boolean
                            get() = sendState == SendState.CAN_SEND
                        override val status: String
                            get() = when (sendState) {
                                SendState.NO_FUNDS -> getString(R.string.send_state_no_funds)
                                SendState.NOT_SUPPORTED -> getString(
                                    R.string.send_state_not_supported)
                                SendState.FUNDS_LOCKED -> getString(
                                    R.string.send_state_locked_funds)
                                SendState.NOT_ENOUGH_GAS -> getString(
                                    R.string.send_state_not_enough_gas)
                                SendState.SEND_IN_FLIGHT -> getString(
                                    R.string.send_state_send_in_flight)
                                else -> ""
                            }
                    }
                }
        } else {
            Single.just(object : AccountDecorator {
                override val enabled: Boolean
                    get() = true
                override val status: String
                    get() = ""
            })
        }

    private fun doOnAccountSelected(account: BlockchainAccount) {
        require(account is CryptoAccount)

        if (account.actions.contains(AssetAction.NewSend)) {
            startNewSend(account)
        } else {
            startOldSend(account)
        }
    }

    private fun startNewSend(fromAccount: CryptoAccount) {
        flow = SendFlow(
            sourceAccount = fromAccount,
            action = AssetAction.NewSend
        ).apply {
            startFlow(
                fragmentManager = childFragmentManager,
                host = this@TransferSendFragment
            )
        }
    }

    private fun startOldSend(account: CryptoAccount) {
        SendActivity.start(requireContext(), account)
    }

    override fun onFlowFinished() {
        flow = null
    }

    companion object {
        fun newInstance() = TransferSendFragment()
    }
}
