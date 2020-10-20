package piuk.blockchain.android.ui.start

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import com.blockchain.koin.scopedInject
import kotlinx.android.synthetic.main.activity_manual_pairing.*
import org.json.JSONObject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.android.ui.customviews.getTwoFactorDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ViewUtils

class ManualPairingActivity : MvpActivity<ManualPairingView, ManualPairingPresenter>(),
    ManualPairingView {

    override val view: ManualPairingView = this
    override val presenter: ManualPairingPresenter by scopedInject()

    private val guid: String
        get() = wallet_id.text.toString()
    private val password: String
        get() = wallet_pass.text.toString()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_pairing)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_general)
        setupToolbar(toolbar, R.string.manual_pairing)

        command_next.setOnClickListener { presenter.onContinueClicked(guid, password) }

        wallet_pass.setOnEditorActionListener { _, i, _ ->
            if (i == EditorInfo.IME_ACTION_GO) {
                presenter.onContinueClicked(guid, password)
            }
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun showToast(@StringRes messageId: Int, @ToastCustom.ToastType toastType: String) {
        ToastCustom.makeText(this, getString(messageId), ToastCustom.LENGTH_SHORT, toastType)
    }

    override fun goToPinPage() {
        startActivity(Intent(this, PinEntryActivity::class.java))
    }

    override fun updateWaitingForAuthDialog(secondsRemaining: Int) {
        updateProgressDialog(getString(R.string.check_email_to_auth_login) + " " + secondsRemaining)
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

    override fun resetPasswordField() {
        if (!isFinishing)
            wallet_pass.setText("")
    }

    public override fun onDestroy() {
        currentFocus?.let {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
        dismissProgressDialog()
        presenter.cancelAuthTimer()
        super.onDestroy()
    }
}
