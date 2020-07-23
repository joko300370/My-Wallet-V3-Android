package piuk.blockchain.android.coincore.erc20.pax

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.erc20.Erc20NonCustodialAccountBase
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

internal class PaxCryptoWalletAccount(
    label: String,
    private val address: String,
    override val erc20Account: Erc20Account,
    exchangeRates: ExchangeRateDataManager,
    override val feeAsset: CryptoCurrency? = CryptoCurrency.ETHER
) : Erc20NonCustodialAccountBase(
    CryptoCurrency.PAX,
    label,
    exchangeRates
) {

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(
            PaxAddress(address, label)
        )
}
