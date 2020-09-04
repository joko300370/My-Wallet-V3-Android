package piuk.blockchain.android.ui.transfer.send.activity

import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import com.blockchain.transactions.Memo
import info.blockchain.wallet.util.HexUtils
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible
import java.lang.Exception
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.DialogFragment
import androidx.appcompat.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.blockchain.account.DefaultAccountDataManager
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcoreui.utils.extensions.invisible

class MemoEditDialog : DialogFragment() {

    private val compositeDisposable = CompositeDisposable()

    init {
        setStyle(STYLE_NO_FRAME, R.style.FullscreenDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.dialog_edit_memo,
        container,
        false
    ).apply {
        isFocusableInTouchMode = true
        requestFocus()
        dialog?.window?.setWindowAnimations(R.style.DialogNoAnimations)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolBar(view)

        view.findViewById<View>(R.id.button_ok).setOnClickListener {
            setResultAndDismiss()
        }

        setUpFieldValidation(view)

        setupSpinner(view)
    }

    override fun onResume() {
        super.onResume()
        showKeyboard(view!!.context)
    }

    private fun setupSpinner(view: View) {
        view.findViewById<Spinner>(R.id.memo_type_spinner)
            .also { spinner ->
                val index = typeIndexFromArgument(arguments)
                spinner.setupOptions(view.context, index)
                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                    override fun onItemSelected(parent: AdapterView<*>, spinner: View, pos: Int, id: Long) {
                        fieldsAndTypes.forEach { (itemId) ->
                            view.findViewById<View>(itemId).gone()
                        }
                        fieldsAndTypes.forEachIndexed { index, (itemId) ->
                            view.findViewById<View>(itemId).update(pos, itemPosition = index)
                        }
                        memoIsValid()
                    }

                    private fun View.update(selectedPosition: Int, itemPosition: Int) {
                        if (selectedPosition == itemPosition) {
                            visible()
                        }
                        if (selectedPosition == itemPosition) post { requestFocus() }
                        setOnKeyListener { _, keyCode, _ ->
                            return@setOnKeyListener if (keyCode == KeyEvent.KEYCODE_ENTER && memoIsValid()) {
                                setResultAndDismiss()
                                true
                            } else {
                                false
                            }
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {
                    }
                }
                populateFromArguments(spinner, index)
            }
    }

    private fun setupToolBar(view: View) {
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar_general)
        toolbar.setTitle(R.string.xlm_memo_toolbar_title)
        toolbar.setNavigationOnClickListener { dismiss() }
    }

    private fun setUpFieldValidation(view: View) {
        val validator = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                view.findViewById<View>(R.id.button_ok).isEnabled = memoIsValid()
            }
        }
        fieldsAndTypes.map { (id) -> id }.distinct().forEach { id ->
            view.findViewById<EditText>(id).addTextChangedListener(validator)
        }
    }

    private fun memoIsValid(): Boolean {
        val value = enteredValue()
        return when (findFieldId(selectedIndex())) {
            R.id.memo_text -> value.length <= 28
            R.id.memo_id -> isValidId(value)
            R.id.memo_hash -> isValidHash(value)
            else -> true
        }
    }

    private fun isValidId(s: String): Boolean {
        return try {
            s.toLong()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isValidHash(s: String): Boolean {
        return try {
            HexUtils.decodeHex(s.toCharArray()).size == 32
        } catch (e: Exception) {
            false
        }
    }

    private fun populateFromArguments(spinner: Spinner, index: Int) {
        arguments?.let {
            spinner.setSelection(index)
            textView(index).text = it.getString(ARGUMENT_VALUE)
        }
    }

    private fun typeIndexFromArgument(bundle: Bundle?): Int {
        if (bundle == null) return 0
        val argType = bundle.getString(ARGUMENT_TYPE)
        return fieldsAndTypes.indexOfFirst { (_, type) -> type == argType }
    }

    private fun showKeyboard(context: Context) {
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    private fun setResultAndDismiss() {
        targetFragment?.onActivityResult(targetRequestCode,
            AppCompatActivity.RESULT_OK,
            Intent().apply {
                putExtra(ARGUMENT_TYPE, selectedType())
                putExtra(ARGUMENT_VALUE, enteredValue())
            }
        )
        dismiss()
    }

    private fun enteredValue(): String =
        textView(selectedIndex()).text.toString()

    private fun textView(selectedIndex: Int): TextView =
        view!!.findViewById(findFieldId(selectedIndex))

    @IdRes
    private fun findFieldId(selectedIndex: Int) = findFieldAndType(selectedIndex).first

    private fun selectedType() =
        findFieldAndType(selectedIndex()).second

    /**
     * The order of these must match the order in [R.array.xlm_memo_types_all] and [R.array.xlm_memo_types_manual]
     * though those arrays can be shorter than this list.
     */
    private val fieldsAndTypes = listOf(
        Pair(R.id.memo_text, "text"),
        Pair(R.id.memo_id, "id"),
        Pair(R.id.memo_hash, "hash"),
        Pair(R.id.memo_hash, "return")
    )

    private fun findFieldAndType(selectedIndex: Int) = fieldsAndTypes[selectedIndex]

    private fun selectedIndex() = view!!.findViewById<Spinner>(R.id.memo_type_spinner).selectedItemPosition

    private fun Spinner.setupOptions(context: Context, selectedIndex: Int) {
        ArrayAdapter.createFromResource(
            context,
            arrayToDisplay(selectedIndex),
            R.layout.dialog_edit_memo_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            this.adapter = adapter
        }
    }

    private fun arrayToDisplay(selectedIndex: Int): Int {
        val manualArraySize = view!!.resources.getStringArray(R.array.xlm_memo_types_manual)
        return if (selectedIndex < manualArraySize.size) {
            R.array.xlm_memo_types_manual
        } else {
            R.array.xlm_memo_types_all
        }
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    companion object {

        private const val ARGUMENT_VALUE = "VALUE"
        private const val ARGUMENT_TYPE = "TYPE"

        fun toMemo(intent: Intent?): Memo {
            if (intent == null) return Memo.None
            return Memo(
                value = intent.extras.getString(ARGUMENT_VALUE),
                type = intent.extras.getString(ARGUMENT_TYPE)
            )
        }

        fun create(memo: Memo): DialogFragment {
            return MemoEditDialog().apply {
                arguments = Bundle().apply {
                    putString(ARGUMENT_VALUE, memo.value)
                    putString(ARGUMENT_TYPE, memo.type ?: "text")
                }
            }
        }
    }
}

class MinBalanceExplanationDialog : DialogFragment() {

    private val compositeDisposable = CompositeDisposable()

    init {
        setStyle(STYLE_NO_FRAME, R.style.FullscreenDialog)
    }

    private val xlmDefaultAccountManager: DefaultAccountDataManager by scopedInject()
    private val prefs: CurrencyPrefs by inject()
    private val exchangeRates: ExchangeRateDataManager by scopedInject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.dialog_min_balance_explainer,
        container,
        false
    ).apply {
        isFocusableInTouchMode = true
        requestFocus()
        dialog?.window?.setWindowAnimations(R.style.DialogNoAnimations)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar_general)
        toolbar.setTitle(R.string.minimum_balance_explanation_title)
        toolbar.setNavigationOnClickListener { dismiss() }

        view.findViewById<Button>(R.id.button_continue).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.minimum_balance_url))))
        }
    }

    override fun onResume() {
        super.onResume()
        val progressBar = view?.findViewById<View>(R.id.progress_bar_funds)!!
        progressBar.visible()
        compositeDisposable += xlmDefaultAccountManager.getBalanceAndMin()
            .map {
                Values(
                    it.minimumBalance,
                    it.balance,
                    CryptoValue.lumensFromStroop(100.toBigInteger()) // Tech debt AND-1663 Repeated Hardcoded fee
                )
            }
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn {
                Values(
                    CryptoValue.ZeroXlm,
                    CryptoValue.ZeroXlm,
                    CryptoValue.ZeroXlm
                )
            }
            .doFinally { progressBar.invisible() }
            .subscribeBy {
                view?.run {
                    updateText(R.id.textview_balance, it.balance)
                    updateText(R.id.textview_reserve, it.min)
                    updateText(R.id.textview_fee, it.fee)
                    updateText(R.id.textview_spendable, it.spendable)
                    findViewById<View>(R.id.linearLayout_funds).visible()
                }
            }
    }

    private fun View.updateText(@IdRes textViewId: Int, value: Money) {
        findViewById<TextView>(textViewId).text = formatWithFiat(value)
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    private fun formatWithFiat(
        value: Money
    ) = value.toStringWithSymbol() + " " +
        value.toFiat(exchangeRates, prefs.selectedFiatCurrency).toStringWithSymbol()
}

private class Values(val min: CryptoValue, val balance: CryptoValue, val fee: CryptoValue) {
    val spendable: Money = balance - min - fee
}
