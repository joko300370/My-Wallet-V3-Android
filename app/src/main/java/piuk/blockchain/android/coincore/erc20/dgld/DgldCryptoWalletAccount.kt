package piuk.blockchain.android.coincore.erc20.dgld

import com.blockchain.preferences.WalletStatus
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.erc20.Erc20NonCustodialAccount
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class DgldCryptoWalletAccount(
    payloadManager: PayloadDataManager,
    label: String,
    private val address: String,
    override val erc20Account: Erc20Account,
    fees: FeeDataManager,
    exchangeRates: ExchangeRateDataManager,
    walletPreferences: WalletStatus,
    custodialWalletManager: CustodialWalletManager
) : Erc20NonCustodialAccount(
    payloadManager,
    CryptoCurrency.DGLD,
    fees,
    label,
    exchangeRates,
    walletPreferences,
    custodialWalletManager
) {

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(
            DgldAddress(address, label)
        )
}