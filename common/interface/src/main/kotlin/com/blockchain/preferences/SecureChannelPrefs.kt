package com.blockchain.preferences

import kotlinx.serialization.Serializable

interface SecureChannelPrefs {
    val deviceKey: String

    fun getBrowserIdentity(pubkeyHash: String): BrowserIdentity?
    fun addBrowserIdentity(browserIdentity: BrowserIdentity)
    fun removeBrowserIdentity(pubkeyHash: String)
    fun updateBrowserIdentityUsedTimestamp(pubkeyHash: String)
    fun addBrowserIdentityAuthorization(pubkeyHash: String, authorization: Authorization)
    fun pruneBrowserIdentities()
}

@Serializable
data class BrowserIdentityMapping(
    val mapping: MutableMap<String, BrowserIdentity>
)

// If an action from a known device comes in, that we haven't authorized yet,
// we should force the IP check and also show some more information about the action.
@Serializable
enum class Authorization {
    LOGIN_WALLET;
}

@Serializable
data class BrowserIdentity(
    val pubkey: String,
    var lastUsed: Long = 0,
    // Due to the message flow, the scanning of a QR code might not lead to the browser
    // sending back the handshake message correctly. We don't want it to then send it
    // days later, since that would make for some weird UX, and maybe even an attack vector.
    // Instead, browser identities that weren't used yet, should be pruned after a few minutes.
    var scanned: Long = System.currentTimeMillis(),
    var authorized: MutableSet<Authorization> = mutableSetOf()
)