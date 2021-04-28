package piuk.blockchain.android.ui.auth.newlogin

import piuk.blockchain.android.R
import java.text.SimpleDateFormat
import java.util.Locale

interface AuthNewLoginDetailsType {
    val headerTextRes: Int
    val value: String
}

class AuthNewLoginLocation(private val location: String) : AuthNewLoginDetailsType {

    override val headerTextRes: Int
        get() = R.string.auth_new_login_location_header

    override val value: String
        get() = location
}

class AuthNewLoginIpAddress(private val ip: String) : AuthNewLoginDetailsType {
    override val headerTextRes: Int
        get() = R.string.auth_new_login_browser_ip_header

    override val value: String
        get() = ip
}

class AuthNewLoginBrowserInfo(private val info: String) : AuthNewLoginDetailsType {
    override val headerTextRes: Int
        get() = R.string.auth_new_login_browser_info_header

    override val value: String
        get() = info
}

class AuthNewLoginLastLogin(private val lastUsed: Long) : AuthNewLoginDetailsType {

    private val sdf = SimpleDateFormat("MMMM dd hh:mm a z", Locale.getDefault())

    override val headerTextRes: Int
        get() = R.string.auth_new_login_last_used_header

    override val value: String
        get() = sdf.format(lastUsed)
}