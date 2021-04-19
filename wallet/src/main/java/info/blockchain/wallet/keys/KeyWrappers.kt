package info.blockchain.wallet.keys

import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.DeterministicKey

interface SigningKey {
    val privateKeyAsHex: String
    val hasPrivKey: Boolean
    fun toECKey(): ECKey
}

internal class SigningKeyImpl(private val key: ECKey) : SigningKey {
    override val privateKeyAsHex: String
        get() = key.privateKeyAsHex

    override val hasPrivKey: Boolean
        get() = key.hasPrivKey()

    override fun toECKey(): ECKey = key
}

interface MasterKey {
    fun toDeterministicKey(): DeterministicKey
}

internal class MasterKeyImpl(private val key: DeterministicKey) : MasterKey {
    override fun toDeterministicKey(): DeterministicKey = key
}
