package piuk.blockchain.android.util

import android.app.backup.BackupAgentHelper
import android.app.backup.SharedPreferencesBackupHelper

class BackupManager: BackupAgentHelper() {
    override fun onCreate() {
        // The default shared preferences name
        // https://stackoverflow.com/a/6310080
        val prefs = packageName + "_preferences"
        SharedPreferencesBackupHelper(this, prefs).also {
            addHelper("prefs", it)
        }
    }
}