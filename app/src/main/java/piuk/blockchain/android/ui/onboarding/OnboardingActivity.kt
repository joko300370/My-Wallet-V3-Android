package piuk.blockchain.android.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.customviews.dialogs.MaterialProgressDialog
import piuk.blockchain.android.ui.fingerprint.FingerprintDialog
import piuk.blockchain.android.ui.fingerprint.FingerprintStage
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity

internal class OnboardingActivity : BaseMvpActivity<OnboardingView, OnboardingPresenter>(),
    OnboardingView,
    FingerprintPromptFragment.OnFragmentInteractionListener,
    EmailPromptFragment.OnFragmentInteractionListener {

    private val onboardingPresenter: OnboardingPresenter by scopedInject()
    private var emailLaunched = false

    private var progressDialog: MaterialProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        progressDialog = MaterialProgressDialog(this).apply {
            setMessage(R.string.please_wait)
            setCancelable(false)
            show()
        }

        onViewReady()
    }

    override fun onResume() {
        super.onResume()

        if (emailLaunched) {
            finish()
        }
    }

    override val showEmail: Boolean
        get() = intent.showEmail

    override val showFingerprints: Boolean
        get() = intent.showFingerprints

    override fun showFingerprintPrompt() {
        if (!isFinishing) {
            dismissDialog()
            val fragmentManager = supportFragmentManager
            fragmentManager.beginTransaction()
                .replace(R.id.content_frame, FingerprintPromptFragment.newInstance())
                .commit()
        }
    }

    override fun showEmailPrompt() {
        if (!isFinishing) {
            dismissDialog()
            val fragmentManager = supportFragmentManager
            fragmentManager.beginTransaction()
                .replace(
                    R.id.content_frame,
                    EmailPromptFragment.newInstance(presenter.email!!)
                )
                .commit()
        }
    }

    override fun onEnableFingerprintClicked() {
        presenter.onEnableFingerprintClicked()
    }

    override fun showFingerprintDialog(pincode: String) {
        if (!isFinishing) {
            val dialog = FingerprintDialog.newInstance(
                pincode,
                FingerprintStage.REGISTER_FINGERPRINT
            )

            dialog.setAuthCallback(object : FingerprintDialog.FingerprintAuthCallback {
                override fun onAuthenticated(data: String?) {
                    dialog.dismissAllowingStateLoss()
                    presenter?.setFingerprintUnlockEnabled(true)
                    if (showEmail) {
                        showEmailPrompt()
                    } else {
                        finish()
                    }
                }

                override fun onCanceled() {
                    dialog.dismissAllowingStateLoss()
                    presenter?.setFingerprintUnlockEnabled(true)
                }
            })

            dialog.show(supportFragmentManager, FingerprintDialog.TAG)
        }
    }

    override fun showEnrollFingerprintsDialog() {
        if (!isFinishing) {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.fingerprint_no_fingerprints_added)
                .setCancelable(true)
                .setPositiveButton(R.string.common_yes) { _, _ ->
                    startActivityForResult(
                        Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS),
                        0
                    )
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    override fun onVerifyEmailClicked() {
        presenter.disableAutoLogout()
        emailLaunched = true
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_APP_EMAIL)
        startActivity(Intent.createChooser(intent, getString(R.string.security_centre_email_check)))
    }

    override fun createPresenter() = onboardingPresenter

    override fun getView(): OnboardingView {
        return this
    }

    private fun dismissDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == EMAIL_CLIENT_REQUEST) {
            presenter.enableAutoLogout()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {

        private const val EMAIL_CLIENT_REQUEST = 5400

        fun launchForFingerprints(ctx: Context) {

            Intent(ctx, OnboardingActivity::class.java).let {
                it.showEmail = false
                it.showFingerprints = true
                ctx.startActivity(it)
            }
        }

        fun launchForEmail(ctx: Context) {

            Intent(ctx, OnboardingActivity::class.java).let {
                it.showEmail = true
                it.showFingerprints = false
                ctx.startActivity(it)
            }
        }

        /**
         * Flag for showing only the email verification prompt. This is for use when signup was
         * completed some other time, but the user hasn't verified their email yet.
         */
        private const val EXTRAS_SHOW_EMAIL = "show_email"
        private const val EXTRAS_SHOW_FINGERPRINTS = "show_fingerprints"

        private var Intent.showEmail: Boolean
            get() = extras?.getBoolean(EXTRAS_SHOW_EMAIL, true) ?: true
            set(v) {
                putExtra(EXTRAS_SHOW_EMAIL, v)
            }

        private var Intent.showFingerprints: Boolean
            get() = extras?.getBoolean(EXTRAS_SHOW_FINGERPRINTS, true) ?: true
            set(v) {
                putExtra(EXTRAS_SHOW_FINGERPRINTS, v)
            }
    }
}
