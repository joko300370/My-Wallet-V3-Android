package piuk.blockchain.android.coincore.alg

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

class AlgoCustodialTradingAccount(
    cryptoCurrency: CryptoCurrency,
    label: String,
    exchangeRates: ExchangeRateDataManager,
    custodialWalletManager: CustodialWalletManager
) : CustodialTradingAccount(cryptoCurrency, label, exchangeRates, custodialWalletManager) {
    override val actions: AvailableActions
        get() = availableActions

    private val availableActions = setOf(
        AssetAction.ViewActivity
    )
}