package piuk.blockchain.android.ui.transfer.send

import android.os.Bundle
import android.view.View
import piuk.blockchain.android.R
import piuk.blockchain.android.accounts.CellDecorator
import piuk.blockchain.android.accounts.DefaultCellDecorator
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.transactionflow.DialogFlow
import piuk.blockchain.android.ui.transactionflow.TransactionFlow
import piuk.blockchain.android.ui.transfer.AccountListFilterFn
import piuk.blockchain.android.ui.transfer.AccountSelectorFragment
import piuk.blockchain.android.ui.transfer.send.activity.SendActivity

class TransferSendFragment :
    AccountSelectorFragment(),
    DialogFlow.FlowHost {

    private var flow: TransactionFlow? = null

    override val filterFn: AccountListFilterFn = { account ->
        (account is CryptoAccount) &&
                account.isFunded &&
                account.actions.intersect(
                    listOf(AssetAction.NewSend, AssetAction.Send)
                ).isNotEmpty()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        renderList()
    }

    private fun renderList() {
        setEmptyStateDetails(R.string.transfer_wallets_empty_title,
            R.string.transfer_wallets_empty_details, R.string.transfer_wallet_buy_crypto) {
            startActivity(SimpleBuyActivity.newInstance(requireContext()))
        }

        initialiseAccountSelectorWithHeader(
            statusDecorator = ::statusDecorator,
            onAccountSelected = ::doOnAccountSelected,
            title = R.string.transfer_send_crypto_title,
            label = R.string.transfer_send_crypto_label,
            icon = R.drawable.ic_send_blue_circle
        )
    }

    private fun statusDecorator(account: BlockchainAccount): CellDecorator =
        if (account is CryptoAccount) {
            SendCellDecorator(account)
        } else {
            DefaultCellDecorator()
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
        flow = TransactionFlow(
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
        refreshItems()
    }

    companion object {
        fun newInstance() = TransferSendFragment()
    }
}
