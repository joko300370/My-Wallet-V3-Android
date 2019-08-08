package piuk.blockchain.android.ui.home

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ShortcutManager
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatEditText
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem
import com.blockchain.annotations.ButWhy
import com.blockchain.annotations.CommonCode
import com.blockchain.kycui.navhost.KycNavHostActivity
import com.blockchain.kycui.navhost.models.CampaignType
import com.blockchain.lockbox.ui.LockboxLandingActivity
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.ui.urllinks.URL_BLOCKCHAIN_SUPPORT_PORTAL
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.util.FormatsUtil
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar_general.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.data.datamanagers.PromptDlgFactory
import piuk.blockchain.android.databinding.ActivityMainBinding
import piuk.blockchain.android.ui.account.AccountActivity
import piuk.blockchain.android.ui.backup.BackupWalletActivity
import piuk.blockchain.android.ui.balance.BalanceFragment
import piuk.blockchain.android.ui.buysell.launcher.BuySellLauncherActivity
import piuk.blockchain.android.ui.confirm.ConfirmPaymentDialog
import piuk.blockchain.android.ui.customviews.callbacks.OnTouchOutsideViewListener
import piuk.blockchain.android.ui.dashboard.DashboardFragment
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.android.ui.pairingcode.PairingCodeActivity
import piuk.blockchain.android.ui.receive.ReceiveFragment
import piuk.blockchain.android.ui.send.SendFragment
import piuk.blockchain.android.ui.settings.SettingsActivity
import piuk.blockchain.android.ui.swap.homebrew.exchange.host.HomebrewNavHostActivity
import piuk.blockchain.android.ui.swapintro.SwapIntroFragment
import piuk.blockchain.android.ui.thepit.PitLaunchBottomDialog
import piuk.blockchain.android.ui.thepit.PitPermissionsActivity
import piuk.blockchain.android.ui.transactions.TransactionDetailActivity
import piuk.blockchain.android.ui.zxing.CaptureActivity
import piuk.blockchain.android.util.calloutToExternalSupportLinkDlg
import piuk.blockchain.androidbuysell.models.WebViewLoginDetails
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.AndroidUtils
import piuk.blockchain.androidcoreui.utils.AppUtil
import piuk.blockchain.androidcoreui.utils.ViewUtils
import timber.log.Timber

class MainActivity : BaseMvpActivity<MainView, MainPresenter>(), HomeNavigator, MainView,
    ConfirmPaymentDialog.OnConfirmDialogInteractionListener {

    var drawerOpen = false
        internal set

    private var handlingResult = false

    private val mainPresenter: MainPresenter by inject()

    private val appUtil: AppUtil by inject()
    private val analytics: Analytics by inject()

    internal lateinit var binding: ActivityMainBinding

    private var progressDlg: MaterialProgressDialog? = null
    private var backPressed: Long = 0

    private var webViewLoginDetails: WebViewLoginDetails? = null

    // Fragment callbacks for currency header
    private val touchOutsideViews = HashMap<View, OnTouchOutsideViewListener>()

    private val tabSelectedListener =
        AHBottomNavigation.OnTabSelectedListener { position, wasSelected ->

            presenter.doTestnetCheck()

            if (!wasSelected) {
                when (position) {
                    ITEM_SEND -> if (currentFragment !is SendFragment) {
                        // This is a bit of a hack to allow the selection of the correct button
                        // On the bottom nav bar, but without starting the fragment again
                        startSendFragment(null)
                        ViewUtils.setElevation(binding.appbarLayout, 0f)
                    }
                    ITEM_HOME -> {
                        startDashboardFragment()
                        ViewUtils.setElevation(binding.appbarLayout, 4f)
                    }
                    ITEM_ACTIVITY -> {
                        startBalanceFragment()
                        ViewUtils.setElevation(binding.appbarLayout, 0f)
                    }
                    ITEM_RECEIVE -> {
                        startReceiveFragment()
                        ViewUtils.setElevation(binding.appbarLayout, 0f)
                    }
                    ITEM_SWAP -> {
                        presenter.startSwapOrKyc(null)
                    }
                }
            }
            true
        }

    private val currentFragment: Fragment
        get() = supportFragmentManager.findFragmentById(R.id.content_frame)!!

    internal val activity: Context
        get() = this

    private val selectedAccountFromFragments: Int
        get() = (currentFragment as? ReceiveFragment)?.getSelectedAccountPosition() ?: -1

    private val menu: Menu
        get() = binding.navigationView.menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        receiver.registerIntents(this)

        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                // No-op
            }

            override fun onDrawerOpened(drawerView: View) {
                drawerOpen = true
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

        // Notify Presenter that page is setup
        onViewReady()

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
            currentItem = ITEM_HOME
        }
    }

    override fun onResume() {
        super.onResume()

        // This can null out in low memory situations, so reset here
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            selectDrawerItem(menuItem)
            true
        }
        presenter.updateTicker()

        if (!handlingResult) {
            resetUi()
        }

        handlingResult = false
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                binding.drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            R.id.action_qr_main -> {
                requestScan()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        handlingResult = true
        if (resultCode == Activity.RESULT_OK && requestCode == SCAN_URI &&
            data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null
        ) {
            val strResult = data.getStringExtra(CaptureActivity.SCAN_RESULT)
            doScanInput(strResult)
        } else if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_BACKUP) {
            resetUi()
        } else if (requestCode == SETTINGS_EDIT ||
            requestCode == ACCOUNT_EDIT ||
            requestCode == KYC_STARTED
        ) {
            replaceContentFragment(DashboardFragment.newInstance())
            // Reset state in case of changing currency etc
            bottom_navigation.currentItem = ITEM_HOME

            // Pass this result to balance fragment
            for (fragment in supportFragmentManager.fragments) {
                fragment.onActivityResult(requestCode, resultCode, data)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onBackPressed() {
        val f = currentFragment
        val backHandled = when {
            drawerOpen -> {
                binding.drawerLayout.closeDrawers()
                true
            }
            f is BalanceFragment -> f.onBackPressed()
            f is SendFragment -> f.onBackPressed()
            f is ReceiveFragment -> f.onBackPressed()
            f is DashboardFragment -> f.onBackPressed()
            f is SwapIntroFragment -> f.onBackPressed()
            else -> {
                // Switch to balance fragment - it's not clear, though,
                // how we can ever wind up here...
                startReceiveFragment()
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

    private fun showExitConfirmToast() {
        ToastCustom.makeText(this,
            getString(R.string.exit_confirm),
            ToastCustom.LENGTH_SHORT,
            ToastCustom.TYPE_GENERAL)
    }

    private fun startScanActivity() {
        if (!appUtil.isCameraOpen) {
            val intent = Intent(this@MainActivity, CaptureActivity::class.java)
            startActivityForResult(intent, SCAN_URI)
        } else {
            ToastCustom.makeText(
                this@MainActivity,
                getString(R.string.camera_unavailable),
                ToastCustom.LENGTH_SHORT,
                ToastCustom.TYPE_ERROR
            )
        }
    }

    private fun doScanInput(strResult: String) {
        when {
            strResult.isBTCorBCHAddress() -> disambiguateBTCandBCHQrScans(strResult)
            strResult.isETHAddress() -> disambiguateETHQrScans(strResult)
            strResult.isHttpUri() -> presenter.handlePossibleDeepLink(strResult)
            else -> startSendFragment(strResult)
        }
    }

    private fun disambiguateBTCandBCHQrScans(uri: String) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.confirm_currency)
            .setMessage(R.string.confirm_currency_message)
            .setCancelable(true)
            .setPositiveButton(R.string.bitcoin_cash) { _, _ ->
                presenter.setCryptoCurrency(CryptoCurrency.BCH)
                startSendFragment(uri)
            }
            .setNegativeButton(R.string.bitcoin) { _, _ ->
                presenter.setCryptoCurrency(CryptoCurrency.BTC)
                startSendFragment(uri)
            }
            .create()
            .show()
    }

    private fun disambiguateETHQrScans(uri: String) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.confirm_currency)
            .setMessage(R.string.confirm_currency_message)
            .setCancelable(true)
            .setPositiveButton(R.string.ether) { _, _ ->
                presenter.setCryptoCurrency(CryptoCurrency.ETHER)
                startSendFragment(uri)
            }
            .setNegativeButton(R.string.usd_pax) { _, _ ->
                presenter.setCryptoCurrency(CryptoCurrency.PAX)
                startSendFragment(uri)
            }
            .create()
            .show()
    }

    private fun String.isHttpUri(): Boolean = startsWith("http")
    private fun String.isBTCorBCHAddress(): Boolean = FormatsUtil.isValidBitcoinAddress(this)
    private fun String.isETHAddress(): Boolean = FormatsUtil.isValidEthereumAddress(this)

    private fun selectDrawerItem(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.nav_lockbox -> LockboxLandingActivity.start(this)
            R.id.nav_backup -> startActivityForResult(
                Intent(this, BackupWalletActivity::class.java), REQUEST_BACKUP)
            R.id.nav_exchange_homebrew_debug -> HomebrewNavHostActivity.start(
                this,
                mainPresenter.defaultCurrency
            )
            R.id.nav_the_pit -> presenter.onThePitMenuClicked()
            R.id.nav_addresses -> startActivityForResult(Intent(this, AccountActivity::class.java), ACCOUNT_EDIT)
            R.id.nav_buy -> presenter.routeToBuySell()
            R.id.login_web_wallet -> PairingCodeActivity.start(this)
            R.id.nav_settings -> startActivityForResult(Intent(this, SettingsActivity::class.java), SETTINGS_EDIT)
            R.id.nav_support -> onSupportClicked()
            R.id.nav_logout -> showLogoutDialog()
        }
        binding.drawerLayout.closeDrawers()
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

    private fun showLogoutDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.unpair_wallet)
            .setMessage(R.string.ask_you_sure_unpair)
            .setPositiveButton(R.string.unpair) { _, _ ->
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
                is BalanceFragment -> currentItem = ITEM_ACTIVITY
                is SendFragment -> currentItem = ITEM_SEND
                is ReceiveFragment -> currentItem = ITEM_RECEIVE
            }
        }
    }

    private fun requestScan() {

        val fragment = currentFragment::class.simpleName ?: "unknown"

        analytics.logEvent(object : AnalyticsEvent {
            override val event = "qr_scan_requested"
            override val params = mapOf("fragment" to fragment)
        })

        val deniedPermissionListener = SnackbarOnDeniedPermissionListener.Builder
            .with(binding.root, R.string.request_camera_permission)
            .withButton(android.R.string.ok) { requestScan() }
            .build()

        val grantedPermissionListener = object : BasePermissionListener() {
            override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                startScanActivity()
            }
        }

        val compositePermissionListener =
            CompositePermissionListener(deniedPermissionListener, grantedPermissionListener)

        Dexter.withActivity(this)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(compositePermissionListener)
            .withErrorListener { error -> Timber.wtf("Dexter permissions error $error") }
            .check()
    }

    private fun startSingleActivity(clazz: Class<*>) {
        val intent = Intent(this@MainActivity, clazz)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun showMetadataNodeFailure() {
        if (!isFinishing) {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.metadata_load_failure)
                .setPositiveButton(R.string.retry) { _, _ -> presenter.initMetadataElements() }
                .setNegativeButton(R.string.exit) { _, _ -> presenter.clearLoginState() }
                .setCancelable(false)
                .create()
                .show()
        }
    }

    override fun kickToLauncherPage() {
        startSingleActivity(LauncherActivity::class.java)
    }

    override fun launchKyc(campaignType: CampaignType) {
        startActivityForResult(KycNavHostActivity.intentArgs(this, campaignType), KYC_STARTED)
    }

    override fun launchSwap(defCurrency: String, targetCrypto: CryptoCurrency?) {
        HomebrewNavHostActivity.start(
            (activity as? Context) ?: return,
            defCurrency,
            targetCrypto
        )
    }

    override fun launchSwapOrKyc(targetCurrency: CryptoCurrency?) {
        presenter.startSwapOrKyc(targetCurrency)
    }

    @ButWhy("What does this really do? Who calls it?")
    override fun refreshDashboard() {
        replaceContentFragment(DashboardFragment.newInstance())
    }

    @CommonCode("Move to base")
    override fun showProgressDialog(@StringRes message: Int) {
        hideProgressDialog()
        if (!isFinishing) {
            progressDlg = MaterialProgressDialog(this).apply {
                setCancelable(false)
                setMessage(message)
                show()
            }
        }
    }

    @CommonCode("Move to base")
    override fun hideProgressDialog() {
        if (!isFinishing && progressDlg != null) {
            progressDlg!!.dismiss()
            progressDlg = null
        }
    }

    override fun onScanInput(strUri: String) {
        doScanInput(strUri)
    }

    override fun getStartIntent(): Intent {
        return intent
    }

    override fun setBuySellEnabled(enabled: Boolean, useWebView: Boolean) {
        setBuyBitcoinVisible(enabled)
    }

    override fun showTradeCompleteMsg(txHash: String) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(getString(R.string.trade_complete))
            .setMessage(R.string.trade_complete_details)
            .setCancelable(false)
            .setPositiveButton(R.string.ok_cap, null)
            .setNegativeButton(R.string.view_details) { _, _ ->
                startBalanceFragment()
                // Show transaction detail
                val bundle = Bundle()
                bundle.putString(BalanceFragment.KEY_TRANSACTION_HASH, txHash)
                TransactionDetailActivity.start(this, bundle)
            }.show()
    }

    private fun setBuyBitcoinVisible(visible: Boolean) {
        val menu = menu
        menu.findItem(R.id.nav_buy).isVisible = visible
    }

    private fun setPitVisible(visible: Boolean) {
        val menu = menu
        menu.findItem(R.id.nav_the_pit).isVisible = visible
    }

    override fun setWebViewLoginDetails(loginDetails: WebViewLoginDetails) {
        Timber.d("setWebViewLoginDetails: called")
        webViewLoginDetails = loginDetails
    }

    override fun clearAllDynamicShortcuts() {
        if (AndroidUtils.is25orHigher()) {
            getSystemService(ShortcutManager::class.java)!!.removeAllDynamicShortcuts()
        }
    }

    @ButWhy("Who calls this... it looks crazy")
    override fun onChangeFeeClicked() {
        val fragment = supportFragmentManager
            .findFragmentByTag(SendFragment::class.java.simpleName) as SendFragment
        fragment.onChangeFeeClicked()
    }

    @ButWhy("Who calls this")
    override fun onSendClicked() {
        val fragment = supportFragmentManager
            .findFragmentByTag(SendFragment::class.java.simpleName) as SendFragment
        fragment.onSendClicked()
    }

    override fun showTestnetWarning() {
        val snack = Snackbar.make(
            binding.coordinatorLayout,
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

    override fun showCustomPrompt(dlgFn: PromptDlgFactory) {
        if (!isFinishing) {
            dlgFn(this).apply {
                show(supportFragmentManager, tag)
            }
        }
    }

    override fun createPresenter() = mainPresenter
    override fun getView() = this

    override fun showSecondPasswordDialog() {
        val editText = AppCompatEditText(this)
        editText.setHint(R.string.password)
        editText.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD or
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        val frameLayout = ViewUtils.getAlertDialogPaddedView(this, editText)

        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.eth_now_supporting)
            .setMessage(R.string.eth_second_password_prompt)
            .setView(frameLayout)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { dialog, which ->
                ViewUtils.hideKeyboard(this)
                presenter.decryptAndSetupMetadata(editText.text.toString())
            }
            .create()
            .show()
    }

    override fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType)
    }

    override fun displayDialog(title: Int, message: Int) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun displayLockboxMenu(lockboxAvailable: Boolean) {
        menu.findItem(R.id.nav_lockbox).isVisible = lockboxAvailable
    }

    override fun showHomebrewDebugMenu() {
        menu.findItem(R.id.nav_exchange_homebrew_debug).isVisible = true
    }

    fun setOnTouchOutsideViewListener(
        view: View,
        onTouchOutsideViewListener: OnTouchOutsideViewListener
    ) {
        touchOutsideViews[view] = onTouchOutsideViewListener
    }

    override fun showNavigation() {
        bottom_navigation.restoreBottomNavigation()
        bottom_navigation.isBehaviorTranslationEnabled = true
    }

    override fun hideNavigation() {
        bottom_navigation.hideBottomNavigation()
        bottom_navigation.isBehaviorTranslationEnabled = false
    }

    override fun gotoSendFor(cryptoCurrency: CryptoCurrency) {
        presenter.setCryptoCurrency(cryptoCurrency)
        startSendFragment(null)
    }

    private fun startSendFragment(scanData: String?) {
        setCurrentTabItem(ITEM_SEND)

        ViewUtils.setElevation(binding.appbarLayout, 0f)

        val sendFragment = SendFragment.newInstance(scanData)
        replaceContentFragment(sendFragment)
    }

    override fun gotoReceiveFor(cryptoCurrency: CryptoCurrency) {
        presenter.setCryptoCurrency(cryptoCurrency)

        startReceiveFragment()
    }

    private fun startReceiveFragment() {
        val receiveFragment = ReceiveFragment.newInstance(selectedAccountFromFragments)
        replaceContentFragment(receiveFragment)
    }

    override fun gotoDashboard() {
        bottom_navigation.currentItem = ITEM_HOME
    }

    private fun startDashboardFragment() {
        val fragment = DashboardFragment.newInstance()
        replaceContentFragment(fragment)
    }

    override fun gotoTransactionsFor(cryptoCurrency: CryptoCurrency) {
        presenter.setCryptoCurrency(cryptoCurrency)
        bottom_navigation.currentItem = ITEM_ACTIVITY
    }

    override fun startBalanceFragment() {
        val fragment = BalanceFragment.newInstance(true)
        replaceContentFragment(fragment)
        toolbar_general.title = ""
    }

    override fun launchKycIntro() {
        val swapIntroFragment = SwapIntroFragment.newInstance()
        replaceContentFragment(swapIntroFragment)
    }

    override fun onStartBuySell() {
        BuySellLauncherActivity.start(this)
    }

    override fun launchSwapIntro() {
        setCurrentTabItem(ITEM_SWAP)

        ViewUtils.setElevation(binding.appbarLayout, 0f)

        val swapIntroFragment = SwapIntroFragment.newInstance()
        replaceContentFragment(swapIntroFragment)
    }

    override fun shouldIgnoreDeepLinking() =
        (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0

    private fun replaceContentFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.content_frame, fragment, fragment.javaClass.simpleName)
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

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            @Suppress("SENSELESS_COMPARISON") // This was probably here for a (bugfix) reason, so leave for now
            if (activity == null) return

            when (intent.action ?: return) {
                ACTION_SEND -> requestScan()
                ACTION_RECEIVE -> {
                    // Used from onboarding
                    presenter.setCryptoCurrency(CryptoCurrency.BTC)
                    bottom_navigation.currentItem = ITEM_RECEIVE
                }
                ACTION_BUY -> presenter.routeToBuySell()
            }
        }

        fun registerIntents(ctx: Context) {
            val broadcastManager = LocalBroadcastManager.getInstance(ctx)
            broadcastManager.registerReceiver(this, IntentFilter(ACTION_SEND))
            broadcastManager.registerReceiver(this, IntentFilter(ACTION_RECEIVE))
            broadcastManager.registerReceiver(this, IntentFilter(ACTION_BUY))
        }
    }

    companion object {

        val TAG = MainActivity::class.java.simpleName!!

        const val ACTION_SEND = "info.blockchain.wallet.ui.BalanceFragment.SEND"
        const val ACTION_RECEIVE = "info.blockchain.wallet.ui.BalanceFragment.RECEIVE"
        const val ACTION_BUY = "info.blockchain.wallet.ui.BalanceFragment.BUY"

        private const val REQUEST_BACKUP = 2225

        const val SCAN_URI = 2007
        const val ACCOUNT_EDIT = 2008
        const val SETTINGS_EDIT = 2009
        const val KYC_STARTED = 2011

        // Keep these constants - the position of the toolbar items - and the generation of the toolbar items
        // together.
        private const val ITEM_ACTIVITY = 0
        private const val ITEM_SWAP = 1
        private const val ITEM_HOME = 2
        private const val ITEM_SEND = 3
        private const val ITEM_RECEIVE = 4

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
                R.string.toolbar_cmd_send,
                R.drawable.ic_vector_toolbar_send,
                R.color.white
            ), AHBottomNavigationItem(
                R.string.toolbar_cmd_receive,
                R.drawable.ic_vector_toolbar_receive,
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
}
