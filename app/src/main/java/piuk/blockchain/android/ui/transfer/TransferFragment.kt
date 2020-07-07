package piuk.blockchain.android.ui.transfer

import android.os.Bundle
import androidx.fragment.app.Fragment

class TransferFragment : Fragment() {

    // TODO add viewpager & show correct starting view

    companion object {
        enum class StartingView {
            SHOW_SEND,
            SHOW_RECEIVE
        }

        private const val START_AT_VIEW = "START_AT_VIEW"
        fun newInstance(startAt: StartingView = StartingView.SHOW_SEND): TransferFragment {
            return TransferFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(START_AT_VIEW, startAt)
                }
            }
        }
    }
}