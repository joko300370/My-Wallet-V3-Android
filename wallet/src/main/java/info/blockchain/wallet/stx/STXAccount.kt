package info.blockchain.wallet.stx

import info.blockchain.wallet.bip44.HDAddress
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation

class STXAccount(params: NetworkParameters, wKey: DeterministicKey) {


    private val aKey = HDKeyDerivation.deriveChildKey(wKey, 5757 or ChildNumber.HARDENED_BIT)
    private val bKey = HDKeyDerivation.deriveChildKey(aKey, 0 or ChildNumber.HARDENED_BIT)
    private val cKey = HDKeyDerivation.deriveChildKey(bKey, 0)
    val address = HDAddress(params, cKey, 0)

    val bitcoinSerializedBase58Address
        get() = address.addressBase58
}