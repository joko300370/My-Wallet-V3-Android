package piuk.blockchain.android.ui.transfer.send.flow

import android.view.View
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import kotlinx.android.synthetic.main.dialog_send_prototype.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet
import piuk.blockchain.android.ui.transfer.send.SendInputSheet
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendState
import timber.log.Timber

class EnterTargetAddressSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_address

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! EnterTargetAddressSheet")
    }

    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    private fun onCtaClick() {
        model.process(
            SendIntent.AddressSelected(
            object : CryptoAddress(CryptoCurrency.ETHER, "An Address") {
                override val label: String = "Label for ETH address"
            }
        ))
    }

    companion object {
        fun newInstance(): EnterTargetAddressSheet =
            EnterTargetAddressSheet()
    }
}
