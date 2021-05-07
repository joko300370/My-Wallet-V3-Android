package info.blockchain.wallet.payload.data

import com.blockchain.serialization.JsonSerializableAccount
import com.fasterxml.jackson.databind.ObjectMapper

interface Account : JsonSerializableAccount {
    override var label: String

    var isArchived: Boolean

    var xpriv: String

    val xpubs: XPubs

    val addressCache: AddressCache

    val addressLabels: MutableList<AddressLabel>

    fun xpubForDerivation(derivation: String): String?

    fun containsXpub(xpub: String): Boolean

    fun addAddressLabel(index: Int, reserveLabel: String)

    fun upgradeToV4(): AccountV4

    fun toJson(mapper: ObjectMapper): String
}
