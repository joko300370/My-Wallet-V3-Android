package piuk.blockchain.android.ui.kyc.email.entry

import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.androidcore.data.settings.Email

data class EmailVeriffState(
    val email: Email? = null
) : MviState