package piuk.blockchain.android.ui.kyc.email.entry

import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentKycAddEmailBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.androidcore.data.settings.Email

class KycEmailEntryFragment : MviFragment<EmailVeriffModel, EmailVeriffIntent, EmailVeriffState>(),
    SlidingModalBottomDialog.Host {

    /* private val presenter: KycEmailEntryPresenter by scopedInject()*/
    /*
        private val analytics: Analytics by inject()
    */
    private val emailEntryHost: EmailEntryHost by ParentActivityDelegate(
        this
    )

    private var _binding: FragmentKycAddEmailBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKycAddEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        emailEntryHost.onEmailEntryFragmentShown()
        model.process(EmailVeriffIntent.FetchEmail)
        binding.editEmailAddress.setOnClickListener {
            model.process(EmailVeriffIntent.CancelEmailVerification)
            ChangeEmailAddressBottomSheet().show(childFragmentManager, "BOTTOM_SHEET")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        model.process(EmailVeriffIntent.StartEmailVerification)
    }

    override fun onPause() {
        super.onPause()
    }

    override val model: EmailVeriffModel by scopedInject()

    override fun render(newState: EmailVeriffState) {
        if (newState.email?.verified == true) {
            drawVerifiedEmailUi()
        } else if (newState.email?.verified == false) {
            drawUnVerifiedEmailUi(newState.email)
        }
    }

    private fun drawVerifiedEmailUi() {
        binding.emailInstructions.text = getString(R.string.success_email_veriff)
        binding.emailStatusText.text = getString(R.string.email_verified)
        binding.txStateIndicator.setImageResource(R.drawable.ic_check_circle)
        binding.txStateIndicator.visible()
        binding.editEmailAddress.gone()
    }

    private fun drawUnVerifiedEmailUi(email: Email) {
        binding.emailInstructions.text = getString(R.string.sent_email_verification, email.address)
        binding.emailStatusText.text = getString(R.string.email_verify)
        binding.txStateIndicator.gone()
        binding.editEmailAddress.visible()
    }

    override fun onSheetClosed() {
        model.process(EmailVeriffIntent.StartEmailVerification)
    }

    /*override fun preFillEmail(email: String) {
        editTextEmail.setText(email)
    }
*//*
    override fun showError(message: Int) =
        toast(message, ToastCustom.TYPE_ERROR)
*/
    /* override fun continueSignUp(email: String) {
         navigate(KycEmailEntryFragmentDirections.actionValidateEmail(email))
     }*/

    /* override fun showProgressDialog() {
         progressDialog = MaterialProgressDialog(requireContext()).apply {
             setOnCancelListener { presenter.onProgressCancelled() }
             setMessage(R.string.kyc_country_selection_please_wait)
             show()
         }
     }*/

    /* override fun dismissProgressDialog() {
         progressDialog?.apply { dismiss() }
         progressDialog = null
     }*/

    /*private fun TextView.onDelayedChange(
        kycStep: KycStep
    ): Observable<Boolean> =
        this.afterTextChangeEvents()
            .debounce(300, TimeUnit.MILLISECONDS)
            .map { it.editable()?.toString() ?: "" }
            .skipFirstUnless { it.isNotEmpty() }
            .observeOn(AndroidSchedulers.mainThread())
            .map { mapToCompleted(it) }
            .distinctUntilChanged()
            .doOnNext {
                buttonNext.isEnabled = it
            }

    private fun mapToCompleted(text: String): Boolean = emailIsValid(text)

    override fun createPresenter(): KycEmailEntryPresenter = presenter

    override fun getMvpView(): KycEmailEntryView = this*/
}

private fun emailIsValid(target: String) =
    !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches()

interface EmailEntryHost {
    fun onEmailEntryFragmentShown()
}