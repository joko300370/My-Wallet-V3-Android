package piuk.blockchain.android.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import com.blockchain.nabu.models.data.LinkBankTransfer
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.ErrorState
import piuk.blockchain.android.simplebuy.yodlee.YodleeLinkingFlowNavigator
import piuk.blockchain.android.simplebuy.yodlee.YodleeSplashFragment
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity

class SettingsActivity : BaseAuthActivity(),YodleeLinkingFlowNavigator {

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

    override fun launchYodleeSplash(fastLinkUrl: String, accessToken: String, configName: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_settings, YodleeSplashFragment.newInstance(fastLinkUrl, accessToken, configName))
            .addToBackStack(YodleeSplashFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    override fun launchYodleeWebview(fastLinkUrl: String, accessToken: String, configName: String) {
        TODO("Not yet implemented")
    }

    override fun linkBankWithPartner(bankTransfer: LinkBankTransfer) {
        TODO("Not yet implemented")
    }

    override fun launchBankLinking(accountProviderId: String, accountId: String) {
        TODO("Not yet implemented")
    }

    override fun launchBankLinkingWithError(errorState: ErrorState) {
        TODO("Not yet implemented")
    }

    override fun retry() {
        TODO("Not yet implemented")
    }

    override fun bankLinkingFinished() {
        TODO("Not yet implemented")
    }

    override fun bankLinkingCancelled() {
        TODO("Not yet implemented")
    }
}
