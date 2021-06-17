package info.blockchain.wallet.util

import com.blockchain.api.bitcoin.data.BalanceResponseDto
import com.blockchain.api.bitcoin.data.MultiAddress
import com.blockchain.api.bitcoin.data.UnspentOutputDto
import com.blockchain.api.bitcoin.data.UnspentOutputsDto
import info.blockchain.wallet.payload.model.Utxo
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.math.BigInteger

fun parseUnspentOutputs(jsonString: String): UnspentOutputsDto {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    return json.decodeFromString(jsonString)
}

fun parseUnspentOutputList(jsonString: String): List<UnspentOutputDto> {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    return json.decodeFromString(jsonString)
}

fun parseUnspentOutputsAsUtxoList(jsonString: String): List<Utxo> =
    parseUnspentOutputs(jsonString = jsonString)
        .unspentOutputs
        .map { Utxo(
            value = BigInteger(it.value),
            script = it.script ?: "",
            txHash = it.txHash,
            txOutputCount = it.txOutputCount,
            isReplayable = it.replayable,
            xpub = it.xpub,
            isSegwit = false,
            isForceInclude = false
        ) }

fun parseMultiAddressResponse(jsonString: String): MultiAddress {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    return json.decodeFromString(jsonString)
}

fun parseBalanceResponseDto(jsonString: String): BalanceResponseDto {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    return json.decodeFromString(jsonString)
}