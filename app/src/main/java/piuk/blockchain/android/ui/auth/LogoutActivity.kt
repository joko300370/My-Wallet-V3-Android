package piuk.blockchain.android.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.koin.scopedInject
import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.StringQualifier
import piuk.blockchain.android.data.coinswebsocket.service.CoinsWebSocketService
import piuk.blockchain.android.util.OSUtil
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsState
import piuk.blockchain.androidcore.utils.PersistentPrefs

class LogoutActivity : AppCompatActivity() {

    private val ethDataManager: EthDataManager by scopedInject()
    private val paxAccount: Erc20Account by scopedInject(StringQualifier("paxAccount"))
    private val usdtAccount: Erc20Account by scopedInject(StringQualifier("usdtAccount"))
    private val bchDataManager: BchDataManager by scopedInject()
    private val walletOptionsState: WalletOptionsState by scopedInject()
    private val nabuDataManager: NabuDataManager by scopedInject()
    private val osUtil: OSUtil by inject()
    private val loginState: AccessState by inject()
    private val prefs: PersistentPrefs by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action == AccessState.LOGOUT_ACTION) {
            val intent = Intent(this, CoinsWebSocketService::class.java)

            // When user logs out, assume onboarding has been completed
            prefs.setValue(PersistentPrefs.KEY_ONBOARDING_COMPLETE, true)

            if (osUtil.isServiceRunning(CoinsWebSocketService::class.java)) {
                stopService(intent)
            }

            // TODO: 30/06/20 We shouldn't need this any more now we have koin scopes
            // TODO: see Jira AND-3312
            clearData()
        }
    }

    private fun clearData() {
        ethDataManager.clearEthAccountDetails()
        paxAccount.clear()
        usdtAccount.clear()
        bchDataManager.clearBchAccountDetails()
        nabuDataManager.clearAccessToken()

        walletOptionsState.wipe()

        loginState.isLoggedIn = false
        finishAffinity()
    }
}
