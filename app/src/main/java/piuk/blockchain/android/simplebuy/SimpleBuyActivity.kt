package piuk.blockchain.android.simplebuy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_simple_buy.*
import kotlinx.android.synthetic.main.toolbar_general.toolbar_general
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible

class SimpleBuyActivity : BlockchainActivity(), SimpleBuyNavigator {
    override val alwaysDisableScreenshots: Boolean
        get() = false

    override val enableLogoutTimer: Boolean = false
    private val simpleBuyModel: SimpleBuyModel by inject()
    private val compositeDisposable = CompositeDisposable()
    private val simpleBuyFlowNavigator: SimpleBuyFlowNavigator by inject()

    private val startedFromDashboard: Boolean by unsafeLazy {
        intent.getBooleanExtra(STARTED_FROM_DASHBOARD_KEY, false)
    }

    private val startedFromKycResume: Boolean by unsafeLazy {
        intent.getBooleanExtra(STARTED_FROM_KYC_RESUME, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_buy)
        setSupportActionBar(toolbar_general)
        if (savedInstanceState == null) {
            compositeDisposable += simpleBuyFlowNavigator.navigateTo(startedFromKycResume).subscribeBy {
                when (it) {
                    FlowScreen.INTRO -> {
                        if (startedFromDashboard) goToBuyCryptoScreen(false) else launchIntro()
                    }
                    FlowScreen.ENTER_AMOUNT -> goToBuyCryptoScreen(false)
                    FlowScreen.KYC -> startKyc()
                    FlowScreen.KYC_VERIFICATION -> goToKycVerificationScreen(false)
                    FlowScreen.CHECKOUT -> goToCheckOutScreen(false)
                    FlowScreen.BANK_DETAILS -> goToBankDetailsScreen(false)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    override fun exitSimpleBuyFlow() {
        if (!startedFromDashboard) {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } else {
            finish()
        }
    }

    override fun goToBuyCryptoScreen(addToBackStack: Boolean) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, SimpleBuyCryptoFragment(), SimpleBuyCryptoFragment::class.simpleName)
            .apply {
                if (addToBackStack) {
                    addToBackStack(SimpleBuyCryptoFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    override fun goToCheckOutScreen(addToBackStack: Boolean) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, SimpleBuyCheckoutFragment(), SimpleBuyCheckoutFragment::class.simpleName)
            .apply {
                if (addToBackStack) {
                    addToBackStack(SimpleBuyCheckoutFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    override fun goToKycVerificationScreen(addToBackStack: Boolean) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, SimpleBuyPendingKycFragment(), SimpleBuyPendingKycFragment::class.simpleName)
            .apply {
                if (addToBackStack) {
                    addToBackStack(SimpleBuyPendingKycFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    override fun goToBankDetailsScreen(addToBackStack: Boolean) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, SimpleBuyBankDetailsFragment(), SimpleBuyBankDetailsFragment::class.simpleName)
            .apply {
                if (addToBackStack) {
                    addToBackStack(SimpleBuyBankDetailsFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    override fun startKyc() {
        KycNavHostActivity.startForResult(this, CampaignType.SimpleBuy, KYC_STARTED)
    }

    override fun launchIntro() {
        supportFragmentManager.beginTransaction()
            .add(R.id.content_frame,
                SimpleBuyIntroFragment())
            .commitAllowingStateLoss()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == KYC_STARTED && resultCode == RESULT_KYC_SIMPLE_BUY_COMPLETE) {
            simpleBuyModel.process(SimpleBuyIntent.KycCompleted)
            goToKycVerificationScreen()
        }
    }

    override fun onBackPressed() {
        val fragments = supportFragmentManager.fragments
        for (fragment in fragments) {
            if (fragment is SimpleBuyScreen && backActionShouldBeHandledByFragment(fragment)) {
                return
            }
        }
        super.onBackPressed()
    }

    private fun backActionShouldBeHandledByFragment(simpleBuyScreen: SimpleBuyScreen): Boolean =
        simpleBuyScreen.onBackPressed() && handleByScreenOrPop(simpleBuyScreen)

    private fun handleByScreenOrPop(simpleBuyScreen: SimpleBuyScreen): Boolean =
        simpleBuyScreen.backPressedHandled() || pop()

    override fun onSupportNavigateUp(): Boolean = consume {
        onBackPressed()
    }

    override fun showLoading() {
        progress.visible()
    }

    override fun hideLoading() {
        progress.gone()
    }

    private fun pop(): Boolean {
        val backStackEntryCount = supportFragmentManager.backStackEntryCount
        if (backStackEntryCount > 1) {
            supportFragmentManager.popBackStack()
            return true
        }
        return false
    }

    companion object {
        const val KYC_STARTED = 6788
        const val RESULT_KYC_SIMPLE_BUY_COMPLETE = 7854

        private const val STARTED_FROM_DASHBOARD_KEY = "started_from_dashboard_key"
        private const val STARTED_FROM_KYC_RESUME = "started_from_kyc_resume_key"

        fun newInstance(context: Context, launchFromDashboard: Boolean = false, launchKycResume: Boolean = false) =
            Intent(context, SimpleBuyActivity::class.java).apply {
                putExtra(STARTED_FROM_DASHBOARD_KEY, launchFromDashboard)
                putExtra(STARTED_FROM_KYC_RESUME, launchKycResume)
            }
    }
}