package piuk.blockchain.androidcore.data.payments

import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.dust.DustService
import info.blockchain.wallet.api.dust.data.DustInput
import info.blockchain.wallet.exceptions.TransactionHashApiException
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.model.Utxo
import info.blockchain.wallet.payment.OutputType
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import io.reactivex.Observable
import io.reactivex.Single
import piuk.blockchain.androidcore.utils.annotations.WebRequest
import java.math.BigInteger
import java.util.HashMap
import org.bitcoinj.core.Transaction

class PaymentService(
    private val payment: Payment,
    private val dustService: DustService
) {

    /**
     * Submits a BTC payment to a specified Bitcoin address and returns the transaction hash if
     * successful
     *
     * @param signedTx signed object
     * @return An [Observable] wrapping a [String] where the String is the transaction hash
     */
    @WebRequest
    internal fun submitBtcPayment(
        signedTx: Transaction
    ): Single<String> = Single.fromCallable {
        val response = payment.publishBtcSimpleTransaction(signedTx).execute()
        when {
            response.isSuccessful -> signedTx.txId.toString()
            else -> throw TransactionHashApiException.fromResponse(signedTx.txId.toString(), response)
        }
    }

    /**
     * Sign a BTC transaction returns a signed transaction
     *
     * @param tx unsigned object
     * @param keys A List of elliptic curve keys
     * @return An [Transaction]
     */
    internal fun signBtcTx(
        tx: Transaction,
        keys: List<SigningKey>
    ): Transaction {
        payment.signBtcTransaction(
            tx,
            keys
        )
        return tx
    }

    /**
     * Get a BTC Transaction to a specified Bitcoin address and returns the transaction.
     *
     * @param unspentOutputBundle UTXO object
     * @param toAddress The Bitcoin Cash address to send the funds to
     * @param changeAddress A change address
     * @param bigIntFee The specified fee amount
     * @param bigIntAmount The actual transaction amount
     * @return A [Transaction]
     */
    internal fun getBtcTx(
        unspentOutputBundle: SpendableUnspentOutputs,
        toAddress: String,
        changeAddress: String,
        bigIntFee: BigInteger,
        bigIntAmount: BigInteger
    ): Transaction {
        val receivers = HashMap<String, BigInteger>()
        receivers[toAddress] = bigIntAmount

        return payment.makeBtcSimpleTransaction(
            unspentOutputBundle.spendableOutputs,
            receivers,
            bigIntFee,
            changeAddress
        )
    }

    /**
     * Submits a BCH payment to a specified Bitcoin Cash address and returns the transaction hash if
     * successful
     *
     * @param signedTx signed object
     * @return An [Observable] wrapping a [String] where the String is the transaction hash
     */
    @WebRequest
    internal fun submitBchPayment(
        signedTx: Transaction,
        dustInput: DustInput
    ): Single<String> =
        Single.fromCallable {
            val response = payment.publishBchTransaction(signedTx, dustInput.lockSecret).execute()
            when {
                response.isSuccessful -> signedTx.txId.toString()
                else -> throw TransactionHashApiException.fromResponse(signedTx.txId.toString(), response)
            }
        }

    /**
     * Sign a BCH transaction returns a signed transaction
     *
     * @param tx unsigned object
     * @param keys A List of elliptic curve keys
     * @return A [Transaction] signed
     */
    internal fun signBchTx(
        tx: Transaction,
        keys: List<SigningKey>
    ): Transaction {
        payment.signBchTransaction(
            tx,
            keys
        )
        return tx
    }

    /**
     * Get a BCH Transaction to a specified Bitcoin Cash address and returns the transaction.
     *
     * @param unspentOutputBundle UTXO object
     * @param toAddress The Bitcoin Cash address to send the funds to
     * @param changeAddress A change address
     * @param bigIntFee The specified fee amount
     * @param bigIntAmount The actual transaction amount
     * @return An [Observable] wrapping the [Transaction]
     */
    internal fun getBchTx(
        unspentOutputBundle: SpendableUnspentOutputs,
        toAddress: String,
        changeAddress: String,
        bigIntFee: BigInteger,
        bigIntAmount: BigInteger
    ): Observable<Pair<Transaction, DustInput?>> = dustService.getDust(CryptoCurrency.BCH)
        .flatMapObservable {
            val receivers = HashMap<String, BigInteger>()
            receivers[toAddress] = bigIntAmount

            val tx = payment.makeBchNonReplayableTransaction(
                unspentOutputBundle.spendableOutputs,
                receivers,
                bigIntFee,
                changeAddress,
                it
            )
            Observable.fromCallable { tx to it }
        }

    /**
     * Returns an [Utxo] object containing all the unspent outputs for a given
     * Bitcoin address.
     *
     * @param xpubs The BTC address you wish to query, as a String
     * @return An [Observable] wrapping a list of [Utxo] objects
     */
    @WebRequest
    internal fun getUnspentBtcOutputs(xpubs: XPubs): Single<List<Utxo>> =
        payment.getUnspentBtcCoins(xpubs)

    /**
     * Returns an [Utxo] object containing all the unspent outputs for a given
     * Bitcoin Cash address. Please note that this method only accepts a valid Base58 (ie Legacy)
     * BCH address. BECH32 is not accepted by the endpoint.
     *
     * @param address The BCH address you wish to query, as a Base58 address String
     * @return An [Observable] wrapping an [Utxo] object
     */
    @WebRequest
    internal fun getUnspentBchOutputs(address: String): Single<List<Utxo>> =
        payment.getUnspentBchCoins(listOf(address))

    /**
     * Returns a [SpendableUnspentOutputs] object from a given [Utxo] object,
     * given the payment amount and the current fee per kB. This method selects the minimum number
     * of inputs necessary to allow a successful payment by selecting from the largest inputs
     * first.
     *
     * @param unspentCoins The addresses' [Utxo]
     * @param targetOutputType Destination output type
     * @param changeOutputType Change output type
     * @param paymentAmount The amount you wish to send, as a [BigInteger]
     * @param feePerKb The current fee per kB, as a [BigInteger]
     * @param includeReplayProtection Whether or not you intend on adding a dust input for replay protection. This is
     * an extra input and therefore affects the transaction fee.
     * @return An [SpendableUnspentOutputs] object, which wraps a list of spendable outputs
     * for the given inputs
     */
    internal fun getSpendableCoins(
        unspentCoins: List<Utxo>,
        targetOutputType: OutputType,
        changeOutputType: OutputType,
        paymentAmount: BigInteger,
        feePerKb: BigInteger,
        includeReplayProtection: Boolean
    ): SpendableUnspentOutputs =
        payment.getSpendableCoins(
            unspentCoins,
            targetOutputType,
            changeOutputType,
            paymentAmount,
            feePerKb,
            includeReplayProtection
        )

    /**
     * Calculates the total amount of bitcoin that can be swept from an [Utxo]
     * object and returns the amount that can be recovered, along with the fee (in absolute terms)
     * necessary to sweep those coins.
     *
     * @param unspentCoins An [Utxo] object that you wish to sweep
     * @param targetOutputType Destination output type
     * @param feePerKb The current fee per kB on the network
     * @param includeReplayProtection Whether or not you intend on adding a dust input for replay protection. This is
     * an extra input and therefore affects the transaction fee.
     * @return A [Pair] object, where left = the sweepable amount as a [BigInteger],
     * right = the absolute fee needed to sweep those coins, also as a [BigInteger]
     */
    internal fun getMaximumAvailable(
        unspentCoins: List<Utxo>,
        targetOutputType: OutputType,
        feePerKb: BigInteger,
        includeReplayProtection: Boolean
    ): Pair<BigInteger, BigInteger> = payment.getMaximumAvailable(
        unspentCoins,
        targetOutputType,
        feePerKb,
        includeReplayProtection
    )

    /**
     * Returns true if the `absoluteFee` is adequate for the number of inputs/outputs in the
     * transaction.
     *
     * @param inputs The number of inputs
     * @param outputs List of output types (P2PKH, P2WPKH)
     * @param absoluteFee The absolute fee as a [BigInteger]
     * @return True if the fee is adequate, false if not
     */
    internal fun isAdequateFee(inputs: List<Utxo>, outputs: List<OutputType>, absoluteFee: BigInteger): Boolean =
        payment.isAdequateFee(inputs, outputs, absoluteFee)

    /**
     * Returns the estimated size of the transaction in kB.
     *
     * @param inputs The number of inputs
     * @param outputs List of output types (P2PKH, P2WPKH)
     * @return The estimated size of the transaction in kB
     */
    internal fun estimateSize(inputs: List<Utxo>, outputs: List<OutputType>): Double =
        payment.estimatedSize(inputs, outputs)

    /**
     * Returns an estimated absolute fee in satoshis (as a [BigInteger] for a given number of
     * inputs and outputs.
     *
     * @param inputs The number of inputs
     * @param outputs List of output types (P2PKH, P2WPKH)
     * @param feePerKb The current fee per kB om the network
     * @return A [BigInteger] representing the absolute fee
     */
    internal fun estimateFee(inputs: List<Utxo>, outputs: List<OutputType>, feePerKb: BigInteger): BigInteger =
        payment.estimatedFee(inputs, outputs, feePerKb)
}
