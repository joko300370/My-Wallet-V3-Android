package info.blockchain.wallet.ethereum.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigInteger

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
class EthTransaction(

    @JsonProperty("blockNumber")
    val blockNumber: Long? = 0L,

    @JsonProperty("timestamp")
    val timestamp: Long = 0L,

    @JsonProperty("hash")
    val hash: String = "",

    @JsonProperty("nonce")
    val nonce: String = "",

    @JsonProperty("blockHash")
    val blockHash: String? = "",

    @JsonProperty("transactionIndex")
    val transactionIndex: Int = 0,

    @JsonProperty("from")
    val from: String = "",

    @JsonProperty("to")
    val to: String = "",

    @JsonProperty("value")
    val value: BigInteger = 0.toBigInteger(),

    @JsonProperty("gasPrice")
    val gasPrice: BigInteger = 0.toBigInteger(),

    @JsonProperty("gasUsed")
    val gasUsed: BigInteger = 0.toBigInteger(),

    @JsonProperty("state")
    val state: String = ""
)

enum class TransactionState {
    CONFIRMED,
    REPLACED,
    PENDING,
    UNKNOWN
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
class EthTransactionsResponse(
    @JsonProperty("transactions")
    val transactions: List<EthTransaction> = emptyList()
)
