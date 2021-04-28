package info.blockchain.api

import info.blockchain.api.wallet.WalletApiInterface
import info.blockchain.api.wallet.data.WalletSettingsDto
import io.reactivex.Single
import retrofit2.Retrofit

class WalletSettingsService(
    retrofitApiRoot: Retrofit,
    private val apiCode: String
) {
    private val walletApi: WalletApiInterface = retrofitApiRoot.create(WalletApiInterface::class.java)

    fun fetchWalletSettings(
        guid: String,
        sharedKey: String
    ): Single<WalletSettingsDto> =
        walletApi.fetchSettings(guid = guid, sharedKey = sharedKey, apiCode = apiCode)
}