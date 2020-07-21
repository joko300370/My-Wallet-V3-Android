package piuk.blockchain.android.ui.transfer.send.flow

import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.dialog_send_confirm.view.*
import kotlinx.android.synthetic.main.item_send_confirm_details.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.activity.detail.adapter.INPUT_FIELD_FLAGS
import piuk.blockchain.android.ui.activity.detail.adapter.MAX_NOTE_LENGTH
import piuk.blockchain.android.ui.transfer.send.NoteState
import piuk.blockchain.android.ui.transfer.send.SendErrorState
import piuk.blockchain.android.ui.transfer.send.SendInputSheet
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.ui.transfer.send.SendStep
import piuk.blockchain.androidcoreui.utils.extensions.gone
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
                PendingTxItem(getString(R.string.common_send),
                    newState.sendAmount.toStringWithSymbol()),
                PendingTxItem(getString(R.string.common_from), newState.sendingAccount.label),
                PendingTxItem(getString(R.string.common_to), newState.targetAddress.label),
                addFeeItem(newState),
                PendingTxItem(getString(R.string.common_total),
                    (newState.sendAmount + newState.feeAmount).toStringWithSymbol())
            )
        )

        showAddNoteIfSupported(newState)
        showNoteState(newState)

        dialogView.confirm_cta_button.text = getString(R.string.send_confirmation_cta_button,
            newState.sendAmount.toStringWithSymbol())

        state = newState
    }

    private fun showNoteState(newState: SendState) {
        if (newState.note.isNotEmpty()) {
            dialogView.confirm_details_note_input.setText(newState.note,
                TextView.BufferType.EDITABLE)
        } else {
            when (newState.noteState) {
                NoteState.UPDATE_SUCCESS -> {
                    Toast.makeText(requireContext(),
                        getString(R.string.send_confirmation_add_note_success), Toast.LENGTH_SHORT)
                        .show()
                }
                NoteState.UPDATE_ERROR -> {
                    // can this happen?
                }
                NoteState.NOT_SET -> {
                    // do nothing
                }
            }
        }
    }

    private fun showAddNoteIfSupported(state: SendState) {
        state.transactionNoteSupported?.let {
            if (it) {
                dialogView.confirm_details_note_input.apply {
                    inputType = INPUT_FIELD_FLAGS
                    filters = arrayOf(InputFilter.LengthFilter(MAX_NOTE_LENGTH))

                    setOnEditorActionListener { v, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE && v.text.isNotEmpty()) {
                            model.process(SendIntent.NoteAdded(v.text.toString()))
                            clearFocus()
                        }

                        false
                    }
                }
            } else {
                dialogView.confirm_details_note_holder.gone()
            }
        } ?: model.process(SendIntent.RequestTransactionNoteSupport)
    }

    private fun addFeeItem(state: SendState): PendingTxItem {
        val feeTitle = getString(R.string.common_spaced_strings,
            getString(R.string.send_confirmation_fee),
            getString(R.string.send_confirmation_regular_estimation))
        return when {
            state.errorState == SendErrorState.FEE_REQUEST_FAILED -> {
                PendingTxItem(feeTitle, getString(R.string.send_confirmation_fee_error))
            }
            state.feeAmount.isZero -> {
                model.process(SendIntent.RequestFee)
                PendingTxItem(feeTitle, getString(R.string.send_confirmation_fee_loading))
            }
            else -> {
                PendingTxItem(feeTitle, state.feeAmount.toStringWithSymbol())
            }
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
