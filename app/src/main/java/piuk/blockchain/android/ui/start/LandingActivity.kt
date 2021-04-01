package piuk.blockchain.android.ui.start

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AlertDialog
import com.blockchain.koin.scopedInject
import com.blockchain.ui.urllinks.WALLET_STATUS_URL
import kotlinx.android.synthetic.main.activity_landing.*
import kotlinx.android.synthetic.main.warning_layout.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.data.connectivity.ConnectivityStatus
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.android.ui.createwallet.CreateWalletActivity
import piuk.blockchain.android.ui.recover.RecoverFundsActivity
import piuk.blockchain.android.util.copyHashOnLongClick
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.visible

class LandingActivity : MvpActivity<LandingView, LandingPresenter>(), LandingView {

    override val presenter: LandingPresenter by scopedInject()
    private val stringUtils: StringUtils by inject()
    override val view: LandingView = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        btn_create.setOnClickListener { launchCreateWalletActivity() }
        btn_login.setOnClickListener { launchLoginActivity() }
        btn_recover.setOnClickListener { showFundRecoveryWarning() }

        if (!ConnectivityStatus.hasConnectivity(this)) {
            showConnectivityWarning()
        } else {
            presenter.checkForRooted()
        }

        text_version.text =
            "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) ${BuildConfig.COMMIT_HASH}"

        text_version.copyHashOnLongClick(this)
    }

    private fun launchCreateWalletActivity() = CreateWalletActivity.start(this)

    private fun launchLoginActivity() =
        startActivity(Intent(this, LoginActivity::class.java))

    private fun startRecoverFundsActivity() = RecoverFundsActivity.start(this)

    private fun showConnectivityWarning() =
        showAlert(AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setMessage(getString(R.string.check_connectivity_exit))
            .setCancelable(false)
            .setNegativeButton(R.string.exit) { _, _ -> finishAffinity() }
            .setPositiveButton(R.string.retry) { _, _ ->
                val intent = Intent(this, LandingActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            .create()
        )

    private fun showFundRecoveryWarning() =
        showAlert(AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.recover_funds_warning_message_1)
            .setPositiveButton(R.string.dialog_continue) { _, _ -> startRecoverFundsActivity() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> clearAlert() }
            .create()
        )

    override fun showIsRootedWarning() =
        showAlert(AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setMessage(R.string.device_rooted)
            .setCancelable(false)
            .setPositiveButton(R.string.dialog_continue) { _, _ -> clearAlert() }
            .create()
        )

    override fun showApiOutageMessage() {
        layout_warning.visible()
        val learnMoreMap = mapOf<String, Uri>("learn_more" to Uri.parse(WALLET_STATUS_URL))
        layout_warning.warning_message.apply {
            movementMethod = LinkMovementMethod.getInstance()
            text = stringUtils.getStringWithMappedAnnotations(
                R.string.wallet_outage_message, learnMoreMap, this@LandingActivity
            )
        }
    }

    override fun showToast(message: String, toastType: String) = toast(message, toastType)

    companion object {
        @JvmStatic
        fun start(context: Context) {
            Intent(context, LandingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(this)
            }
        }
    }
}
