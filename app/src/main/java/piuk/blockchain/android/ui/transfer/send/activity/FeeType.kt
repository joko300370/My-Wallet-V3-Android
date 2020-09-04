package piuk.blockchain.android.ui.transfer.send.activity

import androidx.annotation.IntDef

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

internal object FeeType {

    const val FEE_OPTION_REGULAR = 0
    const val FEE_OPTION_PRIORITY = 1
    const val FEE_OPTION_CUSTOM = 2

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(FEE_OPTION_REGULAR, FEE_OPTION_PRIORITY, FEE_OPTION_CUSTOM)
    internal annotation class FeePriorityDef
}
