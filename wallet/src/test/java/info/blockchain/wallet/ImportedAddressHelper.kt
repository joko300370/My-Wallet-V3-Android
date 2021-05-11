package info.blockchain.wallet

import info.blockchain.wallet.payload.data.ImportedAddress
import org.bitcoinj.core.Base58
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.params.MainNetParams

object ImportedAddressHelper {

    /**
     * Creates authentic ImportedAddress
     */
    @JvmStatic
    @JvmOverloads
    fun getImportedAddress(privateKeyBase58Wif: String = "KwfQ7kP96qiXW7TXekvnjUi7QaRz7Wk4Y9QKMnrK2QDCEF9Gdn6F") =
        privateKeyWifToEcKey(privateKeyBase58Wif).let {
            val address = LegacyAddress.fromKey(MainNetParams.get(), it).toString()
            ImportedAddress().apply {
                setPrivateKeyFromBytes(it.privKeyBytes)
                this.address = address
                createdDeviceName = ""
                createdTime = System.currentTimeMillis()
                createdDeviceVersion = ""
                label = "Some Label"
            }
        }

    private fun privateKeyWifToEcKey(privateKeyBase58Wif: String) = ECKey.fromPrivate(
        Base58.decode(privateKeyBase58Wif)
            .drop(1).take(32).toByteArray()
    )
}
