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
import com.blockchain.ui.urllinks.URL_BACKUP_INFO
import com.blockchain.ui.urllinks.URL_PRIVACY_POLICY
import com.blockchain.ui.urllinks.URL_TOS_POLICY
import com.blockchain.wallet.DefaultLabels
import com.jakewharton.rxbinding2.widget.RxTextView
import info.blockchain.balance.CryptoCurrency
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityCreateWalletBinding
import piuk.blockchain.android.databinding.ViewPasswordStrengthBinding
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.dialogs.MaterialProgressDialog
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.ViewUtils
import piuk.blockchain.android.util.getTextString
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity

class CreateWalletActivity : BaseMvpActivity<CreateWalletView, CreateWalletPresenter>(),
    CreateWalletView,
    View.OnFocusChangeListener {

    private val stringUtils: StringUtils by inject()
    private val defaultLabels: DefaultLabels by inject()
    private val createWalletPresenter: CreateWalletPresenter by scopedInject()
    private var progressDialog: MaterialProgressDialog? = null
    private var applyConstraintSet: ConstraintSet = ConstraintSet()

    private val recoveryPhrase: String by unsafeLazy {
        intent.getStringExtra(RECOVERY_PHRASE) ?: ""
    }

    private val binding: ActivityCreateWalletBinding by lazy {
        ActivityCreateWalletBinding.inflate(layoutInflater)
    }

    private val passwordStrengthBinding: ViewPasswordStrengthBinding by lazy {
        ViewPasswordStrengthBinding.inflate(layoutInflater, binding.root, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        applyConstraintSet.clone(binding.mainConstraintLayout)

        if (recoveryPhrase.isNotEmpty()) {
            setupToolbar(binding.toolbarGeneral.toolbarGeneral, R.string.recover_funds)
            binding.commandNext.setText(R.string.dialog_continue)
        } else {
            setupToolbar(binding.toolbarGeneral.toolbarGeneral, R.string.new_account_title)
            binding.commandNext.setText(R.string.new_account_cta_text)
        }

        with(binding) {
            tos.movementMethod = LinkMovementMethod.getInstance() // make link clickable
            commandNext.isClickable = false
            passwordStrengthBinding.passStrengthBar.max = 100 * 10

            walletPass.onFocusChangeListener = this@CreateWalletActivity
            RxTextView.afterTextChangeEvents(walletPass)
                .doOnNext {
                    showEntropyContainer()
                    presenter.logEventPasswordOneClicked()
                    binding.entropyContainer.updatePassword(it.editable().toString())
                    hideShowCreateButton(
                        it.editable().toString().length,
                        walletPassConfirm.getTextString().length,
                        walletPasswordCheckbox.isChecked
                    )
                }
                .emptySubscribe()

            RxTextView.afterTextChangeEvents(walletPassConfirm)
                .doOnNext {
                    presenter.logEventPasswordTwoClicked()
                    hideShowCreateButton(
                        walletPass.getTextString().length,
                        it.editable().toString().length,
                        walletPasswordCheckbox.isChecked
                    )
                }
                .emptySubscribe()

            walletPasswordCheckbox.setOnCheckedChangeListener { _, isChecked ->
                hideShowCreateButton(
                    walletPass.getTextString().length, walletPassConfirm.getTextString().length, isChecked
                )
            }

            emailAddress.setOnClickListener { presenter.logEventEmailClicked() }
            commandNext.setOnClickListener { onNextClicked() }

            updateTosAndPrivacyLinks()
            updatePasswordDisclaimer()

            walletPassConfirm.setOnEditorActionListener { _, i, _ ->
                consume { if (i == EditorInfo.IME_ACTION_GO) onNextClicked() }
            }

            hideEntropyContainer()

            onViewReady()
        }
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

        binding.tos.apply {
            text = tosText
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun updatePasswordDisclaimer() {
        val linksMap = mapOf<String, Uri>(
            "backup" to Uri.parse(URL_BACKUP_INFO)
        )

        val tosText = stringUtils.getStringWithMappedAnnotations(
            R.string.password_disclaimer,
            linksMap,
            this
        )

        binding.walletPasswordBlurb.apply {
            text = tosText
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun hideShowCreateButton(password1Length: Int, password2Length: Int, isChecked: Boolean) {
        if (password1Length > 0 && password1Length == password2Length && isChecked) {
            showCreateWalletButton()
        } else {
            hideCreateWalletButton()
        }
    }

    override fun getView() = this

    override fun createPresenter() = createWalletPresenter

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun hideEntropyContainer() {
        TransitionManager.beginDelayedTransition(binding.mainConstraintLayout)
        applyConstraintSet.setVisibility(R.id.entropy_container, ConstraintSet.GONE)
        applyConstraintSet.applyTo(binding.mainConstraintLayout)
    }

    private fun showEntropyContainer() {
        TransitionManager.beginDelayedTransition(binding.mainConstraintLayout)
        applyConstraintSet.setVisibility(R.id.entropy_container, ConstraintSet.VISIBLE)
        applyConstraintSet.applyTo(binding.mainConstraintLayout)
    }

    private fun showCreateWalletButton() {
        TransitionManager.beginDelayedTransition(binding.mainConstraintLayout)
        applyConstraintSet.setVisibility(R.id.command_next, ConstraintSet.VISIBLE)
        applyConstraintSet.applyTo(binding.mainConstraintLayout)
    }

    private fun hideCreateWalletButton() {
        TransitionManager.beginDelayedTransition(binding.mainConstraintLayout)
        applyConstraintSet.setVisibility(R.id.command_next, ConstraintSet.GONE)
        applyConstraintSet.applyTo(binding.mainConstraintLayout)
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) = when {
        hasFocus -> showEntropyContainer()
        else -> hideEntropyContainer()
    }

    override fun showError(message: Int) =
        toast(message, ToastCustom.TYPE_ERROR)

    override fun warnWeakPassword(email: String, password: String) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.weak_password)
            .setPositiveButton(R.string.common_retry) { _, _ ->
                binding.apply {
                    walletPass.setText("")
                    walletPassConfirm.setText("")
                    walletPass.requestFocus()
                }
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

    override fun getDefaultAccountName(): String = defaultLabels.getDefaultNonCustodialWalletLabel(CryptoCurrency.BTC)

    override fun enforceFlagSecure() = true

    private fun onNextClicked() {
        with(binding) {
            val email = emailAddress.text.toString().trim()
            val password1 = walletPass.text.toString()
            val password2 = walletPassConfirm.text.toString()

            if (walletPasswordCheckbox.isChecked && presenter.validateCredentials(email, password1, password2)) {
                presenter.createOrRestoreWallet(email, password1, recoveryPhrase)
            }
        }
    }

    companion object {
        const val RECOVERY_PHRASE = "RECOVERY_PHRASE"

        fun start(context: Context) {
            context.startActivity(Intent(context, CreateWalletActivity::class.java))
        }
    }
}