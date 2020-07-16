package piuk.blockchain.android.simplebuy

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.koin.scopedInject
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_simple_buy.*
import kotlinx.android.synthetic.main.toolbar_general.toolbar_general
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.cards.CardDetailsActivity
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
    private val simpleBuyModel: SimpleBuyModel by scopedInject()
    private val compositeDisposable = CompositeDisposable()
    private val simpleBuyFlowNavigator: SimpleBuyFlowNavigator by scopedInject()

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
            compositeDisposable += simpleBuyFlowNavigator.navigateTo(startedFromKycResume, startedFromDashboard)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy {
                    when (it) {
                        FlowScreen.INTRO -> launchIntro()
                        FlowScreen.ENTER_AMOUNT -> goToBuyCryptoScreen(false)
                        FlowScreen.KYC -> startKyc()
                        FlowScreen.CURRENCY_SELECTOR -> goToCurrencySelection(false)
                        FlowScreen.KYC_VERIFICATION -> goToKycVerificationScreen(false)
                        FlowScreen.CHECKOUT -> goToCheckOutScreen(false)
                        FlowScreen.BANK_DETAILS -> goToBankDetailsScreen(false)
                        FlowScreen.ADD_CARD -> addNewCard()
                    }
                }
        }
    }

    private fun addNewCard() {
        val intent = Intent(this, CardDetailsActivity::class.java)
        startActivityForResult(intent, CardDetailsActivity.ADD_CARD_REQUEST_CODE)
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

    override fun goToCurrencySelection(addToBackStack: Boolean) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame,
                SimpleBuySelectCurrencyFragment(),
                SimpleBuySelectCurrencyFragment::class.simpleName)
            .apply {
                if (addToBackStack) {
                    addToBackStack(SimpleBuySelectCurrencyFragment::class.simpleName)
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

    override fun goToPendingOrderScreen() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame,
                SimpleBuyCheckoutFragment.newInstance(true),
                SimpleBuyCheckoutFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun startKyc() {
        KycNavHostActivity.startForResult(this, CampaignType.SimpleBuy, KYC_STARTED)
    }

    override fun hasMoreThanOneFragmentInTheStack(): Boolean =
        supportFragmentManager.backStackEntryCount > 1

    override fun goToCardPaymentScreen(addToBackStack: Boolean) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, SimpleBuyPaymentFragment(), SimpleBuyPaymentFragment::class.simpleName)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == KYC_STARTED && resultCode == RESULT_KYC_SIMPLE_BUY_COMPLETE) {
            simpleBuyModel.process(SimpleBuyIntent.KycCompleted)
            goToKycVerificationScreen()
        } else if (requestCode == CardDetailsActivity.ADD_CARD_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val card = (data?.extras?.getSerializable(CardDetailsActivity.CARD_KEY) as?
                        PaymentMethod.Card) ?: return
                val cardId = card.cardId
                val cardLabel = card.uiLabel()
                val cardPartner = card.partner

                simpleBuyModel.process(SimpleBuyIntent.UpdateSelectedPaymentMethod(
                    cardId,
                    cardLabel,
                    cardPartner,
                    PaymentMethodType.PAYMENT_CARD
                ))
                goToCheckOutScreen()
            } else
                finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean = consume {
        onBackPressed()
    }

    override fun showLoading() {
        progress.visible()
    }

    override fun hideLoading() {
        progress.gone()
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