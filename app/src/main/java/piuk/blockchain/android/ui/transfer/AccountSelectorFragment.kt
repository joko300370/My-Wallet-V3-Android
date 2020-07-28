package piuk.blockchain.android.ui.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.blockchain.koin.scopedInject
import kotlinx.android.synthetic.main.fragment_transfer_account_selector.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.customviews.account.StatusDecorator
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible

typealias AccountListFilterFn = (BlockchainAccount) -> Boolean

open class AccountSelectorFragment : Fragment() {

    private val coincore: Coincore by scopedInject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_transfer_account_selector, container, false)

    private val filterFn: AccountListFilterFn =
        { account -> (account is CryptoAccount) && account.isFunded }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        account_selector_account_list.onLoadError = ::doOnLoadError
        account_selector_account_list.onEmptyList = ::doOnEmptyList
    }

    fun initialiseAccountSelector(
        statusDecorator: StatusDecorator,
        onAccountSelected: (BlockchainAccount) -> Unit
    ) {
        account_selector_account_list.onAccountSelected = onAccountSelected
        account_selector_account_list.initialise(
            coincore.allWallets().map { it.accounts.filter(filterFn) },
            status = statusDecorator
        )
    }

    fun setBlurbText(value: String) {
        account_selector_blurb.text = value
    }

    private fun doOnEmptyList() {
        account_selector_account_list.gone()
        account_selector_blurb.gone()
        account_selector_empty_view.visible()
        account_selector_button_buy_crypto.setOnClickListener {
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
}