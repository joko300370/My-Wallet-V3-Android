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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatSpinner
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.ui.urllinks.URL_XLM_MIN_BALANCE
import kotlinx.android.synthetic.main.item_send_confirm_xlm_memo.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.databinding.ItemSendConfirmXlmMemoBinding
import piuk.blockchain.android.ui.activity.detail.adapter.INPUT_FIELD_FLAGS
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.customviews.installUpdateThrottle
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.visible

class ConfirmXlmMemoItemDelegate<in T>(
    private val model: TransactionModel,
    private val stringUtils: StringUtils,
    private val activity: Activity
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        return items[position] is TxConfirmationValue.Memo
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        XlmMemoItemViewHolder(
            ItemSendConfirmXlmMemoBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            stringUtils,
            activity
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as XlmMemoItemViewHolder).bind(
        items[position] as TxConfirmationValue.Memo,
        model
    )
}

private class XlmMemoItemViewHolder(
    private val binding: ItemSendConfirmXlmMemoBinding,
    private val stringUtils: StringUtils,
    private val activity: Activity
) : RecyclerView.ViewHolder(binding.root) {
    private val maxCharacters = 28

    init {
        binding.apply {
            confirmDetailsMemoSpinner.setupSpinner()
            confirmDetailsMemoSpinner.setSelection(TEXT_INDEX)
            confirmDetailsMemoInput.configureForSelection(TEXT_INDEX)
        }
    }

    fun bind(
        item: TxConfirmationValue.Memo,
        model: TransactionModel
    ) {
        binding.apply {
            if (item.isRequired) {
                showExchangeInfo()
            }

            confirmDetailsMemoSpinner.onItemSelectedListener = null

            if (!item.text.isNullOrBlank()) {
                confirmDetailsMemoSpinner.setSelection(TEXT_INDEX)
                confirmDetailsMemoInput.setText(item.text, TextView.BufferType.EDITABLE)
                model.process(TransactionIntent.ModifyTxOption(item.copy(text = item.text.toString())))
            } else if (item.id != null) {
                confirmDetailsMemoSpinner.setSelection(ID_INDEX)
                confirmDetailsMemoInput.setText(item.id.toString(), TextView.BufferType.EDITABLE)
                model.process(TransactionIntent.ModifyTxOption(item.copy(id = item.id)))
            } else {
                model.process(TransactionIntent.ModifyTxOption(item.copy(id = null, text = null)))
            }

            confirmDetailsMemoSpinner.addSpinnerListener(model, item, confirmDetailsMemoInput)

            if (item.editable) {
                with(confirmDetailsMemoInput) {
                    if (text?.isNotEmpty() == true) {
                        requestFocus()
                        setSelection(confirm_details_memo_input.text?.length ?: 0)
                    }
                    setupOnDoneListener(model, item)
                    setupMemoSaving(model, item)
                }
            } else {
                confirmDetailsMemoSpinner.isEnabled = false
                confirmDetailsMemoInput.isEnabled = false
                confirmDetailsMemoParent.alpha = 0.6f
            }
        }
    }

    private fun AppCompatEditText.setupOnDoneListener(
        model: TransactionModel,
        item: TxConfirmationValue.Memo
    ) {
        inputType = INPUT_FIELD_FLAGS
        filters = arrayOf(InputFilter.LengthFilter(maxCharacters))

        setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && v.text.isNotEmpty()) {
                if (itemView.confirm_details_memo_spinner.selectedItemPosition == TEXT_INDEX) {
                    model.process(
                        TransactionIntent.ModifyTxOption(item.copy(text = v.text.toString()))
                    )
                } else {
                    model.process(
                        TransactionIntent.ModifyTxOption(
                            item.copy(id = v.text.toString().toLong())
                        )
                    )
                }

                clearFocus()
            }
            true
        }
    }

    private fun AppCompatEditText.setupMemoSaving(
        model: TransactionModel,
        item: TxConfirmationValue.Memo
    ) {
        installUpdateThrottle { newText ->
            if (text?.isNotEmpty() == true) {
                if (itemView.confirm_details_memo_spinner.selectedItemPosition == TEXT_INDEX) {
                    if (haveContentsChanged(newText?.toString(), item.text)) {
                            model.process(
                                TransactionIntent.ModifyTxOption(item.copy(text = text.toString()))
                            )
                        }
                    } else {
                        if (haveContentsChanged(newText?.toString(), item.id?.toString())) {
                            model.process(
                                TransactionIntent.ModifyTxOption(
                                    item.copy(id = text.toString().toLong())
                                )
                            )
                        }
                    }
                }
            }
    }

    private fun AppCompatSpinner.setupSpinner() {
        val spinnerArrayAdapter: ArrayAdapter<String> =
            ArrayAdapter(
                context, android.R.layout.simple_spinner_dropdown_item,
                resources.getStringArray(R.array.xlm_memo_types_manual)
            )
        adapter = spinnerArrayAdapter
    }

    private fun AppCompatSpinner.addSpinnerListener(
        model: TransactionModel,
        item: TxConfirmationValue.Memo,
        editText: EditText
    ) {
        var isFirstTime = true
        onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    editText.configureForSelection(position)
                    if (!isFirstTime) {
                        model.process(
                            TransactionIntent.ModifyTxOption(item.copy(id = null, text = null))
                        )
                        editText.setText("", TextView.BufferType.EDITABLE)
                    }
                    isFirstTime = false
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // do nothing
                }
            }
    }

    // only save if same values after countdown but different from original
    private fun EditText.haveContentsChanged(currentText: String? = "", previousText: String? = ""): Boolean =
        text?.toString() == currentText && previousText != currentText

    private fun EditText.configureForSelection(selection: Int) {
        if (selection == TEXT_INDEX) {
            confirm_details_memo_input.hint =
                context.getString(R.string.send_confirm_memo_text_hint)
            confirm_details_memo_input.inputType = InputType.TYPE_CLASS_TEXT
        } else {
            confirm_details_memo_input.hint =
                context.getString(R.string.send_confirm_memo_id_hint)
            confirm_details_memo_input.inputType = InputType.TYPE_CLASS_NUMBER
        }
    }

    private fun ItemSendConfirmXlmMemoBinding.showExchangeInfo() {
        val boldIntro = context.getString(R.string.send_to_exchange_xlm_title)
        val blurb = context.getString(R.string.send_to_exchange_xlm_blurb)

        val map = mapOf("send_memo_link" to Uri.parse(URL_XLM_MIN_BALANCE))
        val link = stringUtils.getStringWithMappedAnnotations(
            R.string.send_to_exchange_xlm_link,
            map,
            activity
        )

        val sb = SpannableStringBuilder()
            .append(boldIntro)
            .append(blurb)
            .append(link)

        sb.setSpan(
            StyleSpan(Typeface.BOLD), 0, boldIntro.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        confirmDetailsMemoExchange.run {
            setText(sb, TextView.BufferType.SPANNABLE)
            movementMethod = LinkMovementMethod.getInstance()
            visible()
        }
    }

    companion object {
        private const val TEXT_INDEX = 0
        private const val ID_INDEX = 1
    }
}
