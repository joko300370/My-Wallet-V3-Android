package piuk.blockchain.android.ui.transfer.send

import android.os.Bundle
import android.view.View
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
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
            onAccountSelected = ::doOnAccountSelected
        )
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
