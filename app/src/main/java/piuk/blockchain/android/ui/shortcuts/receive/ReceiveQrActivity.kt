package piuk.blockchain.android.ui.shortcuts.receive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityReceiveQrBinding
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.shortcuts.LauncherShortcutHelper
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity

internal class ReceiveQrActivity :
    BaseMvpActivity<ReceiveQrView, ReceiveQrPresenter>(), ReceiveQrView {

    private val receiveQrPresenter: ReceiveQrPresenter by scopedInject()
    private val binding: ActivityReceiveQrBinding by lazy {
        ActivityReceiveQrBinding.inflate(layoutInflater)
    }

    override val pageIntent: Intent
        get() = intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        onViewReady()
        logShortcutUse()
        with(binding) {
            actionDone.setOnClickListener { finish() }
            actionCopy.setOnClickListener { presenter.onCopyClicked() }
        }
    }

    override fun lockScreenOrientation() {
        // No-op
    }

    override fun setAddressInfo(addressInfo: String) {
        binding.addressInfo.text = addressInfo
    }

    override fun setAddressLabel(label: String) {
        binding.accountName.text = label
    }

    override fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType)
    }

    override fun setImageBitmap(bitmap: Bitmap) {
        binding.imageviewQr.setImageBitmap(bitmap)
    }

    override fun finishActivity() {
        finish()
    }

    override fun showClipboardWarning(receiveAddressString: String) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.receive_address_to_clipboard)
            .setCancelable(false)
            .setPositiveButton(R.string.common_yes) { _, _ ->
                val clipboard =
                    getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip =
                    ClipData.newPlainText("Send address", receiveAddressString)
                ToastCustom.makeText(
                    this,
                    getString(R.string.copied_to_clipboard),
                    ToastCustom.LENGTH_LONG,
                    ToastCustom.TYPE_GENERAL
                )
                clipboard.setPrimaryClip(clip)
            }
            .setNegativeButton(R.string.common_no, null)
            .show()
    }

    override fun startLogoutTimer() {
        // No-op
    }

    override fun createPresenter(): ReceiveQrPresenter? {
        return receiveQrPresenter
    }

    override fun getView(): ReceiveQrView {
        return this
    }

    private fun logShortcutUse() {
        LauncherShortcutHelper(this)
            .logShortcutUsed(LauncherShortcutHelper.SHORTCUT_ID_QR)
    }

    companion object {
        const val INTENT_EXTRA_ADDRESS = "extra_address"
        const val INTENT_EXTRA_LABEL = "extra_label"
    }
}
