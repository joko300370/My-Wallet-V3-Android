package piuk.blockchain.android.ui.transactionflow.plugin

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSpinner
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.ui.urllinks.URL_TX_FEES
import org.koin.core.KoinComponent
import org.koin.core.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.FeeState
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.databinding.ViewEditTxFeesCtrlBinding
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.assetName
import piuk.blockchain.android.util.AfterTextChangedWatcher
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor

class EditFeesControl @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle), KoinComponent {

    private val feeList = mutableListOf<FeeLevel>()
    private val displayList = mutableListOf<String>()

    private val stringUtils: StringUtils by inject()
    private val analytics: TxFlowAnalytics by inject()

    private lateinit var textChangedWatcher: AfterTextChangedWatcher

    private val binding: ViewEditTxFeesCtrlBinding =
        ViewEditTxFeesCtrlBinding.inflate(LayoutInflater.from(context), this, true)

    private var shouldHideFeeOptionValue = true

    private fun makeTextWatcher(model: TransactionModel) =
        object : AfterTextChangedWatcher() {
            override fun afterTextChanged(s: Editable) {
                val input = s.toString()
                if (input.isNotEmpty()) {
                    sendFeeUpdate(model, FeeLevel.Custom, input.toLong())
                } else {
                    binding.feeOptionCustomIl.error = ""
                }
            }
        }

    fun update(feeSelection: FeeSelection, model: TransactionModel) {
        updateFeeList(feeSelection.availableLevels.toList())
        val selectedOption = feeSelection.selectedLevel

        if (!::textChangedWatcher.isInitialized) {
            textChangedWatcher = makeTextWatcher(model)
        }

        with(binding) {
            feeOptionCustom.removeTextChangedListener(textChangedWatcher)

            showFeeSelector(selectedOption, model, feeSelection)
            showFeeDetails(feeSelection)

            feeOptionCustom.addTextChangedListener(textChangedWatcher)

            val linksMap = mapOf<String, Uri>(
                "send_tx_fees" to Uri.parse(URL_TX_FEES)
            )

            val assetName = feeSelection.asset?.assetName()?.let {
                context.getString(it)
            } ?: ""

            val boldText = context.getString(R.string.tx_confirmation_fee_learn_more_1)
            val networkText = context.getString(
                R.string.tx_confirmation_fee_learn_more_2,
                assetName
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

    private fun feeToPosition(feeLevel: FeeLevel): Int = feeList.indexOf(feeLevel)

    private fun posToFeeLevel(pos: Int): FeeLevel = feeList[pos]

    private fun updateFeeList(list: List<FeeLevel>) {
        feeList.clear()
        feeList.addAll(list)

        displayList.clear()
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

    private fun showFeeDetails(feeSelection: FeeSelection) {
        feeSelection.feeState?.let {
            when (it) {
                is FeeState.FeeUnderMinLimit -> {
                    setCustomFeeValues(
                        feeSelection.customAmount,
                        context.getString(R.string.fee_options_sat_byte_min_error)
                    )
                }
                is FeeState.FeeUnderRecommended -> {
                    setCustomFeeValues(
                        feeSelection.customAmount,
                        context.getString(R.string.fee_options_fee_too_low)
                    )
                }
                is FeeState.FeeOverRecommended -> {
                    setCustomFeeValues(
                        feeSelection.customAmount,
                        context.getString(R.string.fee_options_fee_too_high)
                    )
                }
                is FeeState.ValidCustomFee -> {
                    setCustomFeeValues(feeSelection.customAmount)
                }
                is FeeState.FeeTooHigh -> {
                    shouldHideFeeOptionValue = false
                    updateFeeOptionValueVisibility()
                    binding.feeOptionValue.text =
                        context.getString(R.string.send_confirmation_insufficient_funds_for_fee)
                    binding.feeOptionValue.setTextColor(context.getResolvedColor(R.color.red_600))
                }
                is FeeState.FeeDetails -> {
                    setCustomFeeValues(feeSelection.customAmount)
                }
            }
        }
    }

    private fun setCustomFeeValues(customFee: Long, error: String = "") {
        with(binding) {
            if (customFee != -1L) {
                val fee = customFee.toString()
                feeOptionCustom.setText(fee, TextView.BufferType.EDITABLE)
                feeOptionCustom.setSelection(fee.length)
            } else {
                feeOptionValue.setText("", TextView.BufferType.EDITABLE)
                shouldHideFeeOptionValue = true
                updateFeeOptionValueVisibility()
            }
            feeOptionCustomIl.error = error
        }
    }

    private fun showFeeSelector(
        selectedOption: FeeLevel,
        model: TransactionModel,
        feeSelection: FeeSelection
    ) = with(binding) {
        if (feeList.size > 1) {
            feeOptionSelectSpinner.visible()
            feeOptionSelectSpinner.setupSpinner(selectedOption, model, feeSelection)
        } else {
            feeOptionSelectSpinner.gone()
        }
    }

    private fun AppCompatSpinner.setupSpinner(
        currentLevel: FeeLevel,
        model: TransactionModel,
        feeSelection: FeeSelection
    ) {
        val spinnerArrayAdapter: ArrayAdapter<String> =
            CustomPaddingArrayAdapter(
                context,
                R.layout.tx_fee_spinner_dropdown_item,
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
            FeeLevel.Custom -> showCustomFeeUi(feeSelection)
        }

        post {
            onItemSelectedListener = createSpinnerListener(model, currentLevel, feeSelection)
        }
    }

    private fun createSpinnerListener(
        model: TransactionModel,
        currentLevel: FeeLevel,
        item: FeeSelection
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
                binding.feeOptionCustom.requestFocus()
            } else {
                showStandardUi()
            }

            sendFeeUpdate(model, newFeeLevel)
            analytics.onFeeLevelChanged(currentLevel, newFeeLevel)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            // do nothing
        }
    }

    private fun showCustomFeeUi(feeSelection: FeeSelection) =
        with(binding) {
            feeOptionCustomBounds.text = context.getString(
                R.string.fee_options_sat_byte_inline_hint,
                feeSelection.customLevelRates?.regularFee.toString(),
                feeSelection.customLevelRates?.priorityFee.toString()
            )
            feeTypeSwitcher.displayedChild = SHOW_CUSTOM
        }

    private fun showStandardUi() {
        binding.feeTypeSwitcher.displayedChild = SHOW_STANDARD
        updateFeeOptionValueVisibility()
    }

    private fun updateFeeOptionValueVisibility() = binding.feeOptionValue.visibleIf { !shouldHideFeeOptionValue }

    private fun sendFeeUpdate(model: TransactionModel, level: FeeLevel, customFeeAmount: Long? = null) =
        model.process(
            TransactionIntent.SetFeeLevel(
                feeLevel = level,
                customFeeAmount = customFeeAmount
            )
        )

    companion object {
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