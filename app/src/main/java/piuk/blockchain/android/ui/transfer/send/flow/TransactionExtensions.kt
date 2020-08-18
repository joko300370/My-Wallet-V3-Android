package piuk.blockchain.android.ui.transfer.send.flow

import android.content.res.Resources
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.transfer.send.SendErrorState

internal fun SendErrorState.toString(assetTicker: String, resources: Resources, optionalMessage: String = ""): String? =
    when (this) {
        SendErrorState.NONE -> null
        SendErrorState.MAX_EXCEEDED -> resources.getString(R.string.send_enter_amount_error_max, assetTicker)
        SendErrorState.MIN_REQUIRED -> resources.getString(R.string.send_enter_amount_error_min, assetTicker)
        SendErrorState.INVALID_ADDRESS -> resources.getString(R.string.send_error_not_valid_asset_address, assetTicker)
        SendErrorState.ADDRESS_IS_CONTRACT -> resources.getString(R.string.send_error_address_is_eth_contract)
        SendErrorState.INVALID_PASSWORD -> resources.getString(R.string.send_enter_invalid_password)
        SendErrorState.NOT_ENOUGH_GAS -> resources.getString(R.string.send_enter_insufficient_gas)
        SendErrorState.UNEXPECTED_ERROR -> resources.getString(R.string.send_enter_unexpected_error)
        SendErrorState.MIN_DEPOSIT -> "Minimum deposit of $optionalMessage required"
    }
