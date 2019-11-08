package piuk.blockchain.android.ui.kyc.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavDirections
import com.blockchain.activities.StartOnboarding
import piuk.blockchain.android.ui.kyc.hyperlinks.renderTermsLinks
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navhost.models.KycStep
import piuk.blockchain.android.ui.kyc.navigate
import com.blockchain.swap.nabu.StartBuySell
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.logEvent
import com.blockchain.ui.extensions.throttledClicks
import com.blockchain.ui.urllinks.URL_COINIFY_POLICY
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import com.blockchain.ui.dialog.MaterialProgressDialog
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.androidcoreui.utils.extensions.visible
import timber.log.Timber
import kotlinx.android.synthetic.main.fragment_kyc_splash.button_kyc_splash_apply_now as buttonContinue
import kotlinx.android.synthetic.main.fragment_kyc_splash.text_view_kyc_splash_message as textViewMessage
import kotlinx.android.synthetic.main.fragment_kyc_splash.text_view_kyc_terms_and_conditions as textViewTerms

class KycSplashFragment : BaseFragment<KycSplashView, KycSplashPresenter>(), KycSplashView {

    private val startBuySell: StartBuySell by inject()

    private val presenter: KycSplashPresenter by inject()

    private val settingsDataManager: SettingsDataManager by inject()

    private val onBoardingStarter: StartOnboarding by inject()

    private val progressListener: KycProgressListener by ParentActivityDelegate(this)

    private var progressDialog: MaterialProgressDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_kyc_splash)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val campaignType = progressListener.campaignType
        logEvent(
            when (campaignType) {
                CampaignType.BuySell,
                CampaignType.Blockstack,
                CampaignType.Swap -> AnalyticsEvents.KycWelcome
                CampaignType.Sunriver -> AnalyticsEvents.KycSunriverStart
                CampaignType.Resubmission -> AnalyticsEvents.KycResubmission
            }
        )

        val title = when (progressListener.campaignType) {
            CampaignType.BuySell,
            CampaignType.Sunriver,
            CampaignType.Blockstack,
            CampaignType.Resubmission -> R.string.buy_sell_splash_title
            CampaignType.Swap -> R.string.kyc_splash_title
        }

        progressListener.setHostTitle(title)
        progressListener.incrementProgress(KycStep.SplashPage)

        textViewTerms.renderTermsLinks(
            R.string.buy_sell_splash_terms_and_conditions,
            URL_COINIFY_POLICY,
            URL_COINIFY_POLICY
        )
        textViewTerms.visible()

        textViewMessage.setText(R.string.buy_sell_splash_message)
    }

    private val disposable = CompositeDisposable()

    override fun onResume() {
        super.onResume()
        disposable += buttonContinue
            .throttledClicks()
            .subscribeBy(
                onNext = { presenter.onCTATapped(progressListener.campaignType) },
                onError = { Timber.e(it) }
            )
    }

    override fun onPause() {
        disposable.clear()
        super.onPause()
    }

    override fun goToNextKycStep(direction: NavDirections) =
        navigate(direction)

    override fun goToBuySellView() {
        activity?.finish()
        startBuySell.startBuySellActivity(requireContext())
    }

    override fun displayLoading(isLoading: Boolean) {
        if (isLoading) {
            progressDialog = MaterialProgressDialog(requireContext()).apply {
                setMessage(R.string.buy_sell_please_wait)
                show()
            }
        } else {
            progressDialog?.apply { dismiss() }
            progressDialog = null
        }
    }

    override fun showError(message: String) {
        toast(message, ToastCustom.TYPE_ERROR)
    }

    override fun onEmailNotVerified() {
        disposable += settingsDataManager.getSettings().subscribeBy(onNext = {
            activity?.let {
                onBoardingStarter.startEmailOnboarding(it)
            }
        }, onError = {})
    }

    override fun createPresenter(): KycSplashPresenter = presenter

    override fun getMvpView(): KycSplashView = this
}
