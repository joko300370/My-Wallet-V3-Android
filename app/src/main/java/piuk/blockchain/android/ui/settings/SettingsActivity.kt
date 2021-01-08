package piuk.blockchain.android.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity

class SettingsActivity : BaseAuthActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setupToolbar()
    }

    override fun enforceFlagSecure(): Boolean = true

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_general)
        setupToolbar(toolbar, R.string.action_settings)
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    companion object {

        fun startFor2Fa(context: Context) {
            val starter = Intent(context, SettingsActivity::class.java)
            starter.putExtras(Bundle().apply {
                this.putBoolean(SettingsFragment.EXTRA_SHOW_TWO_FA_DIALOG, true)
            })
            context.startActivity(starter)
        }

        fun startForVerifyEmail(context: Context) {
            val starter = Intent(context, SettingsActivity::class.java)
            starter.putExtras(Bundle().apply {
                this.putBoolean(SettingsFragment.EXTRA_SHOW_ADD_EMAIL_DIALOG, true)
            })
            context.startActivity(starter)
        }
    }
}
