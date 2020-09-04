package piuk.blockchain.android.ui.transfer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import kotlinx.android.synthetic.main.fragment_transfer.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.transfer.receive.TransferReceiveFragment
import piuk.blockchain.android.ui.transfer.send.TransferSendFragment
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class TransferFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_transfer)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transfer_tabs.setupWithViewPager(transfer_pager)

        transfer_pager.adapter = TransferPagerAdapter(
            listOf(getString(R.string.send), getString(R.string.common_receive)),
            childFragmentManager
        )
    }

    companion object {
        fun newInstance(): TransferFragment {
            return TransferFragment()
        }
    }
}

@SuppressLint("WrongConstant")
class TransferPagerAdapter(
    private val titlesList: List<String>,
    fragmentManager: FragmentManager
) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getCount(): Int = titlesList.size
    override fun getPageTitle(position: Int): CharSequence =
        titlesList[position]

    override fun getItem(position: Int): Fragment = when (position) {
        0 -> TransferSendFragment.newInstance()
        else -> TransferReceiveFragment.newInstance()
    }
}