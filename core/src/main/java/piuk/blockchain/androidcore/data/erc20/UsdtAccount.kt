package piuk.blockchain.androidcore.data.erc20

import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.erc20.datastores.Erc20DataStore
import piuk.blockchain.androidcore.data.ethereum.EthDataManager

class UsdtAccount(
    override val ethDataManager: EthDataManager,
    override val dataStore: Erc20DataStore,
    override val cryptoCurrency: CryptoCurrency = CryptoCurrency.USDT,
    environmentSettings: EnvironmentConfig
) : Erc20BaseAccount(environmentSettings)