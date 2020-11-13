package piuk.blockchain.android.coincore.alg

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

class AlgoCustodialTradingAccount(
    cryptoCurrency: CryptoCurrency,
    label: String,
    exchangeRates: ExchangeRateDataManager,
    custodialWalletManager: CustodialWalletManager,
    environmentConfig: EnvironmentConfig
) : CustodialTradingAccount(
    asset = cryptoCurrency,
    label = label,
    exchangeRates = exchangeRates,
    custodialWalletManager = custodialWalletManager,
    environmentConfig = environmentConfig
) {
    override val actions: AvailableActions =
        setOf(AssetAction.ViewActivity, AssetAction.Sell, AssetAction.Swap)
}