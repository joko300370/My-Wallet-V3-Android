package piuk.blockchain.android.ui.kyc.email.entry

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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

class KycEmailEntryFragment :
    MviFragment<EmailVeriffModel, EmailVeriffIntent, EmailVeriffState, FragmentKycAddEmailBinding>(),
    SlidingModalBottomDialog.Host,
    ResendOrChangeEmailBottomSheet.ResendOrChangeEmailHost {

    private val emailEntryHost: EmailEntryHost by ParentActivityDelegate(
        this
    )

    private val emailMustBeValidated by lazy {
        if (arguments?.containsKey("mustBeValidated") == true)
            EmailVerificationArgs.fromBundle(arguments ?: Bundle()).mustBeValidated
        else false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        emailEntryHost.onEmailEntryFragmentShown()

        if (emailMustBeValidated && savedInstanceState == null) {
            model.process(EmailVeriffIntent.ResendEmail)
        }
        model.process(EmailVeriffIntent.StartEmailVerification)
        binding.skip.setOnClickListener {
            emailEntryHost.onEmailVerificationSkipped()
        }
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentKycAddEmailBinding =
        FragmentKycAddEmailBinding.inflate(inflater, container, false)

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
        val boldText = email.address.takeIf { it.isNotEmpty() } ?: return
        val partOne = getString(R.string.email_verification_part_1)
        val partTwo = getString(R.string.email_verification_part_2)
        val sb = SpannableStringBuilder()
            .append(partOne)
            .append(boldText)
            .append(partTwo)

        sb.setSpan(
            StyleSpan(Typeface.BOLD),
            partOne.length,
            partOne.length + boldText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        with(binding) {
            emailInstructions.setText(sb, TextView.BufferType.SPANNABLE)
            emailStatusText.text = getString(R.string.email_verify)
            skip.visibleIf { !emailMustBeValidated }
            txStateIndicator.gone()
            ctaPrimary.apply {
                visible()
                text = getString(R.string.check_my_inbox)
                setOnClickListener {
                    openInbox()
                }
            }
            ctaSecondary.apply {
                visible()
                text = getString(R.string.did_not_get_email)
                setOnClickListener {
                    model.process(EmailVeriffIntent.CancelEmailVerification)
                    ResendOrChangeEmailBottomSheet().show(childFragmentManager, BOTTOM_SHEET)
                }
            }
        }
    }

    private fun openInbox() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_APP_EMAIL)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(Intent.createChooser(intent, getString(R.string.security_centre_email_check)))
    }

    override fun resendEmail() {
        model.process(EmailVeriffIntent.ResendEmail)
    }

    override fun editEmail() {
        EditEmailAddressBottomSheet().show(childFragmentManager, BOTTOM_SHEET)
    }

    override fun onSheetClosed() {
        model.process(EmailVeriffIntent.StartEmailVerification)
    }

    companion object {
        const val BOTTOM_SHEET = "BOTTOM_SHEET"
    }
}

interface EmailEntryHost {
    fun onEmailEntryFragmentShown()
    fun onEmailVerified()
    fun onEmailVerificationSkipped()
}