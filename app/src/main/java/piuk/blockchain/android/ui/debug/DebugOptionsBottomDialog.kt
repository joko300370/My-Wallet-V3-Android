package piuk.blockchain.android.ui.debug

import android.app.LauncherActivity
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.logging.CrashLogger
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.dialog_debug_options.*
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementList
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.util.AppRate
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.utils.AppUtil
import piuk.blockchain.androidcoreui.utils.extensions.toast

class DebugOptionsBottomDialog : BottomSheetDialogFragment() {

    private val compositeDisposable = CompositeDisposable()
    private val prefs: PersistentPrefs by inject()
    private val appUtil: AppUtil by inject()
    private val loginState: AccessState by inject()
    private val crashLogger: CrashLogger by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_debug_options, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_rnd_device_id.setOnClickListener { onRndDeviceId() }
        btn_reset_wallet.setOnClickListener { onResetWallet() }
        btn_reset_announce.setOnClickListener { onResetAnnounce() }
        btn_reset_prefs.setOnClickListener { onResetPrefs() }

        btn_store_linkId.setOnClickListener { prefs.pitToWalletLinkId = "11111111-2222-3333-4444-55556666677" }

        firebase_token.text = prefs.getValue(PersistentPrefs.KEY_FIREBASE_TOKEN)
    }

    private fun onRndDeviceId() {
        prefs.qaRandomiseDeviceId = true
        context?.toast("Device ID randomisation enabled")
        dismiss()
    }

    private fun onResetWallet() {
        appUtil.clearCredentialsAndRestart(LauncherActivity::class.java)
        dismiss()
    }

    private fun onResetAnnounce() {
        val announcementList: AnnouncementList = get()
        val dismissRecorder: DismissRecorder = get()

        dismissRecorder.undismissAll(announcementList)
        context?.toast("Announcement reset")
        dismiss()
    }

    private fun onResetPrefs() {
        prefs.clear()

        AppRate.reset(context)

        crashLogger.log("debug clear prefs. Pin reset")
        loginState.clearPin()

        context?.toast("Prefs Reset")
        dismiss()
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    companion object {
        fun show(fm: FragmentManager) {
            DebugOptionsBottomDialog().show(fm, "DEBUG_DIALOG")
        }
    }
}