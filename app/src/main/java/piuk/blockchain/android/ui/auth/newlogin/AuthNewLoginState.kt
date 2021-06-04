package piuk.blockchain.android.ui.auth.newlogin

import com.blockchain.preferences.BrowserIdentity
import piuk.blockchain.android.ui.base.mvi.MviState

data class AuthNewLoginState(
    val browserIdentity: BrowserIdentity = BrowserIdentity(""),
    val message: SecureChannelBrowserMessage = SecureChannelBrowserMessage("", "", 0),
    val items: List<AuthNewLoginDetailsType> = listOf(),
    val location: String = "",
    val ip: String = "",
    val info: String = "",
    val lastUsed: Long = 0,
    val forcePin: Boolean = false,
    val enableApproval: Boolean = false
) : MviState