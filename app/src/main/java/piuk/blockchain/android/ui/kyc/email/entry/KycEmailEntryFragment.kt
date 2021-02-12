package piuk.blockchain.android.ui.kyc.email.entry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.EmailVerificationArgs
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentKycAddEmailBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.androidcore.data.settings.Email

class KycEmailEntryFragment : MviFragment<EmailVeriffModel, EmailVeriffIntent, EmailVeriffState>(),
    SlidingModalBottomDialog.Host {

    private val emailEntryHost: EmailEntryHost by ParentActivityDelegate(
        this
    )
    private val emailMustBeValidated by lazy {
        if (arguments?.containsKey("mustBeValidated") == true)
            EmailVerificationArgs.fromBundle(arguments ?: Bundle()).mustBeValidated
        else false
    }

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

        if (emailMustBeValidated && savedInstanceState == null) {
            model.process(EmailVeriffIntent.ResendEmail)
        } else {
            model.process(EmailVeriffIntent.StartEmailVerification)
        }
        binding.skip.setOnClickListener {
            emailEntryHost.onEmailVerificationSkipped()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

    override val model: EmailVeriffModel by scopedInject()

    override fun render(newState: EmailVeriffState) {
        if (newState.email.verified) {
            drawVerifiedEmailUi()
        } else {
            drawUnVerifiedEmailUi(newState.email)
        }
    }

    private fun drawVerifiedEmailUi() {
        binding.emailInstructions.text = getString(R.string.success_email_veriff)
        binding.emailStatusText.text = getString(R.string.email_verified)
        binding.skip.gone()
        binding.txStateIndicator.setImageResource(R.drawable.ic_check_circle)
        binding.txStateIndicator.visible()
        binding.ctaPrimary.apply {
            visible()
            text = getString(R.string.next)
            setOnClickListener {
                emailEntryHost.onEmailVerified()
            }
        }
        binding.ctaSecondary.gone()
    }

    private fun drawUnVerifiedEmailUi(email: Email) {
        binding.emailInstructions.text = getString(R.string.sent_email_verification, email.address)
        binding.emailStatusText.text = getString(R.string.email_verify)
        binding.skip.visibleIf { !emailMustBeValidated }
        binding.txStateIndicator.gone()
        binding.ctaPrimary.apply {
            visible()
            text = getString(R.string.check_my_inbox)
            setOnClickListener {
                model.process(EmailVeriffIntent.CancelEmailVerification)
                EditEmailAddressBottomSheet.newInstance(email.address).show(childFragmentManager, "BOTTOM_SHEET")
            }
        }
        binding.ctaSecondary.apply {
            visible()
            text = getString(R.string.did_not_get_email)
            setOnClickListener {
                emailEntryHost.onEmailNeverArrived()
            }
        }
    }

    override fun onSheetClosed() {
        model.process(EmailVeriffIntent.StartEmailVerification)
    }
}

interface EmailEntryHost {
    fun onEmailEntryFragmentShown()
    fun onEmailVerified()
    fun onEmailVerificationSkipped()
    fun onEmailNeverArrived()
}