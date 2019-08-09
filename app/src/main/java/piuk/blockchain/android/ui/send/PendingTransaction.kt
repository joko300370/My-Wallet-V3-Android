package piuk.blockchain.android.ui.send

import com.fasterxml.jackson.annotation.JsonIgnore

import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.payment.SpendableUnspentOutputs

import java.math.BigInteger

import piuk.blockchain.android.ui.account.ItemAccount
import info.blockchain.balance.CryptoCurrency

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

    var bitpayMerchant: String? = null

    val total: BigInteger
        @JsonIgnore
        get() = bigIntAmount.add(bigIntFee)

    val isWatchOnly: Boolean
        @JsonIgnore
        get() {
            var watchOnly = false

            if (sendingObject!!.accountObject is LegacyAddress) {
                val legacyAddress = senderAsLegacyAddress
                watchOnly = legacyAddress.isWatchOnly && legacyAddress.privateKey.isNullOrEmpty()
            }

            return watchOnly
        }

    val displayableReceivingLabel: String?
        @JsonIgnore
        get() = if (receivingObject != null && !receivingObject!!.label.isNullOrEmpty()) {
            receivingObject!!.label
        } else if (bitpayMerchant != null) {
            bitpayMerchant
        } else {
            receivingAddress
        }

    @JsonIgnore
    fun isHD(currency: CryptoCurrency): Boolean {
        return if (currency === CryptoCurrency.BTC) {
            sendingObject!!.accountObject is Account
        } else {
            sendingObject!!.accountObject is GenericMetadataAccount
        }
    }

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

    companion object {
        const val WATCH_ONLY_SPEND_TAG = -5
    }
}
