package piuk.blockchain.android.ui.thepit

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityPitVerifyEmailLayoutBinding
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity

class PitVerifyEmailActivity : BaseMvpActivity<PitVerifyEmailView, PitVerifyEmailPresenter>(), PitVerifyEmailView {

    private val pitVerifyEmailPresenter: PitVerifyEmailPresenter by scopedInject()

    private val binding: ActivityPitVerifyEmailLayoutBinding by lazy {
        ActivityPitVerifyEmailLayoutBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_general)
        setupToolbar(toolbar, R.string.the_exchange_verify_email_title)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val email = intent.getStringExtra(ARGUMENT_EMAIL) ?: ""
        with(binding) {
            emailAddress.text = email

            sendAgain.setOnClickListener {
                presenter.resendMail(email)
            }

            openApp.setOnClickListener {
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_APP_EMAIL)
                startActivity(Intent.createChooser(intent, getString(R.string.security_centre_email_check)))
            }
        }

        presenter.onViewReady()

        // We want to resend the email verification email so that the resent email verification contains the
        // context that the user is trying to link from the Pit.
        presenter.resendMail(email)
    }

    override fun createPresenter() = pitVerifyEmailPresenter

    override fun getView(): PitVerifyEmailView = this

    override fun mailResendFailed() {
        toast(R.string.mail_resent_failed, ToastCustom.TYPE_ERROR)
    }

    override fun mailResentSuccessfully() {
        toast(R.string.mail_resent_succeed, ToastCustom.TYPE_OK)
    }

    override fun emailVerified() {
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        private const val ARGUMENT_EMAIL = "email"

        fun start(ctx: AppCompatActivity, email: String, requestCode: Int) {
            val intent = Intent(ctx, PitVerifyEmailActivity::class.java).apply {
                putExtra(ARGUMENT_EMAIL, email)
            }
            ctx.startActivityForResult(intent, requestCode)
        }
    }
}