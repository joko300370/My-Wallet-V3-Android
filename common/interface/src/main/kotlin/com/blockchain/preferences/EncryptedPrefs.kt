package com.blockchain.preferences

import info.blockchain.balance.CryptoCurrency

interface EncryptedPrefs {
    var backupEncryptedPassword: String?
    var backupEncryptedSharedKey: String?
    var backupEncryptedGuid: String?
    var backupPinIdentifier: String?
    var backupEnabled: Boolean
    val hasBackup: Boolean
    fun clearBackup()
}
