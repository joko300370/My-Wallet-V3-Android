package info.blockchain.wallet.payload.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import info.blockchain.wallet.bip44.HDAccount
import java.io.IOException

/*
This class is used for iOS and Web only.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY,
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE,
    creatorVisibility = Visibility.NONE,
    isGetterVisibility = Visibility.NONE)
data class AddressCache(
    @JsonProperty("receiveAccount")
    var receiveAccount: String? = null,
    @JsonProperty("changeAccount")
    var changeAccount: String? = null
) {
    @Throws(JsonProcessingException::class)
    fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
    }

    companion object {

        @Throws(IOException::class)
        fun fromJson(json: String): AddressCache {
            return ObjectMapper().readValue(json, AddressCache::class.java)
        }

        fun setCachedXPubs(account: HDAccount): AddressCache {
            return AddressCache(
                receiveAccount = account.receive.xpub,
                changeAccount = account.change.xpub
            )
        }
    }
}
