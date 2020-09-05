package piuk.blockchain.android.ui.transfer.send.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.koin.scopedInject
import android.Manifest
import android.view.Menu
import android.view.MenuItem
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.SendAnalytics
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.NullCryptoAccount
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.zxing.CaptureActivity
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.CameraPermissionListener
import timber.log.Timber

class SendActivity : BlockchainActivity(), SendFragment.SendFragmentHost {

    override val alwaysDisableScreenshots = false

    private val coincore: Coincore by scopedInject()
    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.temp_activity_send)
        setSupportActionBar(toolbar_general)
        title = getString(R.string.common_send)

        disposables += coincore.findAccountByName(intent.getStringExtra(ACCOUNT_ID))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    val fragment = SendFragment.newInstance(
                        account = it as CryptoAccount
                    )
                    supportFragmentManager.beginTransaction().replace(R.id.content, fragment).commit()
                },
                onError = {
                    Timber.e("Faied to find account for receive")
                }
            )
    }

    override fun onSupportNavigateUp(): Boolean = consume {
        finish()
    }

    override fun actionBackPress() = finish()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_qr_main -> {
                openQrScan()
                analytics.logEvent(SendAnalytics.QRButtonClicked)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openQrScan() {
        analytics.logEvent(object : AnalyticsEvent {
            override val event = "qr_scan_requested"
            override val params = mapOf("fragment" to "SendFragment")
        })

        val deniedPermissionListener = SnackbarOnDeniedPermissionListener.Builder
            .with(coordinator_layout, R.string.request_camera_permission)
            .withButton(android.R.string.ok) { openQrScan() }
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

    private fun startScanActivity() {
        if (!appUtil.isCameraOpen) {
            val intent = Intent(this@SendActivity, CaptureActivity::class.java)
            startActivityForResult(intent, MainActivity.SCAN_URI)
        } else {
            ToastCustom.makeText(
                this@SendActivity,
                getString(R.string.camera_unavailable),
                ToastCustom.LENGTH_SHORT,
                ToastCustom.TYPE_ERROR
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == MainActivity.SCAN_URI) {
            TODO("Complete as part of AND-3464")
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStop() {
        disposables.clear()
        super.onStop()
    }

    companion object {
        private const val ACCOUNT_ID = "PARAM_ACCOUNT_ID"

        fun start(ctx: Context, account: CryptoAccount) {
            require(account !is NullCryptoAccount)
            ctx.startActivity(
                Intent(ctx, SendActivity::class.java).apply {
                    putExtra(ACCOUNT_ID, account.label)
                }
            )
        }
    }
}
