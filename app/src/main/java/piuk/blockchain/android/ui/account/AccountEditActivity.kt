package piuk.blockchain.android.ui.account

import androidx.appcompat.app.AppCompatActivity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.ImageView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import com.blockchain.koin.scopedInject
import com.blockchain.ui.dialog.MaterialProgressDialog
import com.blockchain.ui.password.SecondPasswordHandler
import info.blockchain.balance.CryptoCurrency
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityAccountEditBinding
import piuk.blockchain.android.ui.shortcuts.LauncherShortcutHelper
import piuk.blockchain.android.ui.zxing.CaptureActivity
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.events.ActionEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.android.scan.QrScanHandler
import piuk.blockchain.android.ui.transfer.send.activity.ConfirmPaymentDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.getTextString
import piuk.blockchain.androidcoreui.utils.extensions.toast

class AccountEditActivity : BaseMvpActivity<AccountEditView, AccountEditPresenter>(),
    AccountEditView {

    override val activityIntent: Intent by unsafeLazy { intent }
    private val dialogRunnable = Runnable {
        if (transactionSuccessDialog?.isShowing == true) {
            transactionSuccessDialog!!.dismiss()
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    private val accountEditPresenter: AccountEditPresenter by scopedInject()
    private val appUtil: AppUtil by inject()
    private val rxBus: RxBus by inject()

    private lateinit var binding: ActivityAccountEditBinding
    private var transactionSuccessDialog: AlertDialog? = null
    private var progress: MaterialProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_account_edit)
        presenter.accountModel = AccountEditModel(this)
        binding.viewModel = accountEditPresenter

        setupToolbar(findViewById<Toolbar>(R.id.toolbar_general), R.string.edit)

        binding.tvTransfer.setOnClickListener {
            if (presenter.transferFundsClickable()) {
                secondPasswordHandler.validate(object : SecondPasswordHandler.ResultListener {
                    override fun onNoSecondPassword() {
                        presenter.onClickTransferFunds()
                    }

                    override fun onSecondPasswordValidated(validatedSecondPassword: String) {
                        presenter.secondPassword = validatedSecondPassword
                        presenter.onClickTransferFunds()
                    }
                })
            }
        }

        onViewReady()
    }

    override fun promptAccountLabel(currentLabel: String?) {
        val etLabel = AppCompatEditText(this).apply {
            inputType = InputType.TYPE_TEXT_FLAG_CAP_WORDS
            filters = arrayOf<InputFilter>(InputFilter.LengthFilter(ADDRESS_LABEL_MAX_LENGTH))
            setHint(R.string.name)
            contentDescription = resources.getString(R.string.content_desc_edit_account_label)
        }
        if (currentLabel != null && currentLabel.length <= ADDRESS_LABEL_MAX_LENGTH) {
            etLabel.setText(currentLabel)
            etLabel.setSelection(currentLabel.length)
        }

        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.edit_wallet_name)
            .setMessage(R.string.edit_wallet_name_helper_text)
            .setView(ViewUtils.getAlertDialogPaddedView(this, etLabel))
            .setCancelable(false)
            .setPositiveButton(R.string.save_name) { _, _ ->
                presenter.updateAccountLabel(etLabel.getTextString())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun showToast(@StringRes message: Int, @ToastCustom.ToastType type: String) {
        toast(message, type)
    }

    override fun setActivityResult(resultCode: Int) = setResult(resultCode)

    override fun onSupportNavigateUp(): Boolean =
        consume { onBackPressed() }

    override fun finishPage() {
        setResult(AppCompatActivity.RESULT_CANCELED)
        finish()
    }

    override fun sendBroadcast(event: ActionEvent) {
        rxBus.emitEvent(ActionEvent::class.java, event)
    }

    override fun startScanActivity() {
        QrScanHandler.requestScanPermissions(
            activity = this,
            rootView = binding.mainLayout
        ) { startCameraIfAvailable() }
    }

    private fun startCameraIfAvailable() {
        if (!appUtil.isCameraOpen) {
            val intent = Intent(this, CaptureActivity::class.java)
            startActivityForResult(intent, SCAN_PRIVX)
        } else {
            toast(R.string.camera_unavailable, ToastCustom.TYPE_ERROR)
        }
    }

    override fun promptPrivateKey(message: String) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.privx_required)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                secondPasswordHandler.validate(object :
                    SecondPasswordHandler.ResultListener {
                    override fun onNoSecondPassword() {
                        startScanActivity()
                    }

                    override fun onSecondPasswordValidated(validatedSecondPassword: String) {
                        presenter.secondPassword = validatedSecondPassword
                        startScanActivity()
                    }
                })
            }
            .setNegativeButton(android.R.string.cancel, null).show()
    }

    override fun showPaymentDetails(details: PaymentConfirmationDetails) {
        ConfirmPaymentDialog.newInstance(details, null, null, false,
            { presenter.submitPayment() })
            .show(supportFragmentManager, ConfirmPaymentDialog::class.java.simpleName)

        if (details.isLargeTransaction) {
            binding.root.postDelayed({ onShowLargeTransactionWarning() }, 500)
        }
    }

    override fun hideMerchantCopy() {
        binding.tvExtendedXpubDescription.setText(R.string.extended_public_key_description_bch_only)
    }

    private fun onShowLargeTransactionWarning() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setCancelable(false)
            .setTitle(R.string.warning)
            .setMessage(R.string.large_tx_warning)
            .setPositiveButton(R.string.accept_higher_fee, null)
            .create()
            .show()
    }

    override fun promptArchive(title: String, message: String) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.common_yes) { _, _ -> presenter.archiveAccount() }
            .setNegativeButton(R.string.common_no, null)
            .show()
    }

    override fun promptBIP38Password(data: String) {
        val password = AppCompatEditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.bip38_password_entry)
            .setView(ViewUtils.getAlertDialogPaddedView(this, password))
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                presenter.importBIP38Address(data, password.getTextString())
            }
            .setNegativeButton(android.R.string.cancel, null).show()
    }

    override fun privateKeyImportMismatch() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(getString(R.string.warning))
            .setMessage(
                getString(R.string.private_key_successfully_imported) + "\n\n" + getString(
                    R.string.private_key_not_matching_address
                )
            )
            .setPositiveButton(R.string.import_try_again) { _, _ ->
                presenter.onClickScanXpriv(View(this))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun privateKeyImportSuccess() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.success)
            .setMessage(R.string.private_key_successfully_imported)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun showXpubSharingWarning() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.warning)
            .setMessage(R.string.xpub_sharing_warning)
            .setCancelable(false)
            .setPositiveButton(R.string.dialog_continue) { _, _ -> presenter.showAddressDetails() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun showAddressDetails(
        heading: String?,
        note: String?,
        copy: String?,
        bitmap: Bitmap?,
        qrString: String?
    ) {
        val view = View.inflate(this, R.layout.dialog_view_qr, null)
        val imageView = view.findViewById<View>(R.id.imageview_qr) as ImageView
        imageView.setImageBitmap(bitmap)

        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(heading)
            .setMessage(note)
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(copy) { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip: ClipData = ClipData.newPlainText("Send address", qrString)
                toast(R.string.copied_to_clipboard)
                clipboard.primaryClip = clip
            }
            .create()
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        data?.let {
            if (requestCode == SCAN_PRIVX && resultCode == AppCompatActivity.RESULT_OK) {
                presenter.handleIncomingScanIntent(data)
            }
        } ?: toast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
    }

    override fun showTransactionSuccess() {
        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = View.inflate(this, R.layout.modal_transaction_success, null)
        transactionSuccessDialog = dialogBuilder.setView(dialogView)
            .setPositiveButton(getString(R.string.done)) { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener { finish() }
            .create()

        transactionSuccessDialog!!.show()

        dialogView.postDelayed(dialogRunnable, (5 * 1000).toLong())
    }

    override fun showProgressDialog(@StringRes message: Int) {
        dismissProgressDialog()

        progress = MaterialProgressDialog(this).apply {
            setMessage(message)
            show()
        }
    }

    override fun dismissProgressDialog() {
        if (progress?.isShowing == true) {
            progress!!.dismiss()
            progress = null
        }
    }

    override fun updateAppShortcuts() {
        LauncherShortcutHelper(this).generateReceiveShortcuts()
    }

    override fun createPresenter() = accountEditPresenter

    override fun getView() = this

    companion object {

        internal const val EXTRA_ACCOUNT_INDEX = "piuk.blockchain.android.EXTRA_ACCOUNT_INDEX"
        internal const val EXTRA_ADDRESS_INDEX = "piuk.blockchain.android.EXTRA_ADDRESS_INDEX"
        internal const val EXTRA_CRYPTOCURRENCY = "piuk.blockchain.android.EXTRA_CRYPTOCURRENCY"

        private const val ADDRESS_LABEL_MAX_LENGTH = 17
        private const val SCAN_PRIVX = 302

        fun startForResult(
            activity: AppCompatActivity,
            accountIndex: Int,
            addressIndex: Int,
            cryptoCurrency: CryptoCurrency,
            requestCode: Int
        ) {
            Intent(activity, AccountEditActivity::class.java).apply {
                putExtra(EXTRA_ACCOUNT_INDEX, accountIndex)
                putExtra(EXTRA_ADDRESS_INDEX, addressIndex)
                putExtra(EXTRA_CRYPTOCURRENCY, cryptoCurrency)
            }.run { activity.startActivityForResult(this, requestCode) }
        }
    }
}
