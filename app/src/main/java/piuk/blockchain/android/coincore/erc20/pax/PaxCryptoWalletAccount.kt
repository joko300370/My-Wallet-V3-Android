package piuk.blockchain.android.coincore.erc20.pax

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.erc20.Erc20NonCustodialAccount
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager

internal class PaxCryptoWalletAccount(
    label: String,
    private val address: String,
    override val erc20Account: Erc20Account,
    feeDataManager: FeeDataManager,
    exchangeRates: ExchangeRateDataManager
) : Erc20NonCustodialAccount(
    CryptoCurrency.PAX,
    label,
    feeDataManager,
    exchangeRates
) {
    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(
            PaxAddress(address, label)
        )
}
