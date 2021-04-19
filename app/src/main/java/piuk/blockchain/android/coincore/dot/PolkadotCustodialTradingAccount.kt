package piuk.blockchain.android.coincore.dot

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount
import piuk.blockchain.android.identity.UserIdentity
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

class PolkadotCustodialTradingAccount(
    cryptoCurrency: CryptoCurrency,
    label: String,
    exchangeRates: ExchangeRateDataManager,
    custodialWalletManager: CustodialWalletManager,
    identity: UserIdentity
) : CustodialTradingAccount(
    asset = cryptoCurrency,
    label = label,
    exchangeRates = exchangeRates,
    custodialWalletManager = custodialWalletManager,
    identity = identity
) {
    override val actions: Single<AvailableActions>
        get() = super.actions.map {
            it.toMutableSet().apply {
                remove(AssetAction.Send)
            }.toSet()
        }
}