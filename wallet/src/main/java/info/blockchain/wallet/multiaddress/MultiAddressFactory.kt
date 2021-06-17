package info.blockchain.wallet.multiaddress

import com.blockchain.api.ApiException
import com.blockchain.api.NonCustodialBitcoinService
import com.blockchain.api.bitcoin.data.MultiAddress
import com.blockchain.api.bitcoin.data.Transaction
import info.blockchain.wallet.bip44.HDChain
import info.blockchain.wallet.payload.data.AddressLabel
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.data.allAddresses
import info.blockchain.wallet.payload.data.legacyXpubAddresses
import info.blockchain.wallet.payload.data.segwitXpubAddresses
import retrofit2.Call
import java.math.BigInteger
import java.util.Collections
import kotlin.collections.Map.Entry

open class MultiAddressFactory(
    internal val bitcoinApi: NonCustodialBitcoinService
) {
    private val nextReceiveAddressMap: HashMap<String, Int> = HashMap()
    private val nextChangeAddressMap: HashMap<String, Int> = HashMap()

    // Field for testing if address belongs to us - Quicker than derivation
    private val addressToXpubMap: HashMap<String, String> = HashMap()

    fun getXpubFromAddress(address: String): String? {
        return addressToXpubMap[address]
    }

    private fun getMultiAddress(
        xpubs: List<XPubs>,
        onlyShow: List<String>?,
        limit: Int,
        offset: Int
    ): MultiAddress? {

        val call = getMultiAddress(xpubs, limit, offset, onlyShow)
        val response = call.execute()

        return if (response.isSuccessful) {
            response.body()
        } else {
            throw ApiException(response.errorBody()!!.string())
        }
    }

    protected open fun getMultiAddress(
        xpubs: List<XPubs>,
        limit: Int,
        offset: Int,
        context: List<String>?
    ): Call<MultiAddress> {
        return bitcoinApi
            .getMultiAddress(
                NonCustodialBitcoinService.BITCOIN,
                xpubs.legacyXpubAddresses(),
                xpubs.segwitXpubAddresses(),
                context?.joinToString("|"),
                NonCustodialBitcoinService.BalanceFilter.RemoveUnspendable,
                limit,
                offset
            )
    }

    /**
     * @param all A list of all xpubs and legacy addresses whose transactions are to
     * be retrieved from API.
     * @param activeImported (Hacky! Needs a rethink) Only set this when fetching a transaction list
     * for imported addresses, otherwise set as Null.
     * A list of all active legacy addresses. Used for 'Imported address' transaction list.
     * @param onlyShow Xpub or legacy address. Used to fetch transaction only relating to this
     * address. Set as Null for a consolidated list like 'All Accounts' or 'Imported'.
     * @param limit Maximum amount of transactions fetched
     * @param offset Page offset
     */

    fun getAccountTransactions(
        all: List<XPubs>,
        activeImported: List<String>?,
        onlyShow: List<String>?,
        limit: Int,
        offset: Int,
        startingBlockHeight: Int
    ): List<TransactionSummary> {

        val multiAddress = getMultiAddress(all, onlyShow, limit, offset)
        return if (multiAddress?.txs == null) {
            emptyList()
        } else {
            summarize(
                all,
                multiAddress,
                activeImported,
                startingBlockHeight
            )
        }
    }

    fun getNextChangeAddressIndex(xpub: String): Int =
        if (nextChangeAddressMap.containsKey(xpub)) {
            nextChangeAddressMap[xpub]!!
        } else {
            0
        }

    fun getNextReceiveAddressIndex(xpub: String, reservedAddresses: List<AddressLabel>): Int {
        if (!nextReceiveAddressMap.containsKey(xpub)) {
            return 0
        }

        var receiveIndex: Int? = nextReceiveAddressMap[xpub]
        // Skip reserved addresses
        for ((index) in reservedAddresses) {
            if (index == receiveIndex) {
                receiveIndex++
            }
        }

        return receiveIndex!!
    }

    fun sort(txs: ArrayList<Transaction>?) {
        if (txs == null) {
            return
        }
        Collections.sort(txs, TxMostRecentDateComparator())
    }

    fun isOwnHDAddress(address: String): Boolean {
        return addressToXpubMap.containsKey(address)
    }

    @Deprecated("Use the XPub version")
    fun incrementNextReceiveAddress(xpub: XPub, reservedAddresses: List<AddressLabel>) {
        var receiveIndex = getNextReceiveAddressIndex(xpub.address, reservedAddresses)
        receiveIndex++

        nextReceiveAddressMap[xpub.address] = receiveIndex
    }

    @Deprecated("Use the XPub version")
    fun incrementNextReceiveAddress(xpub: String, reservedAddresses: List<AddressLabel>) {
        val receiveIndex = getNextReceiveAddressIndex(xpub, reservedAddresses) + 1
        nextReceiveAddressMap[xpub] = receiveIndex
    }

    fun incrementNextChangeAddress(xpub: String) {
        val index = getNextChangeAddressIndex(xpub) + 1
        nextChangeAddressMap[xpub] = index
    }

    inner class TxMostRecentDateComparator : Comparator<Transaction> {
        override fun compare(t1: Transaction, t2: Transaction): Int =
            t2.time.compareTo(t1.time)
    }

    private fun summarize(
        xpubs: List<XPubs>,
        multiAddress: MultiAddress,
        imported: List<String>?,
        startingBlockHeight: Int
    ): List<TransactionSummary> {
        val ownAddresses = xpubs.allAddresses().toMutableList()
        val summaryList = ArrayList<TransactionSummary>()

        // Set next address indexes
        for (address in multiAddress.addresses) {
            nextReceiveAddressMap[address.address] = address.accountIndex
            nextChangeAddressMap[address.address] = address.changeIndex
        }

        val txs = multiAddress.txs ?: return summaryList // Address might not contain transactions

        for (tx in txs) {
            val blockHeight = tx.blockHeight
            if (blockHeight != null && blockHeight != 0L && blockHeight < startingBlockHeight) {
                // Filter out txs before blockHeight (mainly for BCH)
                // Block height will be 0 until included in a block
                continue
            }

            var isImported = false

            val txSummary = TransactionSummary()
            txSummary.inputsMap = HashMap()
            txSummary.outputsMap = HashMap()

            // Map which address belongs to which xpub.
            txSummary.inputsXpubMap = HashMap()
            txSummary.outputsXpubMap = HashMap()

            when {
                tx.result.add(tx.fee).signum() == 0 -> {
                    txSummary.transactionType = TransactionSummary.TransactionType.TRANSFERRED
                }
                tx.result.signum() > 0 -> {
                    txSummary.transactionType = TransactionSummary.TransactionType.RECEIVED
                }
                else -> {
                    txSummary.transactionType = TransactionSummary.TransactionType.SENT
                }
            }

            for (input in tx.inputs) {
                val prevOut = input.prevOut
                if (prevOut != null) {

                    val inputAddr = prevOut.addr
                    val inputValue = prevOut.value
                    if (inputAddr != null) {

                        // Transaction from HD account
                        prevOut.xpub?.let {
                            // xpubBody will only show if it belongs to our account
                            // inputAddr belongs to our own account - add it, it's a transfer/send
                            ownAddresses.add(inputAddr)
                            txSummary.inputsXpubMap[inputAddr] = it.address
                        }

                        // Transaction from HD account
                        val xpubBody = prevOut.xpub
                        if (xpubBody != null) {
                            // xpubBody will only show if it belongs to our account
                            // inputAddr belongs to our own account - add it, it's a transfer/send
                            ownAddresses.add(inputAddr)
                            txSummary.inputsXpubMap[inputAddr] = xpubBody.address
                        }

                        // Flag as imported legacy address
                        isImported = (imported?.contains(inputAddr) == true)

                        // Keep track of inputs
                        val existingBalance: BigInteger = txSummary.inputsMap[inputAddr] ?: BigInteger.ZERO

                        txSummary.inputsMap[inputAddr] = existingBalance.add(inputValue)
                    } else {
                        // No input address available
                        txSummary.inputsMap[ADDRESS_DECODE_ERROR] = inputValue
                    }
                } else {
                    // Newly generated coin
                }
            }

            val changeMap = HashMap<String, BigInteger>()
            var outputAddr: String?
            var outputValue: BigInteger
            for (output in tx.out) {

                outputAddr = output.addr
                outputValue = output.value
                if (outputAddr != null) {
                    val xpubBody = output.xpub
                    if (xpubBody != null) {
                        // inputAddr belongs to our own account - add it
                        ownAddresses.add(outputAddr)
                        if (xpubBody.derivationPath.startsWith("M/" + HDChain.RECEIVE_CHAIN + "/")) {
                            val existingBalance = txSummary.outputsMap[outputAddr] ?: BigInteger.ZERO
                            txSummary.outputsMap[outputAddr] = existingBalance.add(outputValue)
                            txSummary.outputsXpubMap[outputAddr] = xpubBody.address
                        } else {
                            // Change
                            changeMap[outputAddr] = outputValue
                        }
                    } else {
                        // If we own this address and it's not change coming back, it's a transfer
                        if (ownAddresses.contains(outputAddr) &&
                            !txSummary.inputsMap.keys.contains(outputAddr)
                        ) {

                            if (txSummary.transactionType == TransactionSummary.TransactionType.SENT) {
                                txSummary.transactionType = TransactionSummary.TransactionType.TRANSFERRED
                            }

                            // Don't add change coming back
                            if (!txSummary.inputsMap.containsKey(outputAddr)) {
                                val existingBalance =
                                    txSummary.outputsMap[outputAddr] ?: BigInteger.ZERO

                                txSummary.outputsMap[outputAddr] = existingBalance.add(outputValue)
                            } else {
                                changeMap[outputAddr] = outputValue
                            }
                        } else if (txSummary.inputsMap.keys.contains(outputAddr)) {
                            // Our change
                            changeMap[outputAddr] = outputValue
                        } else {
                            // Address does not belong to us
                            val existingBalance =
                                txSummary.outputsMap[outputAddr] ?: BigInteger.ZERO
                            txSummary.outputsMap[outputAddr] = existingBalance.add(outputValue)
                        }
                    }

                    // Flag as imported legacy address
                    if (imported != null && imported.contains(outputAddr)) {
                        isImported = true
                    }
                } else {
                    // No output address available
                    txSummary.outputsMap[ADDRESS_DECODE_ERROR] = outputValue
                }
            }

            // If we are filtering for just legacy tx and this is not legacy, abort
            if (imported != null && !isImported) {
                continue
            }

            // Remove input addresses not ours
            filterOwnedAddresses(
                ownAddresses,
                txSummary.inputsMap,
                txSummary.outputsMap,
                txSummary.transactionType
            )

            txSummary.hash = tx.hash
            txSummary.time = tx.time
            txSummary.isDoubleSpend = tx.isDoubleSpend
            txSummary.fee = tx.fee

            if (txSummary.transactionType == TransactionSummary.TransactionType.RECEIVED) {
                val total = calculateTotalReceived(txSummary.outputsMap)
                txSummary.total = total
            } else {
                val total = calculateTotalSent(
                    inputsMap = txSummary.inputsMap,
                    changeMap = changeMap,
                    fee = tx.fee ?: BigInteger.ZERO,
                    direction = txSummary.transactionType
                )
                txSummary.total = total
            }

            // Set confirmations
            val latestBlock = multiAddress.info.latestBlock.height
            val txBlockHeight = tx.blockHeight
            txBlockHeight?.let {
                if (latestBlock > 0 && it > 0) {
                    txSummary.confirmations = (latestBlock - it + 1).toInt()
                } else {
                    txSummary.confirmations = 0
                }
            } ?: kotlin.run { txSummary.confirmations = 0 }

            addressToXpubMap.putAll(txSummary.getInputsXpubMap())
            addressToXpubMap.putAll(txSummary.getOutputsXpubMap())

            summaryList.add(txSummary)
        }

        return summaryList
    }

    private fun filterOwnedAddresses(
        ownAddresses: List<String>,
        inputsMap: HashMap<String, BigInteger>,
        outputsMap: HashMap<String, BigInteger>,
        transactionType: TransactionSummary.TransactionType
    ) {

        var iterator: MutableIterator<Entry<String, BigInteger>> = inputsMap.entries.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (!ownAddresses.contains(item.key) && transactionType == TransactionSummary.TransactionType.SENT) {
                iterator.remove()
            }
        }

        iterator = outputsMap.entries.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (!ownAddresses.contains(item.key) && transactionType == TransactionSummary.TransactionType.RECEIVED) {
                iterator.remove()
            }
        }
    }

    private fun calculateTotalReceived(outputsMap: HashMap<String, BigInteger>): BigInteger {
        var total = BigInteger.ZERO

        for (output in outputsMap.values) {
            total = total.add(output)
        }

        return total
    }

    private fun calculateTotalSent(
        inputsMap: HashMap<String, BigInteger>,
        changeMap: HashMap<String, BigInteger>,
        fee: BigInteger,
        direction: TransactionSummary.TransactionType
    ): BigInteger {

        var total = BigInteger.ZERO

        for (input in inputsMap.values) {
            total = total.add(input)
        }

        for (change in changeMap.values) {
            total = total.subtract(change)
        }

        if (direction == TransactionSummary.TransactionType.TRANSFERRED) {
            total = total.subtract(fee)
        }

        return total
    }

    companion object {
        const val ADDRESS_DECODE_ERROR = "[--address_decode_error--]"
    }
}