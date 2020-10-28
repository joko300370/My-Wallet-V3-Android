package piuk.blockchain.android.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.FIND_VIEWS_WITH_TEXT
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem
import com.blockchain.koin.scopedInject
import com.blockchain.lockbox.ui.LockboxLandingActivity
import com.blockchain.notifications.NotificationsUtil
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.NotificationAppOpened
import com.blockchain.notifications.analytics.RequestAnalyticsEvents
import com.blockchain.notifications.analytics.SendAnalytics
import com.blockchain.notifications.analytics.SwapAnalyticsEvents
import com.blockchain.notifications.analytics.TransactionsAnalyticsEvents
import com.blockchain.notifications.analytics.activityShown
import com.blockchain.ui.urllinks.URL_BLOCKCHAIN_SUPPORT_PORTAL
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar_general.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoTarget
import piuk.blockchain.android.scan.QrScanError
import piuk.blockchain.android.scan.QrScanHandler
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.account.AccountActivity
import piuk.blockchain.android.ui.activity.ActivitiesFragment
import piuk.blockchain.android.ui.airdrops.AirdropCentreActivity
import piuk.blockchain.android.ui.backup.BackupWalletActivity
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.android.ui.dashboard.DashboardFragment
import piuk.blockchain.android.ui.home.analytics.SideNavEvent
import piuk.blockchain.android.ui.interest.InterestDashboardActivity
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.status.KycStatusActivity
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.android.ui.onboarding.OnboardingActivity
import piuk.blockchain.android.ui.pairingcode.PairingCodeActivity
import piuk.blockchain.android.ui.sell.BuySellFragment
import piuk.blockchain.android.ui.settings.SettingsActivity
import piuk.blockchain.android.ui.swap.SwapFragment
import piuk.blockchain.android.ui.swapintro.SwapIntroFragment
import piuk.blockchain.android.ui.thepit.PitLaunchBottomDialog
import piuk.blockchain.android.ui.thepit.PitPermissionsActivity
import piuk.blockchain.android.ui.tour.IntroTourAnalyticsEvent
import piuk.blockchain.android.ui.tour.IntroTourHost
import piuk.blockchain.android.ui.tour.IntroTourStep
import piuk.blockchain.android.ui.tour.SwapTourFragment
import piuk.blockchain.android.ui.transactionflow.DialogFlow
import piuk.blockchain.android.ui.transactionflow.TransactionFlow
import piuk.blockchain.android.ui.transfer.TransferFragment
import piuk.blockchain.android.ui.zxing.CaptureActivity
import piuk.blockchain.android.util.calloutToExternalSupportLinkDlg
import piuk.blockchain.android.util.getAccount
import piuk.blockchain.android.withdraw.WithdrawActivity
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.AndroidUtils
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible
import timber.log.Timber
import java.util.ArrayList

class MainActivity : MvpActivity<MainView, MainPresenter>(),
    HomeNavigator,
    MainView,
    IntroTourHost,
    DialogFlow.FlowHost {

    override val presenter: MainPresenter by scopedInject()
    private val prefs: PersistentPrefs by inject()
    override val view: MainView = this

    var drawerOpen = false
        internal set

    private var handlingResult = false

    private val _refreshAnnouncements = PublishSubject.create<Unit>()
    val refreshAnnouncements: Observable<Unit>
        get() = _refreshAnnouncements

    private var activityResultAction: () -> Unit = {}

    private var backPressed: Long = 0

    private val tabSelectedListener =
        AHBottomNavigation.OnTabSelectedListener { position, wasSelected ->

            presenter.doTestnetCheck()

            if (!wasSelected) {
                when (position) {
                    ITEM_HOME -> {
                        startDashboardFragment()
                    }
                    ITEM_ACTIVITY -> {
                        startActivitiesFragment()
                        analytics.logEvent(TransactionsAnalyticsEvents.TabItemClick)
                    }
                    ITEM_SWAP -> {
                        if (prefs.newSwapEnabled) startSwapFragment() else
                            presenter.startSwapOrKyc(null, null)
                        analytics.logEvent(SwapAnalyticsEvents.SwapTabItemClick)
                    }
                    ITEM_BUY_SELL -> {
                        startBuySellFragment()
                        analytics.logEvent(RequestAnalyticsEvents.TabItemClicked)
                    }
                    ITEM_TRANSFER -> {
                        startTransferFragment()
                    }
                }
                ViewUtils.setElevation(appbar_layout, 4f)
            }
            true
        }

    private val currentFragment: Fragment
        get() = supportFragmentManager.findFragmentById(R.id.content_frame)!!

    internal val activity: Context
        get() = this

    private val menu: Menu
        get() = navigation_view.menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (intent.hasExtra(NotificationsUtil.INTENT_FROM_NOTIFICATION) &&
            intent.getBooleanExtra(NotificationsUtil.INTENT_FROM_NOTIFICATION, false)
        ) {
            analytics.logEvent(NotificationAppOpened)
        }

        drawer_layout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                // No-op
            }

            override fun onDrawerOpened(drawerView: View) {
                drawerOpen = true
                if (tour_guide.isActive) {
                    setTourMenuView()
                }
                analytics.logEvent(SideNavEvent.SideMenuOpenEvent)
            }

            override fun onDrawerClosed(drawerView: View) {
                drawerOpen = false
            }

            override fun onDrawerStateChanged(newState: Int) {
                // No-op
            }
        })

        // Set up toolbar_constraint
        toolbar_general.navigationIcon = ContextCompat.getDrawable(this, R.drawable.vector_menu)
        toolbar_general.title = ""
        setSupportActionBar(toolbar_general)

        // Styling
        bottom_navigation.apply {
            addItems(toolbarNavigationItems())
            accentColor = ContextCompat.getColor(context, R.color.bottom_toolbar_icon_active)
            inactiveColor = ContextCompat.getColor(context, R.color.bottom_toolbar_icon_inactive)

            titleState = AHBottomNavigation.TitleState.ALWAYS_SHOW
            isForceTint = true

            setUseElevation(true)
            setTitleTypeface(ResourcesCompat.getFont(context, R.font.inter_medium))

            setTitleTextSizeInSp(10.0f, 10.0f)

            // Select Dashboard by default
            setOnTabSelectedListener(tabSelectedListener)
            currentItem = if (intent.getBooleanExtra(START_BUY_SELL_INTRO_KEY, false)) ITEM_BUY_SELL else ITEM_HOME
        }
    }

    override fun onResume() {
        super.onResume()
        activityResultAction().also {
            activityResultAction = {}
        }
        // This can null out in low memory situations, so reset here
        navigation_view.setNavigationItemSelectedListener { menuItem ->
            selectDrawerItem(menuItem)
            true
        }
        presenter.updateTicker()

        if (!handlingResult) {
            resetUi()
        }

        handlingResult = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawer_layout.openDrawer(GravityCompat.START)
                true
            }
            R.id.action_qr_main -> {
                val fragment = currentFragment::class.simpleName ?: "unknown"
                QrScanHandler.requestScanPermissions(
                    activity = this,
                    rootView = parent_constraint_layout
                ) {
                    QrScanHandler.startQrScanActivity(this, appUtil)
                }
                analytics.logEvent(SendAnalytics.QRButtonClicked)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        handlingResult = true
        // We create a lambda so we handle the result after the view is attached to the presenter (onResume)
        activityResultAction = {
            when (requestCode) {
                QrScanHandler.SCAN_URI_RESULT -> {
                    val scanData = data?.getStringExtra(CaptureActivity.SCAN_RESULT)
                    if (resultCode == RESULT_OK && scanData != null) {
                        presenter.processScanResult(scanData)
                    }
                }
                SETTINGS_EDIT,
                ACCOUNT_EDIT,
                KYC_STARTED -> {
                    replaceContentFragment(DashboardFragment.newInstance())
                    // Reset state in case of changing currency etc
                    bottom_navigation.currentItem = ITEM_HOME

                    // Pass this result to balance fragment
                    for (fragment in supportFragmentManager.fragments) {
                        fragment.onActivityResult(requestCode, resultCode, data)
                    }
                }
                INTEREST_DASHBOARD -> {
                    if (resultCode == RESULT_FIRST_USER) {
                        data?.let { intent ->
                            val account = intent.extras?.getAccount(InterestDashboardActivity.ACTIVITY_ACCOUNT)

                            replaceContentFragment(ActivitiesFragment.newInstance(account))
                            setCurrentTabItem(ITEM_ACTIVITY)
                        }
                    }
                }
                else -> super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    override fun onBackPressed() {
        if (tour_guide.isActive) {
            tour_guide.stop()
        }
        val f = currentFragment
        val backHandled = when {
            drawerOpen -> {
                drawer_layout.closeDrawers()
                true
            }
            f is ActivitiesFragment -> f.onBackPressed()
            f is TransferFragment -> {
                setCurrentTabItem(ITEM_HOME)
                startDashboardFragment()
                true
            }
            f is DashboardFragment -> f.onBackPressed()
            f is BuySellFragment -> f.onBackPressed()
            f is SwapIntroFragment -> f.onBackPressed()
            else -> {
                // Switch to dashboard fragment - it's not clear, though,
                // how we can ever wind up here...
                setCurrentTabItem(ITEM_HOME)
                startDashboardFragment()
                true
            }
        }

        if (!backHandled) {
            if (backPressed + BuildConfig.EXIT_APP_COOLDOWN_MILLIS > System.currentTimeMillis()) {
                presenter.clearLoginState()
            } else {
                showExitConfirmToast()
                backPressed = System.currentTimeMillis()
            }
        }
    }

    private fun setTourMenuView() {
        val item = menu.findItem(R.id.nav_backup)

        val out = ArrayList<View>()
        drawer_layout.findViewsWithText(out, item.title, FIND_VIEWS_WITH_TEXT)

        if (out.isNotEmpty()) {
            val menuView = out[0]
            tour_guide.setDeferredTriggerView(menuView, offsetX = -menuView.width / 3)
        }
    }

    private fun showExitConfirmToast() {
        ToastCustom.makeText(this,
            getString(R.string.exit_confirm),
            ToastCustom.LENGTH_SHORT,
            ToastCustom.TYPE_GENERAL)
    }

    private fun selectDrawerItem(menuItem: MenuItem) {
        analytics.logEvent(SideNavEvent(menuItem.itemId))
        when (menuItem.itemId) {
            R.id.nav_lockbox -> LockboxLandingActivity.start(this)
            R.id.nav_backup -> launchBackupFunds()
            R.id.nav_the_exchange -> presenter.onThePitMenuClicked()
            R.id.nav_airdrops -> AirdropCentreActivity.start(this)
            R.id.nav_addresses -> startActivityForResult(Intent(this, AccountActivity::class.java),
                ACCOUNT_EDIT)
            R.id.login_web_wallet -> PairingCodeActivity.start(this)
            R.id.nav_settings -> startActivityForResult(Intent(this, SettingsActivity::class.java),
                SETTINGS_EDIT)
            R.id.nav_support -> onSupportClicked()
            R.id.nav_logout -> showLogoutDialog()
            R.id.nav_interest -> launchInterestDashboard()
        }
        drawer_layout.closeDrawers()
    }

    override fun showLoading() {
        progress.visible()
    }

    override fun hideLoading() {
        progress.gone()
    }

    override fun launchThePitLinking(linkId: String) {
        PitPermissionsActivity.start(this, linkId)
    }

    override fun launchThePit() {
        PitLaunchBottomDialog.launch(this)
    }

    override fun setPitEnabled(enabled: Boolean) {
        setPitVisible(enabled)
    }

    override fun launchBackupFunds(fragment: Fragment?, requestCode: Int) {
        fragment?.let {
            BackupWalletActivity.startForResult(it, requestCode)
        } ?: BackupWalletActivity.start(this)
    }

    override fun launchSetup2Fa() {
        SettingsActivity.startFor2Fa(this)
    }

    override fun launchVerifyEmail() {
        Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_EMAIL)
            startActivity(Intent.createChooser(this, getString(R.string.security_centre_email_check)))
        }
    }

    override fun launchSetupFingerprintLogin() {
        OnboardingActivity.launchForFingerprints(this)
    }

    override fun launchTransfer() {
        startTransferFragment(TransferFragment.TransferViewType.TYPE_RECEIVE)
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.logout_wallet)
            .setMessage(R.string.ask_you_sure_logout)
            .setPositiveButton(R.string.btn_logout) { _, _ ->
                analytics.logEvent(AnalyticsEvents.Logout)
                presenter.unPair()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun onSupportClicked() {
        analytics.logEvent(AnalyticsEvents.Support)
        calloutToExternalSupportLinkDlg(this, URL_BLOCKCHAIN_SUPPORT_PORTAL)
    }

    private fun resetUi() {
        toolbar_general.title = ""

        // Set selected appropriately.
        with(bottom_navigation) {
            when (currentFragment) {
                is DashboardFragment -> currentItem = ITEM_HOME
                is ActivitiesFragment -> currentItem = ITEM_ACTIVITY
                is TransferFragment -> currentItem = ITEM_TRANSFER
                is BuySellFragment -> currentItem = ITEM_BUY_SELL
            }
        }
    }

    private fun startSingleActivity(clazz: Class<*>) {
        val intent = Intent(this@MainActivity, clazz)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun kickToLauncherPage() {
        startSingleActivity(LauncherActivity::class.java)
    }

    override fun showProgressDialog(message: Int) {
        super.showProgressDialog(message, null)
    }

    override fun hideProgressDialog() {
        super.dismissProgressDialog()
    }

    override fun launchKyc(campaignType: CampaignType) {
        KycNavHostActivity.startForResult(this, campaignType, KYC_STARTED)
    }

    override fun launchSwap(
        sourceAccount: CryptoAccount?,
        targetAccount: CryptoAccount?
    ) {
        startSwapFragment(sourceAccount, targetAccount)
    }

    override fun getStartIntent(): Intent {
        return intent
    }

    private fun setPitVisible(visible: Boolean) {
        val menu = menu
        menu.findItem(R.id.nav_the_exchange).isVisible = visible
    }

    override fun clearAllDynamicShortcuts() {
        if (AndroidUtils.is25orHigher()) {
            getSystemService(ShortcutManager::class.java)!!.removeAllDynamicShortcuts()
        }
    }

    override fun showTestnetWarning() {
        val snack = Snackbar.make(
            parent_constraint_layout,
            R.string.testnet_warning,
            Snackbar.LENGTH_SHORT
        )
        val view = snack.view
        view.setBackgroundColor(ContextCompat.getColor(this, R.color.product_red_medium))
        snack.show()
    }

    override fun enableSwapButton(isEnabled: Boolean) {
        if (isEnabled) {
            bottom_navigation.enableItemAtPosition(ITEM_SWAP)
        } else {
            bottom_navigation.disableItemAtPosition(ITEM_SWAP)
        }
    }

    override fun displayDialog(title: Int, message: Int) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    @SuppressLint("CheckResult")
    override fun startTransactionFlowWithTarget(targets: Collection<CryptoTarget>) {
        if (targets.size > 1) {
            disambiguateSendScan(targets)
        } else {
            val targetAddress = targets.first()
            QrScanHandler.selectSourceAccount(this, targetAddress)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { sourceAccount ->
                        TransactionFlow(
                            sourceAccount = sourceAccount,
                            target = targetAddress,
                            action = AssetAction.NewSend
                        ).apply {
                            startFlow(
                                fragmentManager = currentFragment.childFragmentManager,
                                host = this@MainActivity
                            )
                        }
                    },
                    onError = { Timber.e("Unable to select source account for scan") }
                )
        }
    }

    override fun showScanTargetError(error: QrScanError) {
        ToastCustom.makeText(
            this,
            getString(
                when (error.errorCode) {
                    QrScanError.ErrorCode.ScanFailed -> R.string.error_scan_failed_general
                    QrScanError.ErrorCode.BitPayScanFailed -> R.string.error_scan_failed_bitpay
                }
            ),
            ToastCustom.LENGTH_LONG,
            ToastCustom.TYPE_ERROR
        )
    }

    @SuppressLint("CheckResult")
    private fun disambiguateSendScan(targets: Collection<CryptoTarget>) {
        QrScanHandler.disambiguateScan(this, targets)
            .subscribeBy(
                onSuccess = {
                    startTransactionFlowWithTarget(listOf(it))
                }
            )
    }

    override fun displayLockboxMenu(lockboxAvailable: Boolean) {
        menu.findItem(R.id.nav_lockbox).isVisible = lockboxAvailable
    }

    override fun showHomebrewDebugMenu() {
        menu.findItem(R.id.nav_debug_swap).isVisible = true
    }

    override fun goToTransfer() {
        startTransferFragment()
    }

    private fun startTransferFragment(
        viewToShow: TransferFragment.TransferViewType = TransferFragment.TransferViewType.TYPE_SEND
    ) {
        setCurrentTabItem(ITEM_TRANSFER)
        toolbar_general.title = getString(R.string.transfer)

        val transferFragment = TransferFragment.newInstance(viewToShow)
        replaceContentFragment(transferFragment)
    }

    private fun startBuySellFragment(
        viewType: BuySellFragment.BuySellViewType = BuySellFragment.BuySellViewType.TYPE_BUY
    ) {
        setCurrentTabItem(ITEM_BUY_SELL)

        val buySellFragment = BuySellFragment.newInstance(viewType)
        replaceContentFragment(buySellFragment)
    }

    private fun startSwapFragment(sourceAccount: CryptoAccount? = null, destinationAccount: CryptoAccount? = null) {
        setCurrentTabItem(ITEM_SWAP)
        toolbar_general.title = getString(R.string.swap)
        val swapFragment = SwapFragment.newInstance(sourceAccount, destinationAccount)
        replaceContentFragment(swapFragment)
    }

    override fun gotoDashboard() {
        bottom_navigation.currentItem = ITEM_HOME
    }

    private fun startDashboardFragment() {
        val fragment = DashboardFragment.newInstance()
        replaceContentFragment(fragment)
    }

    override fun gotoActivityFor(account: BlockchainAccount?) =
        startActivitiesFragment(account)

    override fun goToWithdraw(currency: String) {
        startActivity(WithdrawActivity.newInstance(
            context = this,
            currency = currency
        ))
    }

    override fun resumeSimpleBuyKyc() {
        startActivity(
            SimpleBuyActivity.newInstance(
                context = this,
                launchKycResume = true
            )
        )
    }

    override fun startSimpleBuy(cryptoCurrency: CryptoCurrency) {
        startActivity(
            SimpleBuyActivity.newInstance(
                context = this,
                launchFromNavigationBar = true,
                cryptoCurrency = cryptoCurrency
            )
        )
    }

    override fun startSell() {
        startBuySellFragment(BuySellFragment.BuySellViewType.TYPE_SELL)
    }

    override fun startInterestDashboard() {
        launchInterestDashboard()
    }

    private fun launchInterestDashboard() =
        startActivityForResult(
            InterestDashboardActivity.newInstance(this), INTEREST_DASHBOARD
        )

    private fun startActivitiesFragment(account: BlockchainAccount? = null) {
        setCurrentTabItem(ITEM_ACTIVITY)
        val fragment = ActivitiesFragment.newInstance(account)
        replaceContentFragment(fragment)
        toolbar_general.title = ""
        analytics.logEvent(activityShown(account?.label ?: "All Wallets"))
    }

    override fun refreshAnnouncements() {
        _refreshAnnouncements.onNext(Unit)
    }

    override fun launchKycIntro() {
        val swapIntroFragment = SwapIntroFragment.newInstance()
        replaceContentFragment(swapIntroFragment)
    }

    override fun launchSwapIntro() {
        setCurrentTabItem(ITEM_SWAP)

        val swapIntroFragment = SwapIntroFragment.newInstance()
        replaceContentFragment(swapIntroFragment)
    }

    override fun launchPendingVerificationScreen(campaignType: CampaignType) {
        KycStatusActivity.start(this, campaignType)
    }

    override fun shouldIgnoreDeepLinking() =
        (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0

    private fun replaceContentFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        fragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment, fragment.javaClass.simpleName)
            .commitAllowingStateLoss()
    }

    /*** Silently switch the current tab in the tab_bar */
    private fun setCurrentTabItem(item: Int) {
        bottom_navigation.apply {
            removeOnTabSelectedListener()
            currentItem = item
            setOnTabSelectedListener(tabSelectedListener)
        }
    }

    companion object {

        val TAG: String = MainActivity::class.java.simpleName
        const val START_BUY_SELL_INTRO_KEY = "START_BUY_SELL_INTRO_KEY"
        const val ACCOUNT_EDIT = 2008
        const val SETTINGS_EDIT = 2009
        const val KYC_STARTED = 2011
        const val INTEREST_DASHBOARD = 2012

        // Keep these constants - the position of the toolbar items - and the generation of the toolbar items
        // together.
        private const val ITEM_ACTIVITY = 0
        private const val ITEM_SWAP = 1
        private const val ITEM_HOME = 2
        private const val ITEM_BUY_SELL = 3
        private const val ITEM_TRANSFER = 4

        private fun toolbarNavigationItems(): List<AHBottomNavigationItem> =
            listOf(AHBottomNavigationItem(
                R.string.toolbar_cmd_activity,
                R.drawable.ic_vector_toolbar_activity,
                R.color.white
            ), AHBottomNavigationItem(
                R.string.toolbar_cmd_swap,
                R.drawable.ic_vector_toolbar_swap,
                R.color.white
            ), AHBottomNavigationItem(
                R.string.toolbar_cmd_home,
                R.drawable.ic_vector_toolbar_home,
                R.color.white
            ), AHBottomNavigationItem(
                R.string.buy_and_sell,
                R.drawable.ic_tab_cart,
                R.color.white
            ), AHBottomNavigationItem(
                R.string.toolbar_cmd_transfer,
                R.drawable.ic_vector_toolbar_transfer,
                R.color.white
            ))

        fun start(context: Context, bundle: Bundle) {
            Intent(context, MainActivity::class.java).apply {
                putExtras(bundle)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)

                context.startActivity(this)
            }
        }
    }

    override fun launchIntroTour() {

        bottom_navigation.restoreBottomNavigation(false)

        val tourSteps = listOf(
            IntroTourStep(
                name = "Step_One",
                lookupTriggerView = { bottom_navigation.getViewAtPosition(ITEM_HOME) },
                analyticsEvent = IntroTourAnalyticsEvent.IntroPortfolioViewedAnalytics,
                msgIcon = R.drawable.ic_vector_toolbar_home,
                msgTitle = R.string.tour_step_one_title,
                msgBody = R.string.tour_step_one_body_1,
                msgButton = R.string.tour_step_one_btn
            ),
            // TODO how will we show these two steps in the future?
            IntroTourStep(
                name = "Step_Two",
                lookupTriggerView = { bottom_navigation.getViewAtPosition(ITEM_TRANSFER) },
                analyticsEvent = IntroTourAnalyticsEvent.IntroSendViewedAnalytics,
                msgIcon = R.drawable.ic_vector_toolbar_transfer,
                msgTitle = R.string.tour_step_two_title,
                msgBody = R.string.tour_step_two_body,
                msgButton = R.string.tour_step_two_btn
            ),
            IntroTourStep(
                name = "Step_Three",
                lookupTriggerView = { bottom_navigation.getViewAtPosition(ITEM_TRANSFER) },
                analyticsEvent = IntroTourAnalyticsEvent.IntroRequestViewedAnalytics,
                msgIcon = R.drawable.ic_vector_toolbar_receive,
                msgTitle = R.string.tour_step_three_title,
                msgBody = R.string.tour_step_three_body,
                msgButton = R.string.tour_step_three_btn
            ),
            IntroTourStep(
                name = "Step_Four",
                lookupTriggerView = { bottom_navigation.getViewAtPosition(ITEM_SWAP) },
                triggerClick = {
                    replaceContentFragment(SwapTourFragment.newInstance())
                },
                analyticsEvent = IntroTourAnalyticsEvent.IntroSwapViewedAnalytics,
                msgIcon = R.drawable.ic_vector_toolbar_swap,
                msgTitle = R.string.tour_step_four_title,
                msgBody = R.string.tour_step_four_body,
                msgButton = R.string.tour_step_four_btn
            )
        )

        tour_guide.start(this, tourSteps)
    }

    override fun onTourFinished() {
        drawer_layout.closeDrawers()
        startDashboardFragment()
    }

    override fun showTourDialog(dlg: BottomSheetDialogFragment) {
        val fm = supportFragmentManager
        dlg.show(fm, "TOUR_SHEET")
    }

    override fun onFlowFinished() {
    }
}
