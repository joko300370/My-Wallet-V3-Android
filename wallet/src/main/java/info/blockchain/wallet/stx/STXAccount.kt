package info.blockchain.wallet.stx

import info.blockchain.wallet.bip44.HDAddress
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation

class STXAccount(params: NetworkParameters, wKey: DeterministicKey) {

    private val coinDerivationKey =
        HDKeyDerivation.deriveChildKey(wKey, 5757 or ChildNumber.HARDENED_BIT)
    private val accountDerivationKey =
        HDKeyDerivation.deriveChildKey(coinDerivationKey, 0 or ChildNumber.HARDENED_BIT)
    private val addressDerivationKey =
        HDKeyDerivation.deriveChildKey(accountDerivationKey, 0)

    val address = HDAddress(params, addressDerivationKey, 0, 44)

    val bitcoinSerializedBase58Address: String
        get() = address.formattedAddress
}