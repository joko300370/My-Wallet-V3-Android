package com.blockchain.preferences

import info.blockchain.balance.CryptoCurrency

interface EncryptedPrefs {
    var backupEncryptedPassword: String?
    var backupEncryptedSharedKey: String?
    var backupEncryptedGuid: String?
    var backupPinIdentifier: String?
    val hasBackup: Boolean
    fun clearBackup()
}
