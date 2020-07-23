package piuk.blockchain.android.coincore.erc20.usdt

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.erc20.Erc20NonCustodialAccount
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

internal class UsdtCryptoWalletAccount(
    label: String,
    private val address: String,
    override val erc20Account: Erc20Account,
    exchangeRates: ExchangeRateDataManager,
    override val feeAsset: CryptoCurrency? = CryptoCurrency.ETHER
) : Erc20NonCustodialAccount(
    CryptoCurrency.USDT,
    label,
    exchangeRates
) {
    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(
            UsdtAddress(address, label)
        )

    override val actions: AvailableActions
        get() = availableActions

    private val availableActions = setOf(
        AssetAction.ViewActivity,
        AssetAction.Swap
    )
}
