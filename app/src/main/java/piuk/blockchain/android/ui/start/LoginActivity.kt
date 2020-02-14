package piuk.blockchain.android.ui.start

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.toolbar_general.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.zxing.CaptureActivity
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.CameraPermissionListener
import piuk.blockchain.androidcoreui.utils.extensions.toast
import timber.log.Timber

class LoginActivity : MvpActivity<LoginView, LoginPresenter>(), LoginView {

    override val presenter: LoginPresenter by inject()
    override val view: LoginView = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        setupToolbar(toolbar_general, R.string.login_auto_pair_title)

        step_one.text = getString(R.string.pair_wallet_step_1, WEB_WALLET_URL_PROD)

        btn_manual_pair.setOnClickListener { onClickManualPair() }
        btn_scan_qr.setOnClickListener { requestCameraPermissionIfNeeded() }
    }

    override fun showToast(message: Int, toastType: String) = toast(message, toastType)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == AppCompatActivity.RESULT_OK && requestCode == PAIRING_QR) {
            if (data?.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {
                presenter.pairWithQR(data.getStringExtra(CaptureActivity.SCAN_RESULT))
            }
        }
    }

    override fun startPinEntryActivity() {
        val intent = Intent(this, PinEntryActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onSupportNavigateUp() = consume { onBackPressed() }

    private fun requestCameraPermissionIfNeeded() {
        val deniedPermissionListener = SnackbarOnDeniedPermissionListener.Builder
            .with(main_layout, R.string.request_camera_permission)
            .withButton(android.R.string.ok) { requestCameraPermissionIfNeeded() }
            .build()

        val grantedPermissionListener = CameraPermissionListener(analytics, {
            startScanActivity()
        })

        val compositePermissionListener =
            CompositePermissionListener(deniedPermissionListener, grantedPermissionListener)

        Dexter.withActivity(this)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(compositePermissionListener)
            .withErrorListener { error -> Timber.wtf("Dexter permissions error $error") }
            .check()
    }

    private fun onClickManualPair() {
        startActivity(Intent(this, ManualPairingActivity::class.java))
    }

    private fun startScanActivity() {
        if (!appUtil.isCameraOpen) {
            val intent = Intent(this, CaptureActivity::class.java)
            intent.putExtra("SCAN_FORMATS", "QR_CODE")
            startActivityForResult(intent, PAIRING_QR)
        } else {
            showToast(R.string.camera_unavailable, ToastCustom.TYPE_ERROR)
        }
    }

    companion object {
        private const val WEB_WALLET_URL_PROD = "https://login.blockchain.com/"
        const val PAIRING_QR = 2005
    }
}