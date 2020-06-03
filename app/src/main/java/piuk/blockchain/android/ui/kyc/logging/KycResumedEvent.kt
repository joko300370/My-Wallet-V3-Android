package piuk.blockchain.android.ui.kyc.logging

import piuk.blockchain.android.ui.kyc.reentry.ReentryPoint
import piuk.blockchain.androidcoreui.utils.logging.LoggingEvent

fun kycResumedEvent(entryPoint: ReentryPoint) =
    LoggingEvent("User Resumed KYC flow", mapOf("User resumed KYC" to entryPoint.entryPoint))
