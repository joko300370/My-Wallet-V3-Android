package piuk.blockchain.android.simplebuy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.BankLinkingPrefs
import info.blockchain.balance.CryptoCurrency
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_activity.*
import kotlinx.android.synthetic.main.toolbar_general.toolbar_general
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.linkbank.BankAuthFlowState
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.linkbank.fromPreferencesValue
import piuk.blockchain.android.ui.linkbank.toPreferencesValue
import piuk.blockchain.android.util.ViewUtils
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class SimpleBuyActivity : BlockchainActivity(), SimpleBuyNavigator {
    override val alwaysDisableScreenshots: Boolean
        get() = false

    override val enableLogoutTimer: Boolean = false
    private val compositeDisposable = CompositeDisposable()
    private val simpleBuyFlowNavigator: SimpleBuyFlowNavigator by scopedInject()
    private val bankLinkingPrefs: BankLinkingPrefs by scopedInject()

    private val startedFromDashboard: Boolean by unsafeLazy {
        intent.getBooleanExtra(STARTED_FROM_NAVIGATION_KEY, false)
    }

    private val startedFromApprovalDeepLink: Boolean by unsafeLazy {
        intent.getBooleanExtra(STARTED_FROM_APPROVAL_KEY, false)
    }

    private val preselectedPaymentMethodId: String? by unsafeLazy {
        intent.getStringExtra(PRESELECTED_PAYMENT_METHOD)
    }

    private val startedFromKycResume: Boolean by unsafeLazy {
        intent.getBooleanExtra(STARTED_FROM_KYC_RESUME, false)
    }

    private val cryptoCurrency: CryptoCurrency? by unsafeLazy {
        intent.getSerializableExtra(CRYPTOCURRENCY_KEY) as? CryptoCurrency
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_activity)
        setSupportActionBar(toolbar_general)
        if (savedInstanceState == null) {
            if (startedFromApprovalDeepLink) {
                val currentState = bankLinkingPrefs.getBankLinkingState().fromPreferencesValue()
                bankLinkingPrefs.setBankLinkingState(
                    currentState.copy(bankAuthFlow = BankAuthFlowState.BANK_APPROVAL_COMPLETE).toPreferencesValue()
                )
            }

            subscribeForNavigation()
        }
    }

    override fun onSheetClosed() = subscribeForNavigation()

    private fun subscribeForNavigation() {
        compositeDisposable += simpleBuyFlowNavigator.navigateTo(
            startedFromKycResume,
            startedFromDashboard,
            startedFromApprovalDeepLink,
            cryptoCurrency
        )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                when (it) {
                    is BuyNavigation.CurrencySelection -> launchCurrencySelector(it.currencies)
                    is BuyNavigation.FlowScreenWithCurrency -> startFlow(it)
                    BuyNavigation.PendingOrderScreen -> goToPendingOrderScreen()
                    BuyNavigation.OrderInProgressScreen -> goToPaymentScreen(false, startedFromApprovalDeepLink)
                }
            }
    }

    private fun launchCurrencySelector(currencies: List<String>) {
        compositeDisposable.clear()
        showBottomSheet(SimpleBuySelectCurrencyFragment.newInstance(currencies))
    }

    private fun startFlow(screenWithCurrency: BuyNavigation.FlowScreenWithCurrency) {
        when (screenWithCurrency.flowScreen) {
            FlowScreen.ENTER_AMOUNT -> goToBuyCryptoScreen(
                false, screenWithCurrency.cryptoCurrency, preselectedPaymentMethodId
            )
            FlowScreen.KYC -> startKyc()
            FlowScreen.KYC_VERIFICATION -> goToKycVerificationScreen(false)
            FlowScreen.CHECKOUT -> goToCheckOutScreen(false)
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

    override fun goToBuyCryptoScreen(
        addToBackStack: Boolean,
        preselectedCrypto: CryptoCurrency,
        preselectedPaymentMethodId: String?
    ) {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.content_frame,
                SimpleBuyCryptoFragment.newInstance(preselectedCrypto, preselectedPaymentMethodId),
                SimpleBuyCryptoFragment::class.simpleName
            )
            .apply {
                if (addToBackStack) {
                    addToBackStack(SimpleBuyCryptoFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    override fun goToCheckOutScreen(addToBackStack: Boolean) {
        ViewUtils.hideKeyboard(this)

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

    override fun goToPendingOrderScreen() {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.content_frame,
                SimpleBuyCheckoutFragment.newInstance(true),
                SimpleBuyCheckoutFragment::class.simpleName
            )
            .commitAllowingStateLoss()
    }

    override fun startKyc() {
        KycNavHostActivity.startForResult(this, CampaignType.SimpleBuy, KYC_STARTED)
    }

    override fun pop() = onBackPressed()

    override fun hasMoreThanOneFragmentInTheStack(): Boolean =
        supportFragmentManager.backStackEntryCount > 1

    override fun goToPaymentScreen(addToBackStack: Boolean, isPaymentAuthorised: Boolean) {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.content_frame, SimpleBuyPaymentFragment.newInstance(isPaymentAuthorised),
                SimpleBuyPaymentFragment::class.simpleName
            )
            .apply {
                if (addToBackStack) {
                    addToBackStack(SimpleBuyPaymentFragment::class.simpleName)
                }
            }
            .commitAllowingStateLoss()
    }

    override fun launchIntro() {
        supportFragmentManager.beginTransaction()
            .add(R.id.content_frame, SimpleBuyIntroFragment())
            .commitAllowingStateLoss()
    }

    override fun onSupportNavigateUp(): Boolean = consume { onBackPressed() }

    override fun showLoading() = progress.visible()

    override fun hideLoading() = progress.gone()

    override fun launchBankAuthWithError(errorState: ErrorState) {
        startActivity(BankAuthActivity.newInstance(errorState, BankAuthSource.SIMPLE_BUY, this))
    }

    companion object {
        const val KYC_STARTED = 6788
        const val RESULT_KYC_SIMPLE_BUY_COMPLETE = 7854

        private const val STARTED_FROM_NAVIGATION_KEY = "started_from_navigation_key"
        private const val STARTED_FROM_APPROVAL_KEY = "STARTED_FROM_APPROVAL_KEY"
        private const val CRYPTOCURRENCY_KEY = "crypto_currency_key"
        private const val PRESELECTED_PAYMENT_METHOD = "preselected_payment_method_key"
        private const val STARTED_FROM_KYC_RESUME = "started_from_kyc_resume_key"

        fun newInstance(
            context: Context,
            cryptoCurrency: CryptoCurrency? = null,
            launchFromNavigationBar: Boolean = false,
            launchKycResume: Boolean = false,
            preselectedPaymentMethodId: String? = null,
            launchFromApprovalDeepLink: Boolean = false
        ) = Intent(context, SimpleBuyActivity::class.java).apply {
            putExtra(STARTED_FROM_NAVIGATION_KEY, launchFromNavigationBar)
            putExtra(CRYPTOCURRENCY_KEY, cryptoCurrency)
            putExtra(STARTED_FROM_KYC_RESUME, launchKycResume)
            putExtra(PRESELECTED_PAYMENT_METHOD, preselectedPaymentMethodId)
            putExtra(STARTED_FROM_APPROVAL_KEY, launchFromApprovalDeepLink)
        }
    }
}