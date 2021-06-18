package piuk.blockchain.android.ui.recover

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.annotation.StringRes
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityAccountRecoveryBinding
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.base.mvi.MviActivity
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.visibleIf

class AccountRecoveryActivity :
    MviActivity<AccountRecoveryModel, AccountRecoveryIntents, AccountRecoveryState, ActivityAccountRecoveryBinding>() {

    override val model: AccountRecoveryModel by scopedInject()

    override val alwaysDisableScreenshots: Boolean
        get() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initControls()
    }

    override fun initBinding(): ActivityAccountRecoveryBinding = ActivityAccountRecoveryBinding.inflate(layoutInflater)

    override fun render(newState: AccountRecoveryState) {
        when (newState.status) {
            AccountRecoveryStatus.INVALID_PHRASE ->
                showSeedPhraseInputError(R.string.invalid_recovery_phrase_1)
            AccountRecoveryStatus.WORD_COUNT_ERROR ->
                showSeedPhraseInputError(R.string.recovery_phrase_word_count_error)
            AccountRecoveryStatus.RECOVERY_SUCCESSFUL -> start<PinEntryActivity>(this)
            AccountRecoveryStatus.RECOVERY_FAILED ->
                toast(R.string.restore_failed, ToastCustom.TYPE_ERROR)
            else -> {
                // Do nothing.
            }
        }
        binding.progressBar.visibleIf {
            newState.status == AccountRecoveryStatus.VERIFYING_SEED_PHRASE ||
                newState.status == AccountRecoveryStatus.RECOVERING_CREDENTIALS
        }
    }

    private fun initControls() {
        with(binding) {
            recoveryPhaseText.apply {
                addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable) {
                        binding.recoveryPhaseTextLayout.apply {
                            isErrorEnabled = false
                            error = ""
                        }
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })
            }
            // TODO add URLs
            resetAccountLabel.text = StringUtils.getStringWithMappedAnnotations(
                context = this@AccountRecoveryActivity,
                stringId = R.string.reset_account_notice,
                linksMap = mapOf("reset_account" to Uri.EMPTY)
            )
            resetKycLabel.text = StringUtils.getStringWithMappedAnnotations(
                context = this@AccountRecoveryActivity,
                stringId = R.string.reset_kyc_notice,
                linksMap = mapOf("learn_more" to Uri.EMPTY)
            )
            verifyButton.setOnClickListener {
                model.process(
                    AccountRecoveryIntents.VerifySeedPhrase(
                        seedPhrase = recoveryPhaseText.text?.toString() ?: ""
                    )
                )
            }
        }
    }

    private fun showSeedPhraseInputError(@StringRes errorText: Int) {
        binding.recoveryPhaseTextLayout.apply {
            isErrorEnabled = true
            error = getString(errorText)
        }
    }
}