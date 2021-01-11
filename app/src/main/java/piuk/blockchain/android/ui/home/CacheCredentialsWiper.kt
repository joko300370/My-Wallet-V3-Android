package piuk.blockchain.android.ui.home

import com.blockchain.nabu.datamanagers.NabuDataManager
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsState

class CacheCredentialsWiper(

    private val ethDataManager: EthDataManager,
    private val bchDataManager: BchDataManager,
    private val metadataManager: MetadataManager,
    private val nabuDataManager: NabuDataManager,
    private val walletOptionsState: WalletOptionsState
) {
    fun wipe() {
        ethDataManager.clearEthAccountDetails()
        bchDataManager.clearBchAccountDetails()
        nabuDataManager.clearAccessToken()
        metadataManager.reset()
        walletOptionsState.wipe()
    }
}