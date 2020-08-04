package piuk.blockchain.android.ui.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.fragment_transfer.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.ui.transfer.receive.TransferReceiveFragment
import piuk.blockchain.android.ui.transfer.send.TransferSendFragment
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class TransferFragment : Fragment() {

    private val pagerAdapter by lazy {
        TransferPagerAdapter(this, startingAccount as? CryptoAccount)
    }

    private val startingView by lazy {
        arguments?.getSerializable(START_AT_VIEW)
    }

    private val startingAccount by lazy {
        arguments?.getSerializable(STARTING_ACCOUNT)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_transfer)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transfer_pager.adapter = pagerAdapter

        TabLayoutMediator(transfer_tabs, transfer_pager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.send)
                1 -> getString(R.string.common_receive)
                else -> ""
            }
        }.attach()

        if (startingView != StartingView.SHOW_SEND) {
            transfer_pager.setCurrentItem(1, true)
        }
    }
    companion object {
        enum class StartingView {
            SHOW_SEND,
            SHOW_RECEIVE
        }

        private const val START_AT_VIEW = "START_AT_VIEW"
        private const val STARTING_ACCOUNT = "STARTING_ACCOUNT"
        fun newInstance(startAt: StartingView = StartingView.SHOW_SEND, account: CryptoAccount? = null): TransferFragment {
            return TransferFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(START_AT_VIEW, startAt)
                    account?.let {
                        putSerializable(STARTING_ACCOUNT, account)
                    }
                }
            }
        }
    }
}

class TransferPagerAdapter(fragment: Fragment, private val startingAccount: CryptoAccount?) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment =
        when (position) {
        0 -> TransferSendFragment.newInstance(startingAccount)
        1 -> TransferReceiveFragment.newInstance(startingAccount)
        else -> throw IllegalStateException("Only two fragments allowed")
    }
}