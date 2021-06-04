package info.blockchain.wallet.payload.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY,
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE,
    creatorVisibility = Visibility.NONE,
    isGetterVisibility = Visibility.NONE)
data class Derivation(
    @JsonProperty("type")
    val type: String = "",

    @JsonProperty("purpose")
    val purpose: Int = 0,

    @JsonProperty("xpriv")
    var xpriv: String = "",

    @JsonProperty("xpub")
    var xpub: String = "",

    @JsonProperty("cache")
    var cache: AddressCache = AddressCache(),

    @JsonProperty("address_labels")
    var addressLabels: MutableList<AddressLabel> = mutableListOf()
) {
    constructor(type: String, purpose: Int) : this(type, purpose, "", "")

    companion object {
        const val LEGACY_TYPE = "legacy"
        const val LEGACY_PURPOSE = 44

        const val SEGWIT_BECH32_TYPE = "bech32"
        const val SEGWIT_BECH32_PURPOSE = 84

        @JvmStatic
        fun create(xpriv: String, xpub: String, cache: AddressCache): Derivation {
            return Derivation(LEGACY_TYPE, LEGACY_PURPOSE, xpriv, xpub, cache)
        }

        @JvmStatic
        fun createSegwit(xpriv: String, xpub: String, cache: AddressCache): Derivation {
            return Derivation(SEGWIT_BECH32_TYPE, SEGWIT_BECH32_PURPOSE, xpriv, xpub, cache)
        }
    }
}
