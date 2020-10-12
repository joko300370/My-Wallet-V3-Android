package com.blockchain.swap.nabu.datamanagers.repositories.serialization

import com.blockchain.swap.nabu.models.interest.AssetLimitsResponse
import com.blockchain.swap.nabu.models.interest.InterestLimitsFullResponse
import com.blockchain.swap.nabu.models.interest.InterestLimitsResponse
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader

class InterestLimitsMapAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): InterestLimitsFullResponse {
        val map = mutableMapOf<String, InterestLimitsResponse>()

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.peek()) {
                JsonReader.Token.NAME ->
                    when (reader.nextName()) {
                        "limits" -> {
                            var k = ""
                            var currency = ""
                            var lockUpDuration: Int = -1
                            var maxWithdrawalAmount = ""
                            var minDepositAmount = ""

                            reader.beginObject()
                            while (reader.hasNext()) {
                                k = reader.nextName()
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    when (reader.nextName()) {
                                        "currency" -> currency = reader.nextString()
                                        "lockUpDuration" -> lockUpDuration = reader.nextInt()
                                        "maxWithdrawalAmount" -> maxWithdrawalAmount =
                                            reader.nextString()
                                        "minDepositAmount" -> minDepositAmount = reader.nextString()
                                    }
                                }
                                reader.endObject()
                                map[k] = InterestLimitsResponse(currency, lockUpDuration,
                                    maxWithdrawalAmount,
                                    minDepositAmount)
                            }
                            reader.endObject()
                        }
                        else -> reader.skipValue()
                    }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        val limitsObject = AssetLimitsResponse(map)

        return InterestLimitsFullResponse(limitsObject)
    }
}