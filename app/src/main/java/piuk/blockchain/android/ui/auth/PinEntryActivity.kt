package piuk.blockchain.android.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import android.view.MotionEvent
import android.view.Window
import androidx.databinding.DataBindingUtil
import androidx.viewpager.widget.ViewPager
import org.koin.android.ext.android.inject
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.data.coinswebsocket.service.CoinsWebSocketService
import piuk.blockchain.android.databinding.ActivityPinEntryBinding
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveFragment
import piuk.blockchain.android.util.OSUtil
import piuk.blockchain.android.util.start
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.annotations.Thunk
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ApplicationLifeCycle
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.OverlayDetection

class PinEntryActivity : BaseAuthActivity(), PinEntryFragment.OnPinEntryFragmentInteractionListener,
    ViewPager.OnPageChangeListener {

    private val osUtil: OSUtil by inject()

    private val overlayDetection: OverlayDetection by inject()
    private val loginState: AccessState by inject()

    @Thunk
    private lateinit var binding: ActivityPinEntryBinding

    private var backPressed: Long = 0
    private val pinEntryFragment: PinEntryFragment by lazy {
        PinEntryFragment.newInstance(!shouldHideSwipeToReceive(), isAfterCreateWallet)
    }

    private val isAfterCreateWallet: Boolean by unsafeLazy {
        intent.getBooleanExtra(EXTRA_IS_AFTER_WALLET_CREATION, false)
    }

    private val isCreatingNewPin: Boolean
        get() = prefs.pinId.isEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_pin_entry)

        val fragmentPagerAdapter: FragmentPagerAdapter
        if (shouldHideSwipeToReceive()) {
            // Don't bother instantiating the QR fragment + Presenter if not necessary
            fragmentPagerAdapter = SwipeToReceiveFragmentPagerAdapter(
                supportFragmentManager,
                pinEntryFragment,
                Fragment())

            lockViewpager()
        } else {
            fragmentPagerAdapter = SwipeToReceiveFragmentPagerAdapter(
                supportFragmentManager,
                pinEntryFragment,
                SwipeToReceiveFragment.newInstance())

            startWebSocketService()
        }

        binding.viewpager.offscreenPageLimit = 2
        binding.viewpager.addOnPageChangeListener(this)
        binding.viewpager.adapter = fragmentPagerAdapter
    }

    private fun shouldHideSwipeToReceive(): Boolean {
        return (intent.hasExtra(KEY_VALIDATING_PIN_FOR_RESULT) ||
                isCreatingNewPin ||
                !prefs.getValue(PersistentPrefs.KEY_SWIPE_TO_RECEIVE_ENABLED, true))
    }

    private fun lockViewpager() {
        binding.viewpager.lockToCurrentPage()
    }

    override fun onSwipePressed() {
        binding.viewpager.currentItem = 1
    }

    override fun onBackPressed() {
        when {
            binding.viewpager.currentItem != 0 -> {
                binding.viewpager.currentItem = 0
            }
            pinEntryFragment.isValidatingPinForResult -> {
                finishWithResultCanceled()
            }
            pinEntryFragment.allowExit() -> {
                if (backPressed + BuildConfig.EXIT_APP_COOLDOWN_MILLIS > System.currentTimeMillis()) {
                    loginState.logout()
                    return
                } else {
                    ToastCustom.makeText(this,
                        getString(R.string.exit_confirm),
                        ToastCustom.LENGTH_SHORT,
                        ToastCustom.TYPE_GENERAL)
                }

                backPressed = System.currentTimeMillis()
            }
        }
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        // No-op
    }

    override fun onPageSelected(position: Int) {
        pinEntryFragment.resetPinEntry()
    }

    override fun onPageScrollStateChanged(state: Int) {
        // No-op
    }

    private fun finishWithResultCanceled() {
        val intent = Intent()
        setResult(RESULT_CANCELED, intent)
        finish()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Test for screen overlays before user enters PIN
        return overlayDetection.detectObscuredWindow(this, event) ||
                super.dispatchTouchEvent(event)
    }

    override fun enforceFlagSecure(): Boolean {
        return true
    }

    private fun startWebSocketService() {
        if (ApplicationLifeCycle.getInstance().isForeground) {
            CoinsWebSocketService::class.java.start(this, osUtil)
        }
    }

    private class SwipeToReceiveFragmentPagerAdapter internal constructor(
        fm: FragmentManager,
        private val pinEntryFragment: PinEntryFragment,
        private val swipeToReceiveFragment: Fragment
    ) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> pinEntryFragment
                else -> swipeToReceiveFragment
            }
        }

        override fun getCount(): Int {
            return NUM_ITEMS
        }

        companion object {

            private const val NUM_ITEMS = 2
        }
    }

    companion object {

        const val REQUEST_CODE_UPDATE = 188
        private const val EXTRA_IS_AFTER_WALLET_CREATION = "piuk.blockchain.android.EXTRA_IS_AFTER_WALLET_CREATION"

        fun start(context: Context) {
            val intent = Intent(context, PinEntryActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        fun startAfterWalletCreation(context: Context) {
            val intent = Intent(context, PinEntryActivity::class.java)
            intent.putExtra(EXTRA_IS_AFTER_WALLET_CREATION, true)
            context.startActivity(intent)
        }
    }
}