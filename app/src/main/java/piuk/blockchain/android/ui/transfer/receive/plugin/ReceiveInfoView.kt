package piuk.blockchain.android.ui.transfer.receive.plugin

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.databinding.ViewReceiveInfoBinding

class ReceiveInfoView @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle) {

    private val binding: ViewReceiveInfoBinding =
        ViewReceiveInfoBinding.inflate(LayoutInflater.from(context), this, true)

    fun update(account: CryptoAccount, onCloseClicked: () -> Unit) {
        with(binding) {
            infoDescription.text =
                context.getString(R.string.receive_rotating_address_desc, account.asset.displayTicker, account.label)

            infoClose.setOnClickListener {
                onCloseClicked()
            }
        }
    }
}
