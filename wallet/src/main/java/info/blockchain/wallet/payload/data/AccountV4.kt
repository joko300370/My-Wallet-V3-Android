package info.blockchain.wallet.payload.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import info.blockchain.wallet.bip44.HDAccount
import java.lang.IllegalStateException

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY,
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE,
    creatorVisibility = Visibility.NONE,
    isGetterVisibility = Visibility.NONE)
data class AccountV4(
    @JsonProperty("label")
    override var label: String = "",

    @JsonProperty("default_derivation")
    var defaultType: String = Derivation.SEGWIT_BECH32_TYPE,

    @JsonProperty("archived")
    override var isArchived: Boolean = false,

    @JsonProperty("derivations")
    val derivations: MutableList<Derivation> = mutableListOf()
) : Account {

    override fun xpubForDerivation(derivation: String): String? =
        derivationForType(derivation)?.xpub

    override fun containsXpub(xpub: String): Boolean =
        derivations.map { it.xpub }.contains(xpub)

    fun derivationForType(type: String) = derivations.find { it.type == type }

    private val derivation
        get() = derivationForType(defaultType)

    override var xpriv: String
        get() = derivation?.xpriv ?: ""
        set(value) {
            derivation?.xpriv = value
        }

    @delegate:Transient
    override val xpubs: XPubs by lazy {
        XPubs(derivations.map { XPub(address = it.xpub, derivation = mapFormat(it.type)) })
    }

    override val addressCache: AddressCache
        get() = derivation?.cache ?: AddressCache()

    override val addressLabels: MutableList<AddressLabel>
        get() = derivation?.addressLabels ?: mutableListOf()

    override fun addAddressLabel(index: Int, reserveLabel: String) {
        val addressLabel = AddressLabel().apply {
            this.index = index
            this.label = reserveLabel
        }
        if (derivation?.addressLabels?.contains(addressLabel) == true) {
            derivation?.addressLabels?.add(addressLabel)
        }
    }

    override fun toJson(mapper: ObjectMapper): String = mapper.writeValueAsString(this)

    override fun upgradeToV4() = this

    @Deprecated("We should pass the info into the Accountv3.upgrade method and keep this immutable if possible")
    fun addSegwitDerivation(hdAccount: HDAccount, index: Int) {
        if (defaultType == Derivation.SEGWIT_BECH32_TYPE) {
            return
        }
        derivations += Derivation.createSegwit(
            hdAccount.xPriv,
            hdAccount.xpub,
            AddressCache.forAccountWithIndex(hdAccount, index, Derivation.SEGWIT_BECH32_PURPOSE))
        defaultType = Derivation.SEGWIT_BECH32_TYPE
    }
}

private fun mapFormat(type: String): XPub.Format =
    when (type) {
        Derivation.LEGACY_TYPE -> XPub.Format.LEGACY
        Derivation.SEGWIT_BECH32_TYPE -> XPub.Format.SEGWIT
        else -> throw IllegalStateException("Unknown derivation type")
    }