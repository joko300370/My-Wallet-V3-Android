package piuk.blockchain.android.ui.createwallet

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.TransitionManager
import com.blockchain.koin.scopedInject
import com.blockchain.ui.urllinks.URL_PRIVACY_POLICY
import com.blockchain.ui.urllinks.URL_TOS_POLICY
import com.jakewharton.rxbinding2.widget.RxTextView
import kotlinx.android.synthetic.main.activity_create_wallet.*
import kotlinx.android.synthetic.main.toolbar_general.*
import kotlinx.android.synthetic.main.view_password_strength.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.customviews.dialogs.MaterialProgressDialog
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.getTextString
import piuk.blockchain.androidcoreui.utils.extensions.toast

class CreateWalletActivity : BaseMvpActivity<CreateWalletView, CreateWalletPresenter>(),
    CreateWalletView,
    View.OnFocusChangeListener {

    private val stringUtils: StringUtils by inject()
    private val createWalletPresenter: CreateWalletPresenter by scopedInject()
    private var progressDialog: MaterialProgressDialog? = null
    private var applyConstraintSet: ConstraintSet = ConstraintSet()

    private val recoveryPhrase: String by unsafeLazy {
        intent.getStringExtra(RECOVERY_PHRASE) ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_wallet)
        applyConstraintSet.clone(mainConstraintLayout)

        if (recoveryPhrase.isNotEmpty()) {
            setupToolbar(toolbar_general, R.string.recover_funds)
            command_next.setText(R.string.dialog_continue)
        } else {
            setupToolbar(toolbar_general, R.string.new_account_title)
            command_next.setText(R.string.new_account_cta_text)
        }

        tos.movementMethod = LinkMovementMethod.getInstance() // make link clickable
        command_next.isClickable = false
        entropy_container.pass_strength_bar.max = 100 * 10

        wallet_pass.onFocusChangeListener = this
        RxTextView.afterTextChangeEvents(wallet_pass)
            .doOnNext {
                showEntropyContainer()
                presenter.logEventPasswordOneClicked()
                presenter.calculateEntropy(it.editable().toString())
                hideShowCreateButton(
                    it.editable().toString().length,
                    wallet_pass_confirm.getTextString().length
                )
            }
            .emptySubscribe()

        RxTextView.afterTextChangeEvents(wallet_pass_confirm)
            .doOnNext {
                presenter.logEventPasswordTwoClicked()
                hideShowCreateButton(
                    wallet_pass.getTextString().length,
                    it.editable().toString().length
                )
            }
            .emptySubscribe()

        email_address.setOnClickListener { presenter.logEventEmailClicked() }
        command_next.setOnClickListener { onNextClicked() }

        updateTosAndPrivacyLinks()

        wallet_pass_confirm.setOnEditorActionListener { _, i, _ ->
            consume { if (i == EditorInfo.IME_ACTION_GO) onNextClicked() }
        }

        hideEntropyContainer()

        onViewReady()
    }

    private fun updateTosAndPrivacyLinks() {
        val linksMap = mapOf<String, Uri>(
            "terms" to Uri.parse(URL_TOS_POLICY),
            "privacy" to Uri.parse(URL_PRIVACY_POLICY)
        )

        val tosText = stringUtils.getStringWithMappedAnnotations(
            R.string.you_agree_terms_of_service,
            linksMap,
            this
        )

        tos.text = tosText
        tos.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun hideShowCreateButton(password1Length: Int, password2Length: Int) {
        if (password1Length > 0 && password1Length == password2Length) {
            showCreateWalletButton()
        } else {
            hideCreateWalletButton()
        }
    }

    override fun getView() = this

    override fun createPresenter() = createWalletPresenter

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun hideEntropyContainer() {
        TransitionManager.beginDelayedTransition(mainConstraintLayout)
        applyConstraintSet.setVisibility(R.id.entropy_container, ConstraintSet.GONE)
        applyConstraintSet.applyTo(mainConstraintLayout)
    }

    private fun showEntropyContainer() {
        TransitionManager.beginDelayedTransition(mainConstraintLayout)
        applyConstraintSet.setVisibility(R.id.entropy_container, ConstraintSet.VISIBLE)
        applyConstraintSet.applyTo(mainConstraintLayout)
    }

    private fun showCreateWalletButton() {
        TransitionManager.beginDelayedTransition(mainConstraintLayout)
        applyConstraintSet.setVisibility(R.id.command_next, ConstraintSet.VISIBLE)
        applyConstraintSet.applyTo(mainConstraintLayout)
    }

    private fun hideCreateWalletButton() {
        TransitionManager.beginDelayedTransition(mainConstraintLayout)
        applyConstraintSet.setVisibility(R.id.command_next, ConstraintSet.GONE)
        applyConstraintSet.applyTo(mainConstraintLayout)
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) = when {
        hasFocus -> showEntropyContainer()
        else -> hideEntropyContainer()
    }

    override fun setEntropyStrength(score: Int) {
        entropy_container.setStrengthProgress(score)
    }

    override fun setEntropyLevel(level: Int) {
        entropy_container.updateLevelUI(level)
    }

    override fun showError(message: Int) {
        toast(message, ToastCustom.TYPE_ERROR)
    }

    override fun warnWeakPassword(email: String, password: String) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.weak_password)
            .setPositiveButton(R.string.common_retry) { _, _ ->
                wallet_pass.setText("")
                wallet_pass_confirm.setText("")
                wallet_pass.requestFocus()
            }.show()
    }

    override fun startPinEntryActivity() {
        ViewUtils.hideKeyboard(this)
        PinEntryActivity.startAfterWalletCreation(this)
    }

    override fun showProgressDialog(message: Int) {
        dismissProgressDialog()
        progressDialog = MaterialProgressDialog(this).apply {
            setCancelable(false)
            setMessage(getString(message))
            if (!isFinishing) show()
        }
    }

    override fun dismissProgressDialog() {
        progressDialog?.apply {
            dismiss()
            progressDialog = null
        }
    }

    override fun getDefaultAccountName(): String = getString(R.string.btc_default_wallet_name)

    override fun enforceFlagSecure() = true

    private fun onNextClicked() {
        val email = email_address.text.toString().trim()
        val password1 = wallet_pass.text.toString()
        val password2 = wallet_pass_confirm.text.toString()

        if (presenter.validateCredentials(email, password1, password2)) {
            presenter.createOrRestoreWallet(email, password1, recoveryPhrase)
        }
    }

    companion object {
        const val RECOVERY_PHRASE = "RECOVERY_PHRASE"

        fun start(context: Context) {
            context.startActivity(Intent(context, CreateWalletActivity::class.java))
        }
    }
}