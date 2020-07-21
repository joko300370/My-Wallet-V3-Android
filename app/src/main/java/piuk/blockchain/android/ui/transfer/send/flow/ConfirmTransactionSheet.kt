package piuk.blockchain.android.ui.transfer.send.flow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.dialog_send_confirm.view.*
import kotlinx.android.synthetic.main.item_send_confirm_details.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.transfer.send.SendErrorState
import piuk.blockchain.android.ui.transfer.send.SendInputSheet
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.ui.transfer.send.SendStep
import timber.log.Timber

data class PendingTxItem(
    val label: String,
    val value: String
)

class ConfirmTransactionSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_confirm

    private val detailsAdapter = DetailsAdapter()
    private var state = SendState()

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! ConfirmTransactionSheet")
        require(newState.currentStep == SendStep.CONFIRM_DETAIL)

        detailsAdapter.populate(
            listOf(
                PendingTxItem("Send", newState.sendAmount.toStringWithSymbol()),
                PendingTxItem("From", newState.sendingAccount.label),
                PendingTxItem("To", newState.targetAddress.label),
                addFeeItem(newState),
                PendingTxItem("Total",
                    (newState.sendAmount.toFloat() + newState.feeAmount.toFloat()).toString())
            )
        )

        state = newState
    }

    private fun addFeeItem(state: SendState): PendingTxItem =
        when {
            state.errorState == SendErrorState.FEE_REQUEST_FAILED -> {
                PendingTxItem("Fee - Regular", "Error loading fee")
            }
            state.feeAmount.isZero -> {
                model.process(SendIntent.RequestFee)
                PendingTxItem("Fee - Regular", "Loading...")
            }
            else -> {
                PendingTxItem("Fee - Regular", state.feeAmount.toStringWithSymbol())
            }
        }

    override fun initControls(view: View) {
        view.confirm_cta_button.setOnClickListener { onCtaClick() }

        with(view.confirm_details_list) {
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )

            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            )
            adapter = detailsAdapter
        }

        view.confirm_sheet_back.setOnClickListener {
            model.process(SendIntent.ReturnToPreviousStep)
        }
    }

    private fun onCtaClick() {
        model.process(SendIntent.ExecuteTransaction)
    }

    companion object {
        fun newInstance(): ConfirmTransactionSheet =
            ConfirmTransactionSheet()
    }
}

class DetailsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val itemsList = mutableListOf<PendingTxItem>()

    internal fun populate(items: List<PendingTxItem>) {
        itemsList.clear()
        itemsList.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return DetailsItemVH(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_send_confirm_details,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int =
        itemsList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = itemsList[position]
        when (holder) {
            is DetailsItemVH -> holder.bind(item.label, item.value)
            else -> {
            }
        }
    }
}

private class DetailsItemVH(val parent: View) :
    RecyclerView.ViewHolder(parent),
    LayoutContainer {

    override val containerView: View?
        get() = itemView

    fun bind(label: String, value: String) {
        itemView.confirmation_item_label.text = label
        itemView.confirmation_item_value.text = value
    }
}
