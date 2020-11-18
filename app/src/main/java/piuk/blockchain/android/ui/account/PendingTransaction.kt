package piuk.blockchain.android.ui.account

import com.fasterxml.jackson.annotation.JsonIgnore
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import java.math.BigInteger

@Deprecated("Obsolete with new send")
class PendingTransaction {
    var unspentOutputBundle: SpendableUnspentOutputs? = null
    var sendingObject: ItemAccount? = null
    var receivingObject: ItemAccount? = null

    var note: String = ""
    var receivingAddress: String = ""
    var changeAddress: String = ""

    var bigIntFee: BigInteger = BigInteger.ZERO
    var bigIntAmount: BigInteger = BigInteger.ZERO

    var addressToReceiveIndex: Int = 0
    var warningText: String = ""
    var warningSubText: String = ""

    val total: BigInteger
        @JsonIgnore
        get() = bigIntAmount.add(bigIntFee)

    val senderAsLegacyAddress
        @JsonIgnore
        get() = sendingObject?.accountObject as LegacyAddress

    @JsonIgnore
    fun clear() {
        unspentOutputBundle = null
        sendingObject = null
        receivingAddress = ""
        note = ""
        receivingAddress = ""
        bigIntFee = BigInteger.ZERO
        bigIntAmount = BigInteger.ZERO
        warningText = ""
        warningSubText = ""
    }

    override fun toString(): String {
        return "PendingTransaction {" +
            "unspentOutputBundle=$unspentOutputBundle" +
            ", sendingObject=$sendingObject" +
            ", receivingObject=$receivingObject" +
            ", note='$note'" +
            ", receivingAddress='$receivingAddress'" +
            ", changeAddress='$changeAddress'" +
            ", bigIntFee=$bigIntFee" +
            ", bigIntAmount=$bigIntAmount" +
            ", addressToReceiveIndex=$addressToReceiveIndex" +
            ", warningText=$warningText" +
            ", warningSubText=$warningSubText" +
            "}"
    }
}
