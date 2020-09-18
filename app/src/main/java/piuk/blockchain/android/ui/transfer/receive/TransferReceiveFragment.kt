package piuk.blockchain.android.ui.transfer.receive

import android.os.Bundle
import android.view.View
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.ui.customviews.account.AccountDecorator
import piuk.blockchain.android.ui.transfer.AccountListFilterFn
import piuk.blockchain.android.ui.transfer.AccountSelectorFragment
import piuk.blockchain.android.ui.transfer.receive.activity.ReceiveActivity

class TransferReceiveFragment : AccountSelectorFragment() {

    override val filterFn: AccountListFilterFn = { account ->
        (account is CryptoAccount) && account.actions.contains(AssetAction.Receive)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        setHeaderDetails(
//            R.string.transfer_receive_crypto_title,
//            R.string.transfer_receive_crypto_label,
//            R.drawable.ic_receive_blue_circle
//        )

        setEmptyStateDetails(R.string.transfer_receive_empty_title,
            R.string.transfer_receive_empty_details, R.string.transfer_receive_empty_cta) {
            refreshItems()
        }

        initialiseAccountSelectorWithHeader(
            statusDecorator = {
                Single.just(object : AccountDecorator {
                    override val enabled: Boolean
                        get() = true
                    override val status: String
                        get() = ""
                })
            },
            onAccountSelected = ::doOnAccountSelected,
            title = R.string.transfer_receive_crypto_title,
            label = R.string.transfer_receive_crypto_label,
            icon = R.drawable.ic_receive_blue_circle
        )
    }

    private fun doOnAccountSelected(account: BlockchainAccount) {
        require(account is CryptoAccount)
        ReceiveActivity.start(requireContext(), account)
    }

    companion object {
        fun newInstance() = TransferReceiveFragment()
    }
}
