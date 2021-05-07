package piuk.blockchain.android.ui.pairingcode

import android.graphics.Bitmap
import piuk.blockchain.android.ui.base.mvi.MviState

sealed class QrCodeImageStatus {
    object NotInitialised : QrCodeImageStatus()
    object Loading : QrCodeImageStatus()
    object Error : QrCodeImageStatus()
    data class Ready(val qrCodeBitmap: Bitmap) : QrCodeImageStatus()
    data class Hidden(val qrCodeBitmap: Bitmap) : QrCodeImageStatus()
}

data class PairingState(
    val imageStatus: QrCodeImageStatus = QrCodeImageStatus.NotInitialised
) : MviState {
    val showQrCode: Boolean
        get() = imageStatus !is QrCodeImageStatus.NotInitialised && imageStatus !is QrCodeImageStatus.Hidden
}