package piuk.blockchain.androidcoreui.ui.base

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.annotation.CallSuper
import android.view.WindowManager
import com.blockchain.koin.scopedInjectActivity
import com.blockchain.ui.password.SecondPasswordHandler
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.data.access.LogoutTimer
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ApplicationLifeCycle

/**
 * A base Activity for all activities which need auth timeouts & screenshot prevention
 */
@Deprecated("Use BlockchainActivity in :app instead")
abstract class BaseAuthActivity : ToolBarActivity() {

    private val environment: EnvironmentConfig by inject()

    private val logoutTimer: LogoutTimer by inject()

    protected val prefs: PersistentPrefs by inject()

    protected val secondPasswordHandler: SecondPasswordHandler by scopedInjectActivity()

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockScreenOrientation()
    }

    /**
     * Allows you to disable Portrait orientation lock on a per-Activity basis.
     */
    protected open fun lockScreenOrientation() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        stopLogoutTimer()
        ApplicationLifeCycle.getInstance().onActivityResumed()

        if (prefs.isUnderTest || (prefs.areScreenshotAllowed && !enforceFlagSecure())) {
            enableScreenshots()
        } else {
            disallowScreenshots()
        }
    }

    private val PersistentPrefs.areScreenshotAllowed
        get() = getValue(PersistentPrefs.KEY_SCREENSHOTS_ENABLED, false)

    /**
     * Allows us to enable screenshots on all pages, unless this is overridden in an Activity and
     * returns true. Some pages are fine to be screenshot, but this lets us keep it permanently
     * disabled on some more sensitive pages.
     *
     * @return False by default. If false, screenshots & screen recording will be allowed on the
     * page if the user so chooses.
     */
    protected open fun enforceFlagSecure(): Boolean {
        return false
    }

    @CallSuper
    override fun onPause() {
        super.onPause()
        startLogoutTimer()
        ApplicationLifeCycle.getInstance().onActivityPaused()
    }

    /**
     * Starts the logout timer. Override in an activity if timeout is not needed.
     */
    protected open fun startLogoutTimer() {
        logoutTimer.start()
    }

    private fun stopLogoutTimer() {
        logoutTimer.stop()
    }

    private fun disallowScreenshots() {
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun enableScreenshots() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
