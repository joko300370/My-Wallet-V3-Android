package com.blockchain.preferences

interface AuthPrefs {
    var encodedPin: String
    var biometricsEnabled: Boolean

    fun clearEncodedPin()

    val encodedKeyName: String

    var sharedKey: String
    var walletGuid: String
}