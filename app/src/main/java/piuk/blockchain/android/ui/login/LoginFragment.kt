package piuk.blockchain.android.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.koin.scopedInject
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentLoginBinding
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.android.ui.scan.QrExpected
import piuk.blockchain.android.ui.scan.QrScanActivity
import piuk.blockchain.android.ui.scan.QrScanActivity.Companion.getRawScanData
import piuk.blockchain.android.util.visibleIf
import timber.log.Timber

class LoginFragment : MviFragment<LoginModel, LoginIntents, LoginState, FragmentLoginBinding>() {

    override val model: LoginModel by scopedInject()

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build()

    private val googleSignInClient: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(requireContext(), gso)
    }

    private val recaptchaClient: GoogleReCaptchaClient by lazy {
        GoogleReCaptchaClient(requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recaptchaClient.initReCaptcha()

        with(binding) {
            backButton.setOnClickListener {
                parentFragmentManager.popBackStack()
            }
            loginEmailText.apply {
                inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

                addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable) {
                        model.process(LoginIntents.UpdateEmail(s.toString()))
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })
            }
            continueButton.setOnClickListener {
                binding.loginEmailText.text?.let { emailInputText ->
                    if (emailInputText.isNotBlank()) {
                        verifyReCaptcha(emailInputText.toString())
                    }
                }
            }
            scanPairingButton.setOnClickListener {
                QrScanActivity.start(this@LoginFragment, QrExpected.MAIN_ACTIVITY_QR)
            }
            continueWithGoogleButton.setOnClickListener {
                startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recaptchaClient.close()
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginBinding =
        FragmentLoginBinding.inflate(inflater, container, false)

    override fun render(newState: LoginState) {
        updateUI(newState)
        when (newState.currentStep) {
            LoginStep.SHOW_SCAN_ERROR -> {
                toast(R.string.pairing_failed, ToastCustom.TYPE_ERROR)
                if (newState.shouldRestartApp) {
                    startActivity(
                        Intent(requireContext(), LauncherActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            }
            LoginStep.ENTER_PIN -> {
                startActivity(
                    Intent(requireContext(), PinEntryActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            LoginStep.VERIFY_DEVICE -> navigateToVerifyDevice()
            LoginStep.SHOW_SESSION_ERROR -> toast(R.string.login_failed_session_id_error, ToastCustom.TYPE_ERROR)
            LoginStep.SHOW_EMAIL_ERROR -> toast(R.string.login_send_email_error, ToastCustom.TYPE_ERROR)
            else -> {}
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == AppCompatActivity.RESULT_OK && requestCode == QrScanActivity.SCAN_URI_RESULT) {
            data.getRawScanData()?.let { rawQrString ->
                model.process(LoginIntents.LoginWithQr(rawQrString))
            }
        } else if (resultCode == AppCompatActivity.RESULT_OK && requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                task.result.email?.let { email ->
                    verifyReCaptcha(email)
                } ?: toast(R.string.login_google_email_not_found, ToastCustom.TYPE_GENERAL)
            } catch (apiException: ApiException) {
                Timber.e(apiException)
                toast(R.string.login_google_sign_in_failed, ToastCustom.TYPE_ERROR)
            }
        }
    }

    private fun updateUI(newState: LoginState) {
        with(binding) {
            progressBar.visibleIf { newState.isLoading }
            loginOrLabel.visibleIf { !newState.isTypingEmail && !newState.isLoading }
            loginOrSeparatorLeft.visibleIf { !newState.isTypingEmail && !newState.isLoading }
            loginOrSeparatorRight.visibleIf { !newState.isTypingEmail && !newState.isLoading }
            scanPairingButton.visibleIf { !newState.isTypingEmail && !newState.isLoading }
            // TODO enable Google auth once ready
            continueButton.visibleIf { newState.isTypingEmail }
            continueButton.isEnabled = newState.isTypingEmail && emailRegex.matches(newState.email)
        }
    }

    private fun navigateToVerifyDevice() {
        parentFragmentManager.run {
            beginTransaction()
                .replace(
                    R.id.content_frame,
                    VerifyDeviceFragment(),
                    VerifyDeviceFragment::class.simpleName
                )
                .addToBackStack(VerifyDeviceFragment::class.simpleName)
                .commitAllowingStateLoss()
        }
    }

    private fun verifyReCaptcha(selectedEmail: String) {
        recaptchaClient.verifyForLogin(
            onSuccess = { response ->
                model.process(
                    LoginIntents.ObtainSessionIdForEmail(
                        selectedEmail = selectedEmail,
                        captcha = response.tokenResult
                    )
                )
            },
            onError = { toast(R.string.common_error, ToastCustom.TYPE_ERROR) }
        )
    }

    private val emailRegex = Regex(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
            "\\@" +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(" +
            "\\." +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
            ")+"
    )

    companion object {
        private const val RC_SIGN_IN = 10
    }
}