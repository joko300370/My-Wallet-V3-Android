package piuk.blockchain.android.ui.interest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.notifications.analytics.InterestAnalytics
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.InterestAccount
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.databinding.ActivityInterestDashboardBinding
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.android.ui.customviews.account.AccountSelectSheet
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.transactionflow.DialogFlow
import piuk.blockchain.android.ui.transactionflow.TransactionLauncher
import piuk.blockchain.android.util.putAccount
import piuk.blockchain.androidcore.utils.helperfunctions.consume

class InterestDashboardActivity : BlockchainActivity(),
    InterestSummarySheet.Host,
    InterestDashboardFragment.InterestDashboardHost,
    DialogFlow.FlowHost {

    private val binding: ActivityInterestDashboardBinding by lazy {
        ActivityInterestDashboardBinding.inflate(layoutInflater)
    }

    private val txLauncher: TransactionLauncher by inject()

    override val alwaysDisableScreenshots: Boolean
        get() = false

    private val fragment: InterestDashboardFragment by lazy { InterestDashboardFragment.newInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarGeneral.toolbarGeneral)
        setTitle(R.string.interest_dashboard_title)

        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment, InterestDashboardFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun onSupportNavigateUp(): Boolean = consume {
        onBackPressed()
    }

    override fun goToActivityFor(account: BlockchainAccount) {
        val b = Bundle()
        b.putAccount(ACTIVITY_ACCOUNT, account)
        setResult(RESULT_FIRST_USER, Intent().apply {
            putExtras(b)
        })
        finish()
    }

    override fun goToInterestDeposit(toAccount: InterestAccount) {
        clearBottomSheet()
        require(toAccount is CryptoAccount)
        txLauncher.startFlow(
            target = toAccount,
            action = AssetAction.InterestDeposit,
            fragmentManager = supportFragmentManager,
            flowHost = this
        )
    }

    override fun goToInterestWithdraw(fromAccount: InterestAccount) {
        clearBottomSheet()
        require(fromAccount is CryptoAccount)
        txLauncher.startFlow(
            sourceAccount = fromAccount,
            action = AssetAction.InterestWithdraw,
            fragmentManager = supportFragmentManager,
            flowHost = this
        )
    }

    override fun onSheetClosed() {
        // do nothing
    }

    override fun startKyc() {
        analytics.logEvent(InterestAnalytics.INTEREST_DASHBOARD_KYC)
        KycNavHostActivity.start(this, CampaignType.Interest)
    }

    override fun showInterestSummarySheet(account: SingleAccount, cryptoCurrency: CryptoCurrency) {
        showBottomSheet(InterestSummarySheet.newInstance(account, cryptoCurrency))
    }

    override fun startDepositFlow(fromAccount: SingleAccount, toAccount: SingleAccount) {
        analytics.logEvent(InterestAnalytics.INTEREST_DASHBOARD_ACTION)
        startDeposit(fromAccount, toAccount)
    }

    override fun startAccountSelection(
        filter: Single<List<BlockchainAccount>>,
        toAccount: SingleAccount
    ) {
        showBottomSheet(
            AccountSelectSheet.newInstance(object : AccountSelectSheet.SelectionHost {
                override fun onAccountSelected(account: BlockchainAccount) {
                    startDeposit(account as SingleAccount, toAccount)
                }

                override fun onSheetClosed() {
                    // do nothing
                }
            }, filter, R.string.select_deposit_source_title)
        )
    }

    private fun startDeposit(
        fromAccount: SingleAccount,
        toAccount: SingleAccount
    ) {
        txLauncher.startFlow(
            sourceAccount = fromAccount as CryptoAccount,
            target = toAccount,
            action = AssetAction.InterestDeposit,
            fragmentManager = supportFragmentManager,
            flowHost = this
        )
    }

    companion object {
        const val ACTIVITY_ACCOUNT = "ACTIVITY_ACCOUNT"

        fun newInstance(context: Context) =
            Intent(context, InterestDashboardActivity::class.java)
    }

    override fun onFlowFinished() {
        fragment.refreshBalances()
    }
}