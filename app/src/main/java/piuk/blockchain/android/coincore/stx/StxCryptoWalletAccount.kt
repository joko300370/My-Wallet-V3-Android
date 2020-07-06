package piuk.blockchain.android.coincore.stx

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

class StxCryptoWalletAccount(
    override val label: String,
    private val address: String,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoNonCustodialAccount(CryptoCurrency.STX) {

    override val isFunded: Boolean
        get() = false

    override val isDefault: Boolean = true // Only one account ever, so always default

    override val balance: Single<CryptoValue>
        get() = TODO("not implemented")

    override val receiveAddress: Single<ReceiveAddress>
        get() = TODO("not implemented")

    override val activity: Single<ActivitySummaryList>
        get() = Single.just(emptyList())
}