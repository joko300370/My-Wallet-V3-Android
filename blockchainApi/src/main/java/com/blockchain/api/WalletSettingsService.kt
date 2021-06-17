package com.blockchain.api

import com.blockchain.api.wallet.WalletApiInterface
import com.blockchain.api.wallet.data.WalletSettingsDto
import io.reactivex.Single

class WalletSettingsService internal constructor(
    private val api: WalletApiInterface,
    private val apiCode: String
) {
    fun fetchWalletSettings(
        guid: String,
        sharedKey: String
    ): Single<WalletSettingsDto> =
        api.fetchSettings(guid = guid, sharedKey = sharedKey, apiCode = apiCode)
}