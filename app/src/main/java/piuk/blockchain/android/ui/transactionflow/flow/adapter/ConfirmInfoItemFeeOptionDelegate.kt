package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.ui.urllinks.URL_TX_FEES
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeState
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.databinding.ItemSendConfirmSelectFeeBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.flow.formatWithExchange
import piuk.blockchain.android.util.AfterTextChangedWatcher
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.context

class ConfirmInfoItemFeeOptionDelegate<in T>(
    private val model: TransactionModel,
    private val analytics: TxFlowAnalytics,
    private val stringUtils: StringUtils,
    private val assetResources: AssetResources
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        return items[position] is TxConfirmationValue.FeeSelection
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        FeeOptionViewHolder(ItemSendConfirmSelectFeeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as FeeOptionViewHolder).bind(
        items[position] as TxConfirmationValue.FeeSelection,
        model,
        analytics,
        stringUtils,
        assetResources
    )

    private class FeeOptionViewHolder(
        private val binding: ItemSendConfirmSelectFeeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val feeList = mutableListOf<FeeLevel>()
        private val displayList = mutableListOf<String>()

        private fun feeToPosition(feeLevel: FeeLevel): Int = feeList.indexOf(feeLevel)

        private fun posToFeeLevel(pos: Int): FeeLevel = feeList[pos]

        private fun updateFeeList(list: List<FeeLevel>) {
            feeList.clear()
            feeList.addAll(list)

            displayList.clear()
            with(itemView) {
                feeList.forEach {
                    displayList.add(
                        when (it) {
                            FeeLevel.None -> throw IllegalStateException("Fee level None not supported")
                            FeeLevel.Regular -> context.getString(
                                R.string.fee_options_label,
                                context.getString(R.string.fee_options_regular),
                                context.getString(R.string.fee_options_regular_time)
                            )
                            FeeLevel.Priority -> context.getString(
                                R.string.fee_options_label, context.getString(R.string.fee_options_priority),
                                context.getString(R.string.fee_options_priority_time)
                            )
                            FeeLevel.Custom -> context.getString(
                                R.string.fee_options_label,
                                context.getString(R.string.fee_options_custom),
                                context.getString(R.string.fee_options_custom_warning)
                            )
                        }
                    )
                }
            }
        }

        private lateinit var textChangedWatcher: AfterTextChangedWatcher
        fun bind(
            item: TxConfirmationValue.FeeSelection,
            model: TransactionModel,
            analytics: TxFlowAnalytics,
            stringUtils: StringUtils,
            assetResources: AssetResources
        ) {
            updateFeeList(item.availableLevels.toList())
            val selectedOption = item.selectedLevel

            if (!::textChangedWatcher.isInitialized) {
                textChangedWatcher = makeTextWatcher(model, item)
            }

            with(binding) {
                feeOptionCustom.removeTextChangedListener(textChangedWatcher)

                showFeeSelector(selectedOption, model, analytics, item)

                showFeeDetails(item)

                feeOptionCustom.addTextChangedListener(textChangedWatcher)

                val linksMap = mapOf<String, Uri>(
                    "send_tx_fees" to Uri.parse(URL_TX_FEES)
                )

                val boldText = context.getString(R.string.tx_confirmation_fee_learn_more_1)
                val networkText = context.getString(
                    R.string.tx_confirmation_fee_learn_more_2,
                    context.getString(assetResources.assetNameRes(item.asset))
                )

                val linkedText = stringUtils.getStringWithMappedAnnotations(
                    R.string.tx_confirmation_fee_learn_more_3,
                    linksMap,
                    context
                )

                val sb = SpannableStringBuilder()
                    .append(boldText)
                    .append(networkText)
                    .append(linkedText)

                sb.setSpan(StyleSpan(Typeface.BOLD), 0, boldText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                feeLearnMore.setText(sb, TextView.BufferType.SPANNABLE)
                feeLearnMore.movementMethod = LinkMovementMethod.getInstance()
            }
        }

        private fun ItemSendConfirmSelectFeeBinding.showFeeDetails(item: TxConfirmationValue.FeeSelection) {
            item.feeDetails?.let {
                when (it) {
                    is FeeState.FeeUnderMinLimit -> {
                        setCustomFeeValues(
                            item.customFeeAmount,
                            context.getString(R.string.fee_options_sat_byte_min_error)
                        )
                    }
                    is FeeState.FeeUnderRecommended -> {
                        setCustomFeeValues(item.customFeeAmount, context.getString(R.string.fee_options_fee_too_low))
                    }
                    is FeeState.FeeOverRecommended -> {
                        setCustomFeeValues(item.customFeeAmount, context.getString(R.string.fee_options_fee_too_high))
                    }
                    is FeeState.ValidCustomFee -> {
                        setCustomFeeValues(item.customFeeAmount)
                    }
                    is FeeState.FeeTooHigh -> {
                        feeOptionValue.text = context.getString(R.string.send_confirmation_insufficient_fee)
                        feeOptionValue.setTextColor(ContextCompat.getColor(context, R.color.red_600))
                    }
                    is FeeState.FeeDetails -> {
                        feeOptionValue.text = it.absoluteFee.formatWithExchange(item.exchange)
                        feeOptionValue.setTextColor(ContextCompat.getColor(context, R.color.grey_800))
                    }
                }
            }
        }

        private fun makeTextWatcher(model: TransactionModel, item: TxConfirmationValue.FeeSelection) =
            object : AfterTextChangedWatcher() {
                override fun afterTextChanged(s: Editable) {
                    val input = s.toString()
                    if (input.isNotEmpty()) {
                        model.process(
                            TransactionIntent.ModifyTxOption(
                                TxConfirmationValue.FeeSelection(
                                    selectedLevel = FeeLevel.Custom,
                                    customFeeAmount = input.toLong(), asset = item.asset
                                )
                            )
                        )
                    } else {
                        binding.feeOptionCustomIl.error = ""
                    }
                }
            }

        private fun ItemSendConfirmSelectFeeBinding.setCustomFeeValues(customFee: Long, error: String = "") {
            if (customFee != -1L) {
                val fee = customFee.toString()
                feeOptionCustom.setText(fee, TextView.BufferType.EDITABLE)
                feeOptionCustom.setSelection(fee.length)
                feeOptionCustom.requestFocus()
            }

            feeOptionCustomIl.error = error
        }

        private fun ItemSendConfirmSelectFeeBinding.showFeeSelector(
            selectedOption: FeeLevel,
            model: TransactionModel,
            analytics: TxFlowAnalytics,
            item: TxConfirmationValue.FeeSelection
        ) {
            if (feeList.size > 1) {
                feeOptionSelectSpinner.setupSpinner(selectedOption, model, analytics, item)
                feeSwitcher.displayedChild = SHOW_DROPDOWN
            } else {
                feeSwitcher.displayedChild = SHOW_STATIC
            }
        }

        private fun AppCompatSpinner.setupSpinner(
            currentLevel: FeeLevel,
            model: TransactionModel,
            analytics: TxFlowAnalytics,
            item: TxConfirmationValue.FeeSelection
        ) {
            val spinnerArrayAdapter: ArrayAdapter<String> =
                CustomPaddingArrayAdapter(
                    context,
                    android.R.layout.simple_spinner_dropdown_item,
                    displayList
                )

            adapter = spinnerArrayAdapter
            val newSelection = feeToPosition(currentLevel)

            onItemSelectedListener = null
            setSelection(newSelection)

            when (currentLevel) {
                FeeLevel.None -> throw IllegalStateException("Fee level None not supported")
                FeeLevel.Regular,
                FeeLevel.Priority -> showStandardUi()
                FeeLevel.Custom -> showCustomFeeUi(item)
            }

            post {
                onItemSelectedListener = createSpinnerListener(model, analytics, currentLevel, item)
            }
        }

        private fun createSpinnerListener(
            model: TransactionModel,
            analytics: TxFlowAnalytics,
            currentLevel: FeeLevel,
            item: TxConfirmationValue.FeeSelection
        ) = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val newFeeLevel = posToFeeLevel(position)

                if (newFeeLevel == FeeLevel.Custom) {
                    showCustomFeeUi(item)
                } else {
                    showStandardUi()
                }

                model.process(
                    TransactionIntent.ModifyTxOption(
                        TxConfirmationValue.FeeSelection(
                            selectedLevel = newFeeLevel,
                            asset = item.asset
                        )
                    )
                )
                analytics.onFeeLevelChanged(currentLevel, newFeeLevel)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // do nothing
            }
        }

        private fun showCustomFeeUi(item: TxConfirmationValue.FeeSelection) {
            with(binding) {
                feeOptionCustomBounds.text = context.getString(
                    R.string.fee_options_sat_byte_inline_hint,
                    item.feeInfo?.regularFee.toString(),
                    item.feeInfo?.priorityFee.toString()
                )

                feeTypeSwitcher.displayedChild = SHOW_CUSTOM
            }
        }

        private fun showStandardUi() {
            binding.feeTypeSwitcher.displayedChild = SHOW_STANDARD
        }
    }

    companion object {
        private const val SHOW_DROPDOWN = 0
        private const val SHOW_STATIC = 1
        private const val SHOW_STANDARD = 0
        private const val SHOW_CUSTOM = 1
    }
}

private class CustomPaddingArrayAdapter<T>(context: Context, layoutId: Int, items: MutableList<T>) :
    ArrayAdapter<T>(context, layoutId, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val smallPadding = context.resources.getDimension(R.dimen.tiny_margin).toInt()
        view.setPadding(0, smallPadding, view.paddingRight, smallPadding)
        return view
    }
}