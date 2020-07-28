package piuk.blockchain.android.ui.transfer.receive

import android.os.Bundle
import android.view.View
import android.widget.Toast
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.ui.transfer.AccountSelectorFragment

class TransferReceiveFragment : AccountSelectorFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setBlurbText(getString(R.string.transfer_receive_crypto))
        initialiseAccountSelector(
            statusDecorator = ::statusDecorator,
            onAccountSelected = ::doOnAccountSelected
        )
    }

    private fun statusDecorator(account: BlockchainAccount): Single<String> =
        // TODO: how do we decorate Receive accounts?
        Single.just("")

    private fun doOnAccountSelected(account: BlockchainAccount) {
        if (account is CryptoAccount) {
            Toast.makeText(requireContext(), "TODO: Implement on account selected",
                Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance() = TransferReceiveFragment()
    }
}
