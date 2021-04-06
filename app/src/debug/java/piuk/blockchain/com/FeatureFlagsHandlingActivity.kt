package piuk.blockchain.com

import android.app.LauncherActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.koin.scopedInject
import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import io.reactivex.disposables.CompositeDisposable
import org.koin.android.ext.android.inject
import piuk.blockchain.android.databinding.ActivityLocalFeatureFlagsBinding
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementList
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.utils.PersistentPrefs

class FeatureFlagsHandlingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocalFeatureFlagsBinding
    private val internalFlags: InternalFeatureFlagApi by inject()
    private val compositeDisposable = CompositeDisposable()
    private val prefs: PersistentPrefs by inject()
    private val appUtil: AppUtil by inject()
    private val loginState: AccessState by inject()
    private val crashLogger: CrashLogger by inject()
    private val simpleBuyPrefs: SimpleBuyPrefs by inject()
    private val currencyPrefs: CurrencyPrefs by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalFeatureFlagsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val featureFlags = internalFlags.getAll()
        if (featureFlags.isEmpty()) {
            ToastCustom.makeText(
                this, "There are no local features defined", Toast.LENGTH_SHORT, ToastCustom.TYPE_ERROR
            )
        } else {
            val parent = binding.nestedParent
            featureFlags.entries.forEachIndexed { index, flag ->
                val switch = SwitchCompat(this)
                switch.text = flag.key.name
                switch.isChecked = flag.value
                switch.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        internalFlags.enable(flag.key)
                    } else {
                        internalFlags.disable(flag.key)
                    }
                }
                // adding index specifically so flags show before all other items
                parent.addView(switch, index + 1)
            }
        }

        with(binding) {
            btnRndDeviceId.setOnClickListener { onRndDeviceId() }
            btnResetWallet.setOnClickListener { onResetWallet() }
            btnResetAnnounce.setOnClickListener { onResetAnnounce() }
            btnResetPrefs.setOnClickListener { onResetPrefs() }
            clearSimpleBuyState.setOnClickListener { clearSimpleBuyState() }
            btnStoreLinkId.setOnClickListener { prefs.pitToWalletLinkId = "11111111-2222-3333-4444-55556666677" }
            deviceCurrency.text = "Select a new currency. Current one is ${currencyPrefs.selectedFiatCurrency}"
            firebaseToken.text = prefs.firebaseToken

            radioEur.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    currencyPrefs.selectedFiatCurrency = "EUR"
                    showToast("Currency changed to EUR")
                }
            }

            radioUsd.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    currencyPrefs.selectedFiatCurrency = "USD"
                    showToast("Currency changed to USD")
                }
            }

            radioGbp.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    currencyPrefs.selectedFiatCurrency = "GBP"
                    showToast("Currency changed to GBP")
                }
            }
        }
    }

    private fun showToast(text: String) {
        ToastCustom.makeText(this, text, Toast.LENGTH_SHORT, ToastCustom.TYPE_GENERAL)
    }

    private fun clearSimpleBuyState() {
        simpleBuyPrefs.clearState()
        showToast("Local SB State cleared")
    }

    private fun onRndDeviceId() {
        prefs.qaRandomiseDeviceId = true
        showToast("Device ID randomisation enabled")
    }

    private fun onResetWallet() {
        appUtil.clearCredentialsAndRestart(LauncherActivity::class.java)
        showToast("Wallet reset")
    }

    private fun onResetAnnounce() {
        val announcementList: AnnouncementList by scopedInject()
        val dismissRecorder: DismissRecorder by scopedInject()

        dismissRecorder.undismissAll(announcementList)

        prefs.resetTour()
        showToast("Announcement reset")
    }

    private fun onResetPrefs() {
        prefs.clear()

        crashLogger.logEvent("debug clear prefs. Pin reset")
        loginState.clearPin()

        showToast("Prefs Reset")
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }
}