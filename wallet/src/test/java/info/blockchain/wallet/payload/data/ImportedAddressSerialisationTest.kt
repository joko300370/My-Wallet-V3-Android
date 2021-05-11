package info.blockchain.wallet.payload.data

import com.blockchain.serialization.fromMoshiJson
import com.blockchain.serialization.toMoshiJson
import com.blockchain.testutils.getStringFromResource
import com.fasterxml.jackson.databind.ObjectMapper
import org.amshove.kluent.`should equal to`
import org.junit.Test

class ImportedAddressSerialisationTest {

    @Test
    fun `jackson to moshi`() {
        val objectMapper = ObjectMapper()
        val json = getStringFromResource("serialisation/ImportedAddress.json")

        val object1 = objectMapper.readValue(json, ImportedAddress::class.java)
        val jsonA = object1.toMoshiJson()
        val object2 = objectMapper.readValue(jsonA, ImportedAddress::class.java)
        val jsonB = object2.toMoshiJson()

        jsonA `should equal to` jsonB
    }

    @Test
    fun `moshi to jackson`() {
        val objectMapper = ObjectMapper()
        val json = getStringFromResource("serialisation/ImportedAddress.json")

        val object1 = ImportedAddress::class.fromMoshiJson(json)
        val jsonA = objectMapper.writeValueAsString(object1)
        val object2 = ImportedAddress::class.fromMoshiJson(jsonA)
        val jsonB = objectMapper.writeValueAsString(object2)

        jsonA `should equal to` jsonB
    }
}