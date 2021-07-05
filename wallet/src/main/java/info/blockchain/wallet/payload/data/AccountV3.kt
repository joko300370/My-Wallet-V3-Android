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
data class AccountV3(
    @JsonProperty("label")
    override var label: String = "",

    @JsonProperty("archived")
    override var isArchived: Boolean = false,

    @JsonProperty("xpriv")
    override var xpriv: String = "",

    @JsonProperty("xpub")
    val legacyXpub: String = ""
) : Account {

    @delegate:Transient
    override val xpubs: XPubs by lazy {
        XPubs(XPub(address = legacyXpub, derivation = XPub.Format.LEGACY))
    }

    @JsonProperty("cache")
    override val addressCache: AddressCache = AddressCache()

    @JsonProperty("address_labels")
    override val addressLabels: MutableList<AddressLabel> = mutableListOf()

    constructor(xPub: String) : this(legacyXpub = xPub)

    override fun addAddressLabel(index: Int, reserveLabel: String) {
        val addressLabel = AddressLabel()
        addressLabel.label = reserveLabel
        addressLabel.index = index

        if (!addressLabels.contains(addressLabel)) {
            addressLabels.add(addressLabel)
        }
    }

    override fun upgradeToV4(): AccountV4 {
        val legacyDerivation = Derivation(
            Derivation.LEGACY_TYPE,
            Derivation.LEGACY_PURPOSE,
            xpriv,
            legacyXpub,
            addressCache,
            addressLabels
        )
        val derivations = mutableListOf(legacyDerivation)
        return AccountV4(label, legacyDerivation.type, isArchived, derivations)
    }

    override fun xpubForDerivation(derivation: String): String? =
        if (derivation == Derivation.LEGACY_TYPE) legacyXpub else null

    override fun containsXpub(xpub: String): Boolean {
        return legacyXpub == xpub
    }
}
