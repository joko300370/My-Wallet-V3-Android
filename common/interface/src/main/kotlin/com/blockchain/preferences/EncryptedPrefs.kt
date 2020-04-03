package com.blockchain.preferences

interface EncryptedPrefs {
    var backupEncryptedPassword: String?
    var backupEncryptedSharedKey: String?
    var backupEncryptedGuid: String?
    var backupPinIdentifier: String?
    var backupEnabled: Boolean
    val hasBackup: Boolean
    fun clearBackup()
}
