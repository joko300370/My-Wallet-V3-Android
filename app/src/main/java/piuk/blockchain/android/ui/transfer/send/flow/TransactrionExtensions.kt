package piuk.blockchain.android.ui.transfer.send.flow

import android.content.res.Resources
import kotlinx.android.synthetic.main.dialog_send_enter_amount.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.transfer.send.SendErrorState

internal fun SendErrorState.toString(assetTicker: String, resources: Resources): String? =
    when (this) {
        SendErrorState.NONE -> null
        SendErrorState.MAX_EXCEEDED -> resources.getString(R.string.send_enter_amount_error_max, assetTicker)
        SendErrorState.MIN_REQUIRED -> resources.getString(R.string.send_enter_amount_error_min, assetTicker)
        SendErrorState.INVALID_ADDRESS,
        SendErrorState.ADDRESS_IS_CONTRACT,
        SendErrorState.INVALID_PASSWORD,
        SendErrorState.FEE_REQUEST_FAILED -> throw NotImplementedError("Not expected here")
        SendErrorState.NOT_ENOUGH_GAS -> TODO()
        SendErrorState.UNEXPECTED_ERROR -> TODO()

    }