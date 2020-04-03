package piuk.blockchain.android.util

import android.app.backup.BackupAgentHelper
import android.app.backup.SharedPreferencesBackupHelper

class BackupManager : BackupAgentHelper() {
    override fun onCreate() {
        val prefs = SHARED_PREF_NAME
        SharedPreferencesBackupHelper(this, prefs).also {
            addHelper("prefs", it)
        }
    }

    companion object {
        // TODO what is a good place for this static? Can't import it from here in coreModule file
        val SHARED_PREF_NAME = "shared_pref_backup"
    }
}