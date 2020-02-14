package piuk.blockchain.android.ui.launcher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AlertDialog
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.start.PasswordRequiredActivity
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.start.LandingActivity
import piuk.blockchain.android.ui.upgrade.UpgradeWalletActivity
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.utils.extensions.toast
import timber.log.Timber

class LauncherActivity : BaseMvpActivity<LauncherView, LauncherPresenter>(), LauncherView {

    private val launcherPresenter: LauncherPresenter by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        Handler().postDelayed(DelayStartRunnable(this), 500)
    }

    override fun logScreenView() = Unit

    override fun createPresenter() = launcherPresenter

    override fun getView() = this

    override fun getPageIntent(): Intent = intent

    override fun onNoGuid() = LandingActivity.start(this)

    override fun onRequestPin() {
        startSingleActivity(PinEntryActivity::class.java, null)
    }

    override fun onCorruptPayload() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(getString(R.string.not_sane_error))
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                presenter.clearCredentialsAndRestart()
            }
            .show()
    }

    override fun onRequestUpgrade() {
        startActivity(Intent(this, UpgradeWalletActivity::class.java))
        finish()
    }

    override fun onStartMainActivity(uri: Uri?) {
        startSingleActivity(MainActivity::class.java, null, uri)
    }

    override fun startSimpleBuy() {
        val intent = Intent(this, SimpleBuyActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun onReEnterPassword() {
        startSingleActivity(PasswordRequiredActivity::class.java, null)
    }

    override fun showToast(message: Int, toastType: String) = toast(message, toastType)

    private fun startSingleActivity(clazz: Class<*>, extras: Bundle?, uri: Uri? = null) {
        val intent = Intent(this, clazz).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            data = uri
        }
        Timber.d("DeepLink: Starting Activity $clazz with: $uri")
        extras?.let { intent.putExtras(extras) }
        startActivity(intent)
    }

    private class DelayStartRunnable internal constructor(
        private val activity: LauncherActivity
    ) : Runnable {

        override fun run() {
            if (activity.presenter != null && !activity.isFinishing) {
                activity.onViewReady()
            }
        }
    }
}
