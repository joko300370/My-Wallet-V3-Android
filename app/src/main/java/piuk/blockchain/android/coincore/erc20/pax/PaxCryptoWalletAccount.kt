package piuk.blockchain.android.coincore.erc20.pax

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.erc20.Erc20NonCustodialAccount
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class PaxCryptoWalletAccount(
    currencyPrefs: CurrencyPrefs,
    custodialWalletManager: CustodialWalletManager,
    payloadManager: PayloadDataManager,
    label: String,
    private val address: String,
    override val erc20Account: Erc20Account,
    fees: FeeDataManager,
    exchangeRates: ExchangeRateDataManager
) : Erc20NonCustodialAccount(
    currencyPrefs,
    custodialWalletManager,
    payloadManager,
    CryptoCurrency.PAX,
    fees,
    label,
    exchangeRates
) {
    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(
            PaxAddress(address, label)
        )
}
