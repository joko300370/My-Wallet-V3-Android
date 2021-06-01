package com.blockchain.preferences

interface AuthPrefs {
    var encodedPin: String
    var biometricsEnabled: Boolean

    fun clearEncodedPin()

    val encodedKeyName: String

    var sharedKey: String
    var walletGuid: String
    var encryptedPassword: String
    var pinFails: Int
    var sessionId: String

    fun clearSessionId()
}