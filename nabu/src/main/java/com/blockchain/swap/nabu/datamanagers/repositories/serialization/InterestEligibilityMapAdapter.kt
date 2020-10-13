package com.blockchain.swap.nabu.datamanagers.repositories.serialization

import com.blockchain.swap.nabu.models.interest.DisabledReason
import com.blockchain.swap.nabu.models.interest.InterestEligibilityFullResponse
import com.blockchain.swap.nabu.models.interest.InterestEligibilityResponse
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader

class InterestEligibilityMapAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): InterestEligibilityFullResponse {
        val map = mutableMapOf<String, InterestEligibilityResponse>()

        reader.beginObject()
        while (reader.hasNext()) {
            val k = reader.nextName()
            var eligible = false
            var reason: DisabledReason = DisabledReason.NONE
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.selectName(KEYS)) {
                    0 -> eligible = reader.nextBoolean()
                    1 -> {
                        reason = if (reader.peek() == JsonReader.Token.STRING) {
                            mapStringToReason(reader.nextString())
                        } else {
                            mapStringToReason(reader.nextNull<String>())
                        }
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            map[k] = InterestEligibilityResponse(eligible, reason)
        }
        reader.endObject()

        return InterestEligibilityFullResponse(map)
    }

    private fun mapStringToReason(reason: String?): DisabledReason =
        when (reason) {
            REGION -> DisabledReason.REGION
            KYC_TIER -> DisabledReason.KYC_TIER
            null -> DisabledReason.NONE
            else -> DisabledReason.OTHER
        }

    companion object {
        private val KEYS = JsonReader.Options.of("eligible", "ineligibleReason")
        private const val REGION = "UNSUPPORTED_REGION"
        private const val KYC_TIER = "TIER_TOO_LOW"
    }
}