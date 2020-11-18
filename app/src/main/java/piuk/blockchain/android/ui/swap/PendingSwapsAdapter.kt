package piuk.blockchain.android.ui.swap

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.swap.nabu.datamanagers.SwapOrder
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import kotlinx.android.synthetic.main.swap_pending_item_layout.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.util.extensions.toFormattedDate
import piuk.blockchain.android.util.setAssetIconColours
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class PendingSwapsAdapter(
    private val orders: List<SwapOrder>,
    private val toFiat: (Money) -> Money
) :
    RecyclerView.Adapter<PendingSwapsAdapter.PendingSwapViewHolder>() {

    override fun getItemCount(): Int = orders.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingSwapViewHolder =
        PendingSwapViewHolder(
            parent.inflate(R.layout.swap_pending_item_layout)
        )

    override fun onBindViewHolder(holder: PendingSwapViewHolder, position: Int) {
        holder.bind(orders[position], toFiat)
    }

    class PendingSwapViewHolder(private val parent: View) : RecyclerView.ViewHolder(parent) {
        fun bind(swapOrder: SwapOrder, toFiat: (Money) -> Money) {
            with(parent) {
                title.text = resources.getString(
                    R.string.swap_direction, (swapOrder.inputMoney as CryptoValue).currency.displayTicker,
                    (swapOrder.outputMoney as CryptoValue).currency.displayTicker)
                subtitle.text = swapOrder.createdAt.toFormattedDate()
                fiatvalue.text = toFiat(swapOrder.inputMoney).toStringWithSymbol()
                cryptovalue.text = swapOrder.inputMoney.toStringWithSymbol()
                icon.setAssetIconColours((swapOrder.inputMoney as CryptoValue).currency, context)
            }
        }
    }
}