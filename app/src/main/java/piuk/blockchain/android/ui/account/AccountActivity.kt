package piuk.blockchain.android.ui.account

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.MenuItem
import android.view.MotionEvent
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.ui.dialog.MaterialProgressDialog
import com.blockchain.ui.password.SecondPasswordHandler
import com.google.zxing.BarcodeFormat
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_accounts.*
import kotlinx.android.synthetic.main.toolbar_general.*
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.scan.QrScanHandler
import piuk.blockchain.android.ui.account.AccountPresenter.Companion.ADDRESS_LABEL_MAX_LENGTH
import piuk.blockchain.android.ui.account.adapter.AccountAdapter
import piuk.blockchain.android.ui.account.adapter.AccountHeadersListener
import piuk.blockchain.android.ui.transactionflow.DialogFlow
import piuk.blockchain.android.ui.transactionflow.TransactionFlow
import piuk.blockchain.android.ui.zxing.CaptureActivity
import piuk.blockchain.android.ui.zxing.Intents
import piuk.blockchain.androidcore.data.events.ActionEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.getTextString
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.toast
import java.util.EnumSet
import java.util.Locale

class AccountActivity : BaseMvpActivity<AccountView, AccountPresenter>(),
    AccountView,
    AccountHeadersListener, DialogFlow.FlowHost {

    override val locale: Locale = Locale.getDefault()

    private val rxBus: RxBus by inject()
    private val accountPresenter: AccountPresenter by scopedInject()

    private val accountsAdapter: AccountAdapter by unsafeLazy {
        AccountAdapter(this)
    }

    private var progress: MaterialProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accounts)
        setupToolbar(toolbar_general, R.string.drawer_addresses)
        get<Analytics>().logEvent(AnalyticsEvents.AccountsAndAddresses)

        val displaySet = presenter.getDisplayableCurrencies()

        with(currency_header) {
            CryptoCurrency.values().forEach {
                if (it !in displaySet) hide(it)
            }
            setCurrentlySelectedCurrency(presenter.cryptoCurrency)
            setSelectionListener { presenter.cryptoCurrency = it }
        }

        with(recyclerview_accounts) {
            layoutManager = LinearLayoutManager(this@AccountActivity)
            itemAnimator = null
            setHasFixedSize(true)
            adapter = accountsAdapter
        }
        onViewReady()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> consume { onBackPressed() }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (currency_header.isOpen()) {
            currency_header.close()
        } else {
            super.onBackPressed()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            // Notify touchOutsideViewListeners if user tapped outside a given view
            if (currency_header != null) {
                val viewRect = Rect()
                currency_header.getGlobalVisibleRect(viewRect)
                if (!viewRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    if (currency_header.isOpen()) {
                        currency_header.close()
                        return false
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun startScanForResult() {
        Intent(this, CaptureActivity::class.java).apply {
            putExtra(Intents.Scan.FORMATS, EnumSet.allOf(BarcodeFormat::class.java))
            putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE)
        }.run { startActivityForResult(this, IMPORT_PRIVATE_REQUEST_CODE) }
    }

    override fun onCreateNewClicked() {
        createNewAccount()
    }

    override fun onImportAddressClicked() {
        QrScanHandler.requestScanPermissions(
            activity = this,
            rootView = linear_layout_root
        ) { onScanButtonClicked() }
    }

    override fun onAccountClicked(cryptoCurrency: CryptoCurrency, correctedPosition: Int) {
        onRowClick(cryptoCurrency, correctedPosition)
    }

    private fun onRowClick(cryptoCurrency: CryptoCurrency, position: Int) {
        AccountEditActivity.startForResult(
            this,
            getAccountPosition(cryptoCurrency, position),
            if (position >= presenter.accountSize) position - presenter.accountSize else -1,
            cryptoCurrency,
            EDIT_ACTIVITY_REQUEST_CODE
        )
    }

    private fun getAccountPosition(cryptoCurrency: CryptoCurrency, position: Int): Int =
        if (cryptoCurrency == CryptoCurrency.BTC) {
            if (position < presenter.accountSize) position else -1
        } else {
            position
        }

    private fun onScanButtonClicked() {
        secondPasswordHandler.validate(object : SecondPasswordHandler.ResultListener {
            override fun onNoSecondPassword() {
                presenter.onScanButtonClicked()
            }

            override fun onSecondPasswordValidated(validatedSecondPassword: String) {
                presenter.doubleEncryptionPassword = validatedSecondPassword
                presenter.onScanButtonClicked()
            }
        })
    }

    private fun createNewAccount() {
        secondPasswordHandler.validate(object : SecondPasswordHandler.ResultListener {
            override fun onNoSecondPassword() {
                promptForAccountLabel()
            }

            override fun onSecondPasswordValidated(validatedSecondPassword: String) {
                presenter.doubleEncryptionPassword = validatedSecondPassword
                promptForAccountLabel()
            }
        })
    }

    private fun promptForAccountLabel() {
        val editText = AppCompatEditText(this).apply {
            inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            filters = arrayOf<InputFilter>(InputFilter.LengthFilter(ADDRESS_LABEL_MAX_LENGTH))
            setHint(R.string.name)
        }

        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.create_a_new_wallet)
            .setMessage(R.string.create_a_new_wallet_helper_text)
            .setView(ViewUtils.getAlertDialogPaddedView(this, editText))
            .setCancelable(false)
            .setPositiveButton(R.string.create_now) { _, _ ->
                if (editText.getTextString().trim { it <= ' ' }.isNotEmpty()) {
                    addAccount(editText.getTextString().trim { it <= ' ' })
                } else {
                    toast(R.string.label_cant_be_empty, ToastCustom.TYPE_ERROR)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun addAccount(accountLabel: String) {
        presenter.createNewAccount(accountLabel)
    }

    override fun updateAccountList(displayAccounts: List<AccountItem>) {
        accountsAdapter.items = displayAccounts
    }

    private val event by unsafeLazy {
        rxBus.register(ActionEvent::class.java)
    }

    private val compositeDisposable = CompositeDisposable()

    override fun onResume() {
        super.onResume()
        onViewReady()
    }

    override fun onPause() {
        super.onPause()
        rxBus.unregister(ActionEvent::class.java, event)
        compositeDisposable.clear()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == AppCompatActivity.RESULT_OK &&
            requestCode == IMPORT_PRIVATE_REQUEST_CODE &&
            data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null
        ) {

            val strResult = data.getStringExtra(CaptureActivity.SCAN_RESULT)
            presenter.onAddressScanned(strResult)
            setResult(resultCode)
        } else if (resultCode == AppCompatActivity.RESULT_OK && requestCode == EDIT_ACTIVITY_REQUEST_CODE) {
            onViewReady()
            setResult(resultCode)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun showBip38PasswordDialog(data: String) {
        val password = AppCompatEditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.bip38_password_entry)
            .setView(ViewUtils.getAlertDialogPaddedView(this, password))
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                presenter.importBip38Address(data, password.getTextString())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun showWatchOnlyWarningDialog(address: String) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.warning)
            .setCancelable(false)
            .setMessage(getString(R.string.watch_only_not_supported))
            .setPositiveButton(R.string.ok_cap, null)
            .show()
    }

    override fun showRenameImportedAddressDialog(address: LegacyAddress) {
        val editText = AppCompatEditText(this).apply {
            inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            filters = arrayOf<InputFilter>(InputFilter.LengthFilter(ADDRESS_LABEL_MAX_LENGTH))
            setHint(R.string.name)
        }

        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.label_address)
            .setView(ViewUtils.getAlertDialogPaddedView(this, editText))
            .setCancelable(false)
            .setPositiveButton(R.string.save_name) { _, _ ->
                val label = editText.getTextString()
                if (label.trim { it <= ' ' }.isNotEmpty()) {
                    address.label = label
                }

                remoteSaveNewAddress(address)
            }
            .setNegativeButton(R.string.polite_no) { _, _ -> remoteSaveNewAddress(address) }
            .show()
    }

    override fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        toast(message, toastType)
    }

    private fun remoteSaveNewAddress(legacy: LegacyAddress) {
        presenter.updateLegacyAddress(legacy)
    }

    override fun showTransferFunds(sendingAccount: CryptoAccount, defaultAccount: SingleAccount) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.transfer_funds_title)
            .setMessage(getString(R.string.transfer_funds_description) + "\n")
            .setPositiveButton(R.string.transfer_all) { _, _ ->
                launchFlow(sendingAccount, defaultAccount)
            }
            .setNegativeButton(R.string.not_now) { _, _ ->
            }.show()
    }

    private fun launchFlow(sourceAccount: CryptoAccount, targetAccount: SingleAccount) {
        TransactionFlow(
            sourceAccount = sourceAccount,
            action = AssetAction.NewSend,
            target = targetAccount
        ).apply {
            startFlow(
                fragmentManager = supportFragmentManager,
                host = this@AccountActivity
            )
        }
    }

    override fun onFlowFinished() {
        // do nothing
    }

    override fun showProgressDialog(@StringRes message: Int) {
        dismissProgressDialog()
        if (!isFinishing) {
            progress = MaterialProgressDialog(this).apply {
                setMessage(message)
                setCancelable(false)
                show()
            }
        }
    }

    override fun dismissProgressDialog() {
        if (progress?.isShowing == true) {
            progress!!.dismiss()
            progress = null
        }
    }

    override fun hideCurrencyHeader() {
        currency_header?.apply {
            gone()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissProgressDialog()
    }

    override fun createPresenter() = accountPresenter

    override fun getView() = this

    companion object {

        private const val IMPORT_PRIVATE_REQUEST_CODE = 2006
        private const val EDIT_ACTIVITY_REQUEST_CODE = 2007
    }
}
