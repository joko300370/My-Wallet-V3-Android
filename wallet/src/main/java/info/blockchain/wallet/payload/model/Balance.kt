package info.blockchain.wallet.payload.model

import com.blockchain.api.bitcoin.data.BalanceDto
import com.blockchain.api.bitcoin.data.BalanceResponseDto
import java.math.BigInteger

data class Balance(
    var finalBalance: BigInteger,
    var txCount: Long = 0,
    var totalReceived: BigInteger
)

fun BalanceDto.toBalance() =
    Balance(
        finalBalance = BigInteger(finalBalance),
        txCount = txCount,
        totalReceived = BigInteger(totalReceived)
    )

fun BalanceResponseDto.toBalanceMap() =
    map { (k, v) -> k to v.toBalance() }.toMap()