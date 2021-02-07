package piuk.blockchain.android.ui.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import info.blockchain.wallet.payload.PayloadManager
import kotlinx.android.synthetic.main.toolbar_general.*
import org.koin.android.ext.android.get
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.backup.completed.BackupWalletCompletedFragment
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingFragment
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.androidcore.utils.helperfunctions.consume

class BackupWalletActivity : BlockchainActivity() {

    private val payloadManger: PayloadManager by scopedInject()

    override val alwaysDisableScreenshots: Boolean
        get() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup_wallet)
        get<Analytics>().logEvent(AnalyticsEvents.Backup)

        setupToolbar(toolbar_general, R.string.backup_wallet_title)

        if (isBackedUp()) {
            startFragment(
                BackupWalletCompletedFragment.newInstance(),
                BackupWalletCompletedFragment.TAG
            )
        } else {
            startFragment(BackupWalletStartingFragment(), BackupWalletStartingFragment.TAG)
        }
    }

    private fun startFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment)
            .addToBackStack(tag)
            .commit()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount <= 1) {
            setResult(if (isBackedUp()) RESULT_OK else Activity.RESULT_CANCELED)
            finish()
        } else {
            supportFragmentManager.popBackStack()
        }
    }

    override fun onSupportNavigateUp() =
        consume { onBackPressed() }

    private fun isBackedUp() = payloadManger.isWalletBackedUp

    companion object {
        fun start(context: Context) {
            val starter = Intent(context, BackupWalletActivity::class.java)
            context.startActivity(starter)
        }

        fun startForResult(fragment: Fragment, requestCode: Int) {
            fragment.startActivityForResult(
                Intent(fragment.context, BackupWalletActivity::class.java),
                requestCode
            )
        }
    }
}