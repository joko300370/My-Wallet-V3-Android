package piuk.blockchain.android.ui.recover

import info.blockchain.wallet.metadata.Metadata
import info.blockchain.wallet.metadata.MetadataDerivation
import info.blockchain.wallet.metadata.MetadataInteractor
import io.reactivex.Completable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import piuk.blockchain.androidcore.data.auth.metadata.WalletRecoveryMetadata
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs

class AccountRecoveryInteractor(
    private val payloadDataManager: PayloadDataManager,
    private val prefs: PersistentPrefs,
    private val metadataInteractor: MetadataInteractor,
    private val metadataDerivation: MetadataDerivation
) {

    fun recoverCredentials(seedPhrase: String): Completable {

        val masterKey = payloadDataManager.generateMasterKeyFromSeed(seedPhrase)
        val metadataNode = metadataDerivation.deriveMetadataNode(masterKey)

        return metadataInteractor.loadRemoteMetadata(
            Metadata.newInstance(
                metaDataHDNode = metadataDerivation.deserializeMetadataNode(metadataNode),
                type = WalletRecoveryMetadata.WALLET_CREDENTIALS_METADATA_NODE,
                metadataDerivation = metadataDerivation
            )
        )
            .flatMapCompletable { json ->
                val credentials = Json.decodeFromString<WalletRecoveryMetadata>(json)
                payloadDataManager.initializeAndDecrypt(
                    credentials.sharedKey,
                    credentials.guid,
                    credentials.password
                )
            }
    }

    fun restoreWallet() = Completable.fromCallable {
        payloadDataManager.wallet?.let { wallet ->
            prefs.sharedKey = wallet.sharedKey
            prefs.walletGuid = wallet.guid
            prefs.isOnboardingComplete = true
        }
    }
}