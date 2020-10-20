package piuk.blockchain.android.ui.start

import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.blockchain.koin.scopedInject
import kotlinx.android.synthetic.main.activity_password_required.*
import org.json.JSONObject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.android.ui.customviews.getTwoFactorDialog
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.android.ui.recover.RecoverFundsActivity
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ViewUtils

class PasswordRequiredActivity : MvpActivity<PasswordRequiredView, PasswordRequiredPresenter>(),
    PasswordRequiredView {

    override val presenter: PasswordRequiredPresenter by scopedInject()
    override val view: PasswordRequiredView = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_password_required)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_general)
        setupToolbar(toolbar, R.string.confirm_password)
        supportActionBar!!.setDisplayHomeAsUpEnabled(false)

        button_continue.setOnClickListener { presenter.onContinueClicked(field_password.text.toString()) }
        button_forget.setOnClickListener { presenter.onForgetWalletClicked() }
        button_recover.setOnClickListener { launchRecoveryFlow() }
    }

    override fun showToast(@StringRes messageId: Int, @ToastCustom.ToastType toastType: String) {
        ToastCustom.makeText(this, getString(messageId), ToastCustom.LENGTH_SHORT, toastType)
    }

    override fun restartPage() {
        val intent = Intent(this, LauncherActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun resetPasswordField() {
        if (!isFinishing) field_password.setText("")
    }

    override fun goToPinPage() {
        startActivity(Intent(this, PinEntryActivity::class.java))
    }

    override fun updateWaitingForAuthDialog(secondsRemaining: Int) =
        updateProgressDialog(getString(R.string.check_email_to_auth_login) + " " + secondsRemaining)

    override fun showForgetWalletWarning(onForgetConfirmed: () -> Unit) {
        showAlert(
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.warning)
                .setMessage(R.string.forget_wallet_warning)
                .setPositiveButton(R.string.forget_wallet) { _, _ -> onForgetConfirmed() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .create()
        )
    }

    override fun showTwoFactorCodeNeededDialog(
        responseObject: JSONObject,
        sessionId: String,
        authType: Int,
        guid: String,
        password: String
    ) {
        ViewUtils.hideKeyboard(this)

        showAlert(
            getTwoFactorDialog(this, authType, positiveAction = {
                presenter.submitTwoFactorCode(responseObject,
                    sessionId,
                    guid,
                    password,
                    it
                )
            }, resendAction = {
                presenter.requestNew2FaCode(password, guid)
            }
            ))
    }

    override fun onDestroy() {
        dismissProgressDialog()
        presenter.cancelAuthTimer()
        super.onDestroy()
    }

    private fun launchRecoveryFlow() = RecoverFundsActivity.start(this)
}
