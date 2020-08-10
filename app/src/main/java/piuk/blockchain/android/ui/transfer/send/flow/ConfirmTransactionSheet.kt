package piuk.blockchain.android.ui.transfer.send.flow

import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.dialog_send_confirm.view.*
import kotlinx.android.synthetic.main.item_send_confirm_details.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.transfer.send.FlowInputSheet
import piuk.blockchain.android.ui.activity.detail.adapter.INPUT_FIELD_FLAGS
import piuk.blockchain.android.ui.activity.detail.adapter.MAX_NOTE_LENGTH
import piuk.blockchain.android.ui.transfer.send.SendErrorState
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.ui.transfer.send.SendStep
import piuk.blockchain.androidcoreui.utils.extensions.gone
import timber.log.Timber

data class PendingTxItem(
    val label: String,
    val value: String
)

class ConfirmTransactionSheet(
    host: SlidingModalBottomDialog.Host
) : FlowInputSheet(host) {
    override val layoutResource: Int = R.layout.dialog_send_confirm

    private val detailsAdapter = DetailsAdapter()
    private var state = SendState()

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! ConfirmTransactionSheet")
        require(newState.currentStep == SendStep.CONFIRM_DETAIL)

        // We _should_ always have a pending Tx when we get here
        require(newState.pendingTx != null)

        val totalAmount = (newState.sendAmount + newState.feeAmount).toStringWithSymbol()
        detailsAdapter.populate(
            listOf(
                PendingTxItem(getString(R.string.common_send), newState.sendAmount.toStringWithSymbol()),
                PendingTxItem(getString(R.string.common_from), newState.sendingAccount.label),
                PendingTxItem(getString(R.string.common_to), newState.sendTarget.label),
                addFeeItem(newState),
                PendingTxItem(getString(R.string.common_total), totalAmount)
            )
        )

        newState.pendingTx?.let {
            updateOptions(it)
        }

        dialogView.confirm_cta_button.text = getString(R.string.send_confirmation_cta_button,
            totalAmount)

        state = newState
    }

    private fun updateOptions(pendingTx: PendingTx) {
        // Current iteration only supports notes/description. But this is where all and any other
        // options - ie agreements, t&c confirmations etc are added and updated

        val note = pendingTx.getOption<TxOptionValue.TxTextOption>(TxOption.DESCRIPTION)
        note?.let { opt ->
            // Option exists. Show and update the field
            dialogView.confirm_details_note_input.apply {
                inputType = INPUT_FIELD_FLAGS
                filters = arrayOf(InputFilter.LengthFilter(MAX_NOTE_LENGTH))

                setOnEditorActionListener { v, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE && v.text.isNotEmpty()) {
                        model.process(SendIntent.ModifyTxOption(opt.copy(text = v.text.toString())))
                        clearFocus()
                    }
                    false
                }

                setText(opt.text, TextView.BufferType.EDITABLE)
            }
        } ?: dialogView.confirm_details_note_holder.gone()
    }

    private fun addFeeItem(state: SendState): PendingTxItem {
        val feeTitle = getString(R.string.common_spaced_strings,
            getString(R.string.send_confirmation_fee),
            getString(R.string.send_confirmation_regular_estimation))
        return if (state.errorState == SendErrorState.FEE_REQUEST_FAILED) {
            PendingTxItem(feeTitle, getString(R.string.send_confirmation_fee_error))
        } else {
            PendingTxItem(feeTitle, state.feeAmount.toStringWithSymbol())
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
