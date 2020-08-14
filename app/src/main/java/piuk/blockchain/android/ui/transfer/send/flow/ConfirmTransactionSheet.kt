package piuk.blockchain.android.ui.transfer.send.flow

import android.graphics.Typeface.BOLD
import android.net.Uri
import android.text.InputFilter
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.ui.urllinks.INTEREST_PRIVACY_POLICY
import com.blockchain.ui.urllinks.INTEREST_TERMS_OF_SERVICE
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRates
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.dialog_send_confirm.view.*
import kotlinx.android.synthetic.main.item_send_confirm_details.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.ui.activity.detail.adapter.INPUT_FIELD_FLAGS
import piuk.blockchain.android.ui.activity.detail.adapter.MAX_NOTE_LENGTH
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.transfer.send.FlowInputSheet
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.ui.transfer.send.SendStep
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible
import timber.log.Timber

data class PendingTxItem(
    val label: String,
    val value: String
)

class ConfirmTransactionSheet(
    host: SlidingModalBottomDialog.Host
) : FlowInputSheet(host) {
    override val layoutResource: Int = R.layout.dialog_send_confirm

    private val stringUtils: StringUtils by inject()
    private val exchangeRates: ExchangeRates by scopedInject()
    private val prefs: CurrencyPrefs by scopedInject()

    private val detailsAdapter = DetailsAdapter()
    private var state = SendState()

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! ConfirmTransactionSheet")
        require(newState.currentStep == SendStep.CONFIRM_DETAIL)

        // We _should_ always have a pending Tx when we get here
        require(newState.pendingTx != null)

        val itemList = mutableListOf(
            PendingTxItem(getString(R.string.common_from), newState.sendingAccount.label),
            PendingTxItem(getString(R.string.common_to), newState.sendTarget.label)
        )

        getFeeItem(newState)?.let {
            itemList.add(it)
        }

        itemList.add(PendingTxItem(getString(R.string.common_total), getTotalAmount(newState)))

        // These options should probably be items also TODO
        updateOptions(newState, itemList)

        showSendUi(newState, itemList)

        detailsAdapter.populate(itemList)

        state = newState
    }

    private fun getTotalAmount(newState: SendState): String {
        val fee = newState.feeAmount
        val amount = newState.sendAmount

        return if (amount.symbol == fee.symbol) {
            (amount + fee).toStringWithSymbol()
        } else {
            "${amount.toStringWithSymbol()} (${fee.toStringWithSymbol()})"
        }
    }

    private fun updateOptions(
        state: SendState,
        itemList: MutableList<PendingTxItem>
    ) {
        // Current iteration only supports notes/description. But this is where all and any other
        // options - ie agreements, t&c confirmations etc are added and updated

        val note = state.pendingTx?.getOption<TxOptionValue.TxTextOption>(TxOption.DESCRIPTION)
        note?.let { opt ->
            dialogView.confirm_details_bottom_view_switcher.displayedChild = DESCRIPTION_INPUT

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
        }

        val agreement = state.pendingTx?.getOption<TxOptionValue.TxTextOption>(TxOption.AGREEMENT)
        agreement?.let {
            dialogView.confirm_details_bottom_view_switcher.displayedChild = AGREEMENT_INPUT

            setupTosAndPPLinks()
            setupHoldingValues(state.sendAmount)
            setupCheckboxEvents()
            // TODO how do we show specific UI for a different transfer type?
            // showDepositUi(state, itemList)
        } // ?: showSendUi(state, itemList)

        if (note != null || agreement != null) {
            dialogView.confirm_details_bottom_view_switcher.visible()
        } else {
            dialogView.confirm_details_bottom_view_switcher.gone()
        }
    }

    private fun showDepositUi(
        state: SendState,
        itemList: MutableList<PendingTxItem>
    ) {
        dialogView.confirm_cta_button.text = getString(R.string.send_confirmation_deposit_cta_button)
        itemList.add(0, PendingTxItem(getString(R.string.common_deposit),
            state.sendAmount.toStringWithSymbol()))
    }

    private fun showSendUi(
        state: SendState,
        itemList: MutableList<PendingTxItem>
    ) {
        dialogView.confirm_cta_button.text = getString(R.string.send_confirmation_cta_button,
            getTotalAmount(state))
        itemList.add(0,
            PendingTxItem(getString(R.string.common_send), state.sendAmount.toStringWithSymbol()))
    }

    private fun getFeeItem(state: SendState): PendingTxItem? {
        state.pendingTx?.let {
            if (it.feeLevel != FeeLevel.None) {
                val feeTitle = getString(
                    R.string.common_spaced_strings,
                    getString(R.string.send_confirmation_fee),
                    getString(R.string.send_confirmation_regular_estimation)
                )
                return PendingTxItem(feeTitle, state.feeAmount.toStringWithSymbol())
            }
        }
        return null
    }

    private fun setupTosAndPPLinks() {
        val linksMap = mapOf<String, Uri>(
            "interest_tos" to Uri.parse(INTEREST_TERMS_OF_SERVICE),
            "interest_pp" to Uri.parse(INTEREST_PRIVACY_POLICY)
        )
        dialogView.confirm_details_tos_pp_checkbox.text =
            stringUtils.getStringWithMappedLinks(
                R.string.send_confirmation_interest_tos_pp,
                linksMap,
                requireActivity()
            )
        dialogView.confirm_details_tos_pp_checkbox.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setupHoldingValues(sendAmount: CryptoValue) {
        val part1 = getString(R.string.send_confirmation_interest_holding_period_1)
        val part2 =
            sendAmount.toFiat(exchangeRates, prefs.selectedFiatCurrency).toStringWithSymbol()
        val part3 = getString(R.string.send_confirmation_interest_holding_period_2,
            sendAmount.toStringWithSymbol())
        val sb = SpannableStringBuilder()
        sb.append(part1)
        sb.append(part2)
        sb.setSpan(StyleSpan(BOLD), part1.length, part1.length + part2.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append(part3)
        dialogView.confirm_details_holdings_checkbox.setText(sb, TextView.BufferType.SPANNABLE)
    }

    private fun setupCheckboxEvents() {
        dialogView.confirm_cta_button.isEnabled = false

        dialogView.confirm_details_tos_pp_checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (dialogView.confirm_details_holdings_checkbox.isChecked) {
                    dialogView.confirm_cta_button.isEnabled = true
                }
            } else {
                dialogView.confirm_cta_button.isEnabled = false
            }
        }

        dialogView.confirm_details_holdings_checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (dialogView.confirm_details_tos_pp_checkbox.isChecked) {
                    dialogView.confirm_cta_button.isEnabled = true
                }
            } else {
                dialogView.confirm_cta_button.isEnabled = false
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
        private const val DESCRIPTION_INPUT = 0
        private const val AGREEMENT_INPUT = 1
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
