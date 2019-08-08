package piuk.blockchain.android.ui.receive

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.CoordinatorLayout
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.blockchain.serialization.JsonSerializableAccount
import com.blockchain.ui.chooser.AccountChooserActivity
import com.blockchain.ui.chooser.AccountMode
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.alert_watch_only_spend.view.confirm_cancel
import kotlinx.android.synthetic.main.alert_watch_only_spend.view.confirm_continue
import kotlinx.android.synthetic.main.alert_watch_only_spend.view.confirm_dont_ask_again
import kotlinx.android.synthetic.main.fragment_receive.amount_container
import kotlinx.android.synthetic.main.fragment_receive.button_request
import kotlinx.android.synthetic.main.fragment_receive.currency_header
import kotlinx.android.synthetic.main.fragment_receive.custom_keyboard
import kotlinx.android.synthetic.main.fragment_receive.divider1
import kotlinx.android.synthetic.main.fragment_receive.divider3
import kotlinx.android.synthetic.main.fragment_receive.divider_to
import kotlinx.android.synthetic.main.fragment_receive.image_qr
import kotlinx.android.synthetic.main.fragment_receive.progressbar
import kotlinx.android.synthetic.main.fragment_receive.scrollview
import kotlinx.android.synthetic.main.fragment_receive.textview_receiving_address
import kotlinx.android.synthetic.main.fragment_receive.to_container
import kotlinx.android.synthetic.main.include_amount_row.amountCrypto
import kotlinx.android.synthetic.main.include_amount_row.amountFiat
import kotlinx.android.synthetic.main.include_amount_row.currencyCrypto
import kotlinx.android.synthetic.main.include_amount_row.currencyFiat
import kotlinx.android.synthetic.main.include_amount_row.view.amountCrypto
import kotlinx.android.synthetic.main.include_amount_row.view.amountFiat
import kotlinx.android.synthetic.main.include_to_row.constraint_layout_to_row
import kotlinx.android.synthetic.main.include_to_row.toAddressTextView
import kotlinx.android.synthetic.main.include_to_row.toArrowImage
import kotlinx.android.synthetic.main.view_expanding_currency_header.textview_selected_currency
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.balance.BalanceFragment
import piuk.blockchain.android.ui.customviews.callbacks.OnTouchOutsideViewListener
import piuk.blockchain.android.ui.home.HomeFragment
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.util.EditTextFormatUtil
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.ToolBarActivity
import piuk.blockchain.androidcoreui.ui.customviews.NumericKeyboardCallback
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.AppUtil
import piuk.blockchain.androidcoreui.utils.extensions.disableSoftKeyboard
import piuk.blockchain.androidcoreui.utils.extensions.getTextString
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.helperfunctions.AfterTextChangedWatcher
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.concurrent.TimeUnit

class ReceiveFragment : HomeFragment<ReceiveView, ReceivePresenter>(),
    ReceiveView,
    NumericKeyboardCallback {

    override val locale: Locale = Locale.getDefault()

    private val currencyState: CurrencyState by inject()
    private val appUtil: AppUtil by inject()
    private val receivePresenter: ReceivePresenter by inject()

    private var bottomSheetDialog: BottomSheetDialog? = null

    private var textChangeAllowed = true
    private var textChangeSubject = PublishSubject.create<String>()
    private var selectedAccountPosition = -1
    private var handlingActivityResult = false

    private val intentFilter = IntentFilter(BalanceFragment.ACTION_INTENT)
    private val defaultDecimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator.toString()

    private val receiveIntentHelper by unsafeLazy {
        ReceiveIntentHelper(context!!, appUtil)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BalanceFragment.ACTION_INTENT) {
                presenter?.apply {
                    // Update UI with new Address + QR
                    onResume(selectedAccountPosition)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        arguments?.run {
            selectedAccountPosition = getInt(ARG_SELECTED_ACCOUNT_POSITION)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_receive)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.apply {
            (activity as MainActivity).setOnTouchOutsideViewListener(
                currency_header,
                object : OnTouchOutsideViewListener {
                    override fun onTouchOutside(view: View, event: MotionEvent) {
                        currency_header.close()
                    }
                }
            )
        }

        onViewReady()
        setupLayout()
        setCustomKeypad()

        scrollview?.post { scrollview?.scrollTo(0, 0) }

        currency_header.setSelectionListener { currency ->
            when (currency) {
                CryptoCurrency.BTC -> presenter?.onSelectDefault(selectedAccountPosition)
                CryptoCurrency.ETHER -> presenter?.onEthSelected()
                CryptoCurrency.BCH -> presenter?.onSelectBchDefault()
                CryptoCurrency.XLM -> presenter?.onXlmSelected()
                CryptoCurrency.PAX -> presenter?.onPaxSelected()
            }
        }
    }

    private fun setupToolbar() {
        val supportActionBar = (activity as AppCompatActivity).supportActionBar
        if (supportActionBar != null) {
            (activity as ToolBarActivity).setupToolbar(
                supportActionBar, R.string.request
            )
        } else {
            finishPage()
        }
    }

    override fun disableCurrencyHeader() {
        textview_selected_currency?.apply {
            isClickable = false
        }
    }

    private fun setupLayout() {
        if (!presenter.shouldShowAccountDropdown()) {
            constraint_layout_to_row.gone()
            divider_to.gone()
        }

        // BTC Field
        amountCrypto.apply {
            hint = "0${defaultDecimalSeparator}00"
            addTextChangedListener(btcTextWatcher)
            disableSoftKeyboard()
        }

        // Fiat Field
        amountFiat.apply {
            hint = "0${defaultDecimalSeparator}00"
            addTextChangedListener(fiatTextWatcher)
            disableSoftKeyboard()
        }

        updateUnits()

        // QR Code
        image_qr.apply {
            setOnClickListener { showClipboardWarning() }
            setOnLongClickListener { consume { onShareClicked() } }
        }

        // Receive address
        textview_receiving_address.setOnClickListener { showClipboardWarning() }

        val toListener: (View) -> Unit = {
            val currency = currencyState.cryptoCurrency
            AccountChooserActivity.startForResult(
                this,
                AccountMode.CryptoAccountMode(currency),
                if (currency == CryptoCurrency.BTC) {
                    REQUEST_CODE_RECEIVE_BITCOIN
                } else {
                    REQUEST_CODE_RECEIVE_BITCOIN_CASH
                },
                getString(R.string.to)
            )
        }

        toAddressTextView.setOnClickListener(toListener)
        toArrowImage.setOnClickListener(toListener)

        textChangeSubject.debounce(300, TimeUnit.MILLISECONDS)
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { presenter.onBitcoinAmountChanged(getBtcAmount()) }
            .emptySubscribe()

        button_request.setOnClickListener {
            onShareClicked()
        }
    }

    private fun updateUnits() {
        currencyCrypto.text = presenter.cryptoUnit
        currencyFiat.text = presenter.fiatUnit
    }

    private val btcTextWatcher = object : AfterTextChangedWatcher() {
        override fun afterTextChanged(s: Editable?) {
            var editable = s
            amountCrypto.removeTextChangedListener(this)
            editable = EditTextFormatUtil.formatEditable(
                editable,
                presenter.getMaxCryptoDecimalLength(),
                amountCrypto,
                defaultDecimalSeparator
            )

            amountCrypto.addTextChangedListener(this)

            if (textChangeAllowed) {
                textChangeAllowed = false
                presenter.updateFiatTextField(editable.toString())
                textChangeSubject.onNext(editable.toString())
                textChangeAllowed = true
            }
        }
    }

    private val fiatTextWatcher = object : AfterTextChangedWatcher() {
        override fun afterTextChanged(s: Editable) {
            var editable = s
            amountFiat.removeTextChangedListener(this)
            val maxLength = 2
            editable = EditTextFormatUtil.formatEditable(
                editable,
                maxLength,
                amountFiat,
                defaultDecimalSeparator
            )

            amountFiat.addTextChangedListener(this)

            if (textChangeAllowed) {
                textChangeAllowed = false
                presenter.updateBtcTextField(editable.toString())
                textChangeSubject.onNext(editable.toString())
                textChangeAllowed = true
            }
        }
    }

    override fun getBtcAmount() = amountCrypto.getTextString()

    override fun updateReceiveAddress(address: String) {
        if (!isRemoving) {
            textview_receiving_address.text = address
        }
    }

    override fun updateFiatTextField(text: String) {
        amountFiat.setText(text)
    }

    override fun updateBtcTextField(text: String) {
        amountCrypto.setText(text)
    }

    override fun onResume() {
        super.onResume()

        if (!handlingActivityResult)
            presenter.onResume(selectedAccountPosition)

        handlingActivityResult = false

        closeKeypad()
        setupToolbar()
        LocalBroadcastManager.getInstance(context!!)
            .registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun showQrLoading() {
        image_qr.invisible()
        textview_receiving_address.invisible()
        progressbar.visible()
    }

    override fun showQrCode(bitmap: Bitmap?) {
        if (!isRemoving) {
            progressbar.invisible()
            image_qr.visible()
            textview_receiving_address.visible()
            image_qr.setImageBitmap(bitmap)
        }
    }

    private fun displayBitcoinLayout() {
        divider1.visible()
        amount_container.visible()
        divider3.visible()

        if (presenter.shouldShowAccountDropdown()) {
            to_container.visible()
            divider_to.visible()
        } else {
            to_container.gone()
            divider_to.gone()
        }
    }

    private fun displayEtherLayout() {
        custom_keyboard.hideKeyboard()
        divider1.gone()
        amount_container.gone()
        divider_to.gone()
        to_container.gone()
        divider3.gone()
    }

    private fun displayERC20Layout() {
        displayEtherLayout()
    }

    private fun displayXlmLayout() {
        custom_keyboard.hideKeyboard()
        divider1.gone()
        amount_container.gone()
        divider_to.gone()
        to_container.gone()
        divider3.gone()
    }

    private fun displayBitcoinCashLayout() {
        custom_keyboard.hideKeyboard()
        divider1.gone()
        amount_container.gone()
        divider3.visible()

        if (presenter.shouldShowAccountDropdown()) {
            to_container.visible()
            divider_to.visible()
        } else {
            to_container.gone()
            divider_to.gone()
        }
    }

    override fun setSelectedCurrency(cryptoCurrency: CryptoCurrency) {
        currency_header.setCurrentlySelectedCurrency(cryptoCurrency)
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> displayBitcoinLayout()
            CryptoCurrency.ETHER -> displayEtherLayout()
            CryptoCurrency.BCH -> displayBitcoinCashLayout()
            CryptoCurrency.XLM -> displayXlmLayout()
            CryptoCurrency.PAX -> displayERC20Layout()
        }
        updateUnits()
    }

    override fun updateReceiveLabel(label: String) {
        toAddressTextView.text = label
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        handlingActivityResult = true

        // Set receiving account
        if (resultCode == Activity.RESULT_OK &&
            requestCode == REQUEST_CODE_RECEIVE_BITCOIN &&
            data != null
        ) {
            when (val account = unpackAccountResult(data)) {
                is LegacyAddress -> presenter.onLegacyAddressSelected(account)
                is Account -> presenter.onAccountBtcSelected(account)
                else -> presenter.onSelectDefault(selectedAccountPosition)
            }
        } else if (resultCode == Activity.RESULT_OK &&
            requestCode == REQUEST_CODE_RECEIVE_BITCOIN_CASH &&
            data != null
        ) {
            when (val account = unpackAccountResult(data)) {
                is LegacyAddress -> presenter.onLegacyBchAddressSelected(account)
                is GenericMetadataAccount -> presenter.onBchAccountSelected(account)
                else -> presenter.onSelectBchDefault()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun unpackAccountResult(intent: Intent?): JsonSerializableAccount? =
        AccountChooserActivity.unpackAccountResult(intent)

    override fun showShareBottomSheet(uri: String) {
        receiveIntentHelper.getIntentDataList(uri, getQrBitmap(), currencyState.cryptoCurrency).let {
            val adapter = ShareReceiveIntentAdapter(it).apply {
                itemClickedListener = { bottomSheetDialog?.dismiss() }
            }

            val sheetView = View.inflate(activity, R.layout.bottom_sheet_receive, null)
            sheetView.findViewById<RecyclerView>(R.id.recycler_view).apply {
                this.adapter = adapter
                layoutManager = LinearLayoutManager(context)
            }

            bottomSheetDialog = BottomSheetDialog(context!!, R.style.BottomSheetDialog).apply {
                setContentView(sheetView)
            }

            adapter.notifyDataSetChanged()
        }

        bottomSheetDialog?.apply { show() }
        if (bottomSheetDialog == null) {
            toast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
        }
    }

    private fun onShareClicked() {
        activity?.run {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_share)
                .setCancelable(false)
                .setPositiveButton(R.string.yes) { _, _ -> requestStoragePermissionIfNeeded() }
                .setNegativeButton(R.string.no, null)
                .show()
        }
    }

    private fun requestStoragePermissionIfNeeded() {
        presenter.onShowBottomShareSheetSelected()
    }

    override fun showWatchOnlyWarning() {
        activity?.run {
            val dialogView = layoutInflater.inflate(R.layout.alert_watch_only_spend, null)
            val alertDialog = AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setView(dialogView.rootView)
                .setCancelable(false)
                .create()

            dialogView.confirm_cancel.setOnClickListener {
                presenter.onSelectDefault(selectedAccountPosition)
                presenter.setWarnWatchOnlySpend(!dialogView.confirm_dont_ask_again.isChecked)
                alertDialog.dismiss()
            }

            dialogView.confirm_continue.setOnClickListener {
                presenter.setWarnWatchOnlySpend(!dialogView.confirm_dont_ask_again.isChecked)
                alertDialog.dismiss()
            }

            alertDialog.show()
        }
    }

    override fun getQrBitmap(): Bitmap = (image_qr.drawable as BitmapDrawable).bitmap

    override fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        toast(message, toastType)
    }

    fun getSelectedAccountPosition(): Int = presenter.getSelectedAccountPosition()

    private fun showClipboardWarning() {
        val address = textview_receiving_address.text
        activity?.run {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_clipboard)
                .setCancelable(false)
                .setPositiveButton(R.string.yes) { _, _ ->
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Send address", address)
                    toast(R.string.copied_to_clipboard)
                    clipboard.primaryClip = clip
                }
                .setNegativeButton(R.string.no, null)
                .show()
        }
    }

    override fun onBackPressed(): Boolean =
        when {
            isKeyboardVisible() -> {
                closeKeypad()
                true
            }
            currency_header.isOpen() -> {
                currency_header.close()
                true
            }
            else -> false
        }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(context!!).unregisterReceiver(broadcastReceiver)
        currency_header?.close()
    }

    override fun finishPage() {
        navigator().gotoDashboard()
    }

    private fun setCustomKeypad() {
        custom_keyboard.apply {
            setCallback(this@ReceiveFragment)
            setDecimalSeparator(defaultDecimalSeparator)
            // Enable custom keypad and disables default keyboard from popping up
            enableOnView(amount_container.amountCrypto)
            enableOnView(amount_container.amountFiat)
        }

        amount_container.amountCrypto.apply {
            setText("")
            requestFocus()
        }
    }

    private fun closeKeypad() {
        custom_keyboard.setNumpadVisibility(View.GONE)
    }

    private fun isKeyboardVisible(): Boolean = custom_keyboard.isVisible

    override fun createPresenter() = receivePresenter

    override fun getMvpView() = this

    override fun onKeypadClose() {
        // Show bottom nav if applicable
        navigator().showNavigation()

        val height = activity!!.resources.getDimension(R.dimen.action_bar_height).toInt()
        // Resize activity to default
        scrollview.apply {
            setPadding(0, 0, 0, 0)
            layoutParams = CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply { setMargins(0, height, 0, height) }

            postDelayed({ smoothScrollTo(0, 0) }, 100)
        }
    }

    override fun onKeypadOpen() {
        currency_header?.close()
        navigator().hideNavigation()
    }

    override fun onKeypadOpenCompleted() {
        // Resize activity around view
        val height = activity!!.resources.getDimension(R.dimen.action_bar_height).toInt()
        scrollview.apply {
            setPadding(0, 0, 0, custom_keyboard.height)
            layoutParams = CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply { setMargins(0, height, 0, 0) }

            scrollTo(0, bottom)
        }
    }

    companion object {

        private const val REQUEST_CODE_RECEIVE_BITCOIN = 800
        private const val REQUEST_CODE_RECEIVE_BITCOIN_CASH = 801

        private const val ARG_SELECTED_ACCOUNT_POSITION = "ARG_SELECTED_ACCOUNT_POSITION"

        @JvmStatic
        fun newInstance(selectedAccountPosition: Int) = ReceiveFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_SELECTED_ACCOUNT_POSITION, selectedAccountPosition)
            }
        }
    }
}
