package piuk.blockchain.android.coincore.alg

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import org.apache.commons.lang3.NotImplementedException
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountNonCustodialBase
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

internal class AlgoCryptoWalletAccount(
    override val label: String,
    override val isDefault: Boolean = true,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoSingleAccountNonCustodialBase(CryptoCurrency.ALGO) {

    override val balance: Single<CryptoValue>
        get() = Single.just(CryptoValue.ZeroAlg)

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.error(NotImplementedException("Need implementation of ALGO receive"))

    override val activity: Single<ActivitySummaryList>
        get() = Single.just(emptyList())

    override val isFunded: Boolean
        get() = false
}