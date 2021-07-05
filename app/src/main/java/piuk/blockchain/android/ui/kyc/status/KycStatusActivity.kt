package piuk.blockchain.android.ui.kyc.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintSet
import com.blockchain.extensions.px
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.models.responses.nabu.KycTierState
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.logEvent
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.databinding.ActivityKycStatusBinding
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.dialogs.MaterialProgressDialog
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.ui.transactionflow.DialogFlow
import piuk.blockchain.android.ui.transactionflow.TransactionLauncher
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedDrawable

class KycStatusActivity : BaseMvpActivity<KycStatusView, KycStatusPresenter>(), KycStatusView, DialogFlow.FlowHost {

    private val binding: ActivityKycStatusBinding by lazy {
        ActivityKycStatusBinding.inflate(layoutInflater)
    }

    private val txLauncher: TransactionLauncher by inject()
    private val presenter: KycStatusPresenter by scopedInject()
    private val campaignType by unsafeLazy { intent.getSerializableExtra(EXTRA_CAMPAIGN_TYPE) as CampaignType }
    private var progressDialog: MaterialProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        logEvent(AnalyticsEvents.KycComplete)

        val title = when (campaignType) {
            CampaignType.Swap -> R.string.kyc_splash_title
            CampaignType.Sunriver,
            CampaignType.Blockstack,
            CampaignType.SimpleBuy,
            CampaignType.Resubmission,
            CampaignType.FiatFunds,
            CampaignType.None,
            CampaignType.Interest -> R.string.identity_verification
        }
        setupToolbar(binding.toolbarKyc, title)

        onViewReady()
    }

    override fun startExchange() {
        startSwapFlow()
    }

    private fun startSwapFlow() {
        txLauncher.startFlow(
            action = AssetAction.Swap,
            fragmentManager = supportFragmentManager,
            flowHost = this@KycStatusActivity
        )
    }

    override fun renderUi(kycState: KycTierState) {
        when (kycState) {
            KycTierState.Pending -> onPending()
            KycTierState.UnderReview -> onInReview()
            KycTierState.Expired, KycTierState.Rejected -> onFailed()
            KycTierState.Verified -> onVerified()
            KycTierState.None -> throw IllegalStateException(
                "Users who haven't started KYC should not be able to access this page"
            )
        }
    }

    private fun onPending() {
        with(binding) {
            imageViewKycStatus.setImageDrawable(
                getResolvedDrawable(
                    if (campaignType == CampaignType.Sunriver) {
                        R.drawable.vector_xlm_check
                    } else {
                        R.drawable.vector_in_progress
                    }
                )
            )
            textViewVerificationSubtitle.visible()
            textViewVerificationState.setTextColor(getResolvedColor(R.color.kyc_in_progress))
            textViewVerificationState.setText(R.string.kyc_status_title_in_progress)
            displayNotificationButton()
            val message = when (campaignType) {
                CampaignType.Swap,
                CampaignType.None,
                CampaignType.Resubmission -> R.string.kyc_status_message_in_progress
                CampaignType.Blockstack,
                CampaignType.SimpleBuy,
                CampaignType.Sunriver,
                CampaignType.FiatFunds,
                CampaignType.Interest -> R.string.sunriver_status_message
            }
            textViewVerificationMessage.setText(message)
        }
    }

    private fun onInReview() {
        with(binding) {
            imageViewKycStatus.setImageDrawable(getResolvedDrawable(R.drawable.vector_in_progress))
            textViewVerificationState.setTextColor(getResolvedColor(R.color.kyc_in_progress))
            textViewVerificationState.setText(R.string.kyc_status_title_in_review)
            textViewVerificationMessage.setText(R.string.kyc_status_message_under_review)
            displayNotificationButton()
        }
    }

    private fun displayNotificationButton() {
        binding.buttonKycStatusNext.apply {
            setText(R.string.kyc_status_button_notify_me)
            setOnClickListener { presenter.onClickNotifyUser() }
            visible()
        }
        binding.textViewKycStatusNoThanks.apply {
            visible()
            setOnClickListener { finish() }
        }
    }

    private fun onFailed() {
        with(binding) {
            imageViewKycStatus.setImageDrawable(getResolvedDrawable(R.drawable.vector_failed))
            textViewVerificationState.setTextColor(getResolvedColor(R.color.product_red_medium))
            textViewVerificationState.setText(R.string.kyc_status_title_failed)
            textViewVerificationMessage.setText(R.string.kyc_status_message_failed)
            buttonKycStatusNext.gone()
        }
    }

    private fun onVerified() {
        with(binding) {
            imageViewKycStatus.setImageDrawable(getResolvedDrawable(R.drawable.vector_verified))
            textViewVerificationState.setTextColor(getResolvedColor(R.color.kyc_progress_green))
            textViewVerificationState.setText(R.string.kyc_settings_status_verified)
            textViewVerificationMessage.setText(R.string.kyc_status_message_verified)
            buttonKycStatusNext.apply {
                setText(R.string.kyc_status_button_get_started)
                setOnClickListener { presenter.onClickContinue() }
                ConstraintSet().apply {
                    clone(constraintLayoutKycStatus)
                    setMargin(
                        R.id.button_kyc_status_next,
                        ConstraintSet.BOTTOM,
                        32.px
                    )
                    applyTo(constraintLayoutKycStatus)
                }

                visible()
            }
        }
    }

    override fun showToast(message: Int) {
        toast(message, ToastCustom.TYPE_OK)
    }

    override fun showNotificationsEnabledDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.kyc_status_button_notifications_enabled_title)
            .setMessage(R.string.kyc_status_button_notifications_enabled_message)
            .setPositiveButton(android.R.string.ok, null)
            .setOnDismissListener { finish() }
            .show()
    }

    override fun showProgressDialog() {
        progressDialog = MaterialProgressDialog(this).apply {
            setOnCancelListener { presenter.onProgressCancelled() }
            setMessage(R.string.kyc_country_selection_please_wait)
            show()
        }
    }

    override fun dismissProgressDialog() {
        progressDialog?.apply { dismiss() }
        progressDialog = null
    }

    override fun finishPage() {
        toast(R.string.kyc_status_error, ToastCustom.TYPE_ERROR)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean = consume { finish() }

    override fun createPresenter(): KycStatusPresenter = presenter

    override fun getView(): KycStatusView = this

    companion object {

        private const val EXTRA_CAMPAIGN_TYPE =
            "info.blockchain.wallet.ui.BalanceFragment.EXTRA_CAMPAIGN_TYPE"

        @JvmStatic
        fun start(context: Context, campaignType: CampaignType) {
            Intent(context, KycStatusActivity::class.java)
                .apply { putExtra(EXTRA_CAMPAIGN_TYPE, campaignType) }
                .run { context.startActivity(this) }
        }
    }

    override fun onFlowFinished() {
    }
}
