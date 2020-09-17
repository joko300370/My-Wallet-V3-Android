package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.app.Activity
import android.graphics.Typeface
import android.net.Uri
import android.text.InputFilter
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatSpinner
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.ui.urllinks.URL_XLM_MIN_BALANCE
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_send_confirm_xlm_memo.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.ui.activity.detail.adapter.INPUT_FIELD_FLAGS
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible

class ConfirmXlmMemoItemDelegate<in T>(
    private val model: TransactionModel,
    private val stringUtils: StringUtils,
    private val activity: Activity
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        return items[position] is TxOptionValue.Memo
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        XlmMemoItemViewHolder(
            parent.inflate(R.layout.item_send_confirm_xlm_memo),
            stringUtils,
            activity
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as XlmMemoItemViewHolder).bind(
        items[position] as TxOptionValue.Memo,
        model
    )
}

private class XlmMemoItemViewHolder(
    val parent: View,
    val stringUtils: StringUtils,
    val activity: Activity
) : RecyclerView.ViewHolder(parent), LayoutContainer {
    private val maxCharacters = 28
    private var isAutomaticInput = false

    override val containerView: View?
        get() = itemView

    init {
        itemView.apply {
            confirm_details_memo_spinner.setupSpinner()
        }
    }

    fun bind(
        item: TxOptionValue.Memo,
        model: TransactionModel
    ) {
        itemView.apply {
            if (item.isRequired) {
                showExchangeInfo()
            }

            confirm_details_memo_spinner.addSpinnerListener(model, item)

            isAutomaticInput = true
            if (!item.text.isNullOrBlank()) {
                confirm_details_memo_spinner.setSelection(0)
                confirm_details_memo_input.setText(item.text, TextView.BufferType.EDITABLE)
            } else if (item.id != null) {
                confirm_details_memo_spinner.setSelection(1)
                confirm_details_memo_input.setText(item.id.toString(), TextView.BufferType.EDITABLE)
            }
            isAutomaticInput = false

            confirm_details_memo_input.setupOnDoneListener(model, item)
        }
    }

    private fun AppCompatEditText.setupOnDoneListener(
        model: TransactionModel,
        item: TxOptionValue.Memo
    ) {
        inputType = INPUT_FIELD_FLAGS
        filters = arrayOf(InputFilter.LengthFilter(maxCharacters))

        setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && v.text.isNotEmpty()) {
                if (itemView.confirm_details_memo_spinner.selectedItemPosition == 0) {
                    model.process(
                        TransactionIntent.ModifyTxOption(item.copy(text = v.text.toString())))
                } else {
                    model.process(TransactionIntent.ModifyTxOption(
                        item.copy(id = v.text.toString().toLong())))
                }

                clearFocus()
            }
            true
        }
    }

    private fun AppCompatSpinner.setupSpinner() {
        val spinnerArrayAdapter: ArrayAdapter<String> =
            ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item,
                resources.getStringArray(R.array.xlm_memo_types_manual))

        adapter = spinnerArrayAdapter
        setSelection(0)
    }

    private fun AppCompatSpinner.addSpinnerListener(model: TransactionModel, item: TxOptionValue.Memo) {
        onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    itemView.apply {
                        if (position == 0) {
                            confirm_details_memo_input.hint =
                                context.getString(R.string.send_confirm_memo_text_hint)
                            confirm_details_memo_input.inputType = InputType.TYPE_CLASS_TEXT
                        } else {
                            confirm_details_memo_input.hint =
                                context.getString(R.string.send_confirm_memo_id_hint)
                            confirm_details_memo_input.inputType = InputType.TYPE_CLASS_NUMBER
                        }

                        if (!isAutomaticInput) {
                            model.process(
                                TransactionIntent.ModifyTxOption(item.copy(id = null, text = null)))
                            confirm_details_memo_input.setText("", TextView.BufferType.EDITABLE)
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // do nothing
                }
            }
    }

    private fun View.showExchangeInfo() {
        val boldIntro = context.getString(R.string.send_to_exchange_xlm_title)
        val blurb = context.getString(R.string.send_to_exchange_xlm_blurb)

        val map = mapOf("send_memo_link" to Uri.parse(URL_XLM_MIN_BALANCE))
        val link = stringUtils.getStringWithMappedLinks(
            R.string.send_to_exchange_xlm_link,
            map,
            activity)

        val sb = SpannableStringBuilder()
            .append(boldIntro)
            .append(blurb)
            .append(link)

        sb.setSpan(StyleSpan(Typeface.BOLD), 0, boldIntro.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        confirm_details_memo_exchange.run {
            setText(sb, TextView.BufferType.SPANNABLE)
            movementMethod = LinkMovementMethod.getInstance()
            visible()
        }
    }
}
