package info.blockchain.wallet.payment

import com.blockchain.api.NonCustodialBitcoinService
import info.blockchain.wallet.api.dust.data.DustInput
import info.blockchain.wallet.bch.BchMainNetParams
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.model.Utxo
import info.blockchain.wallet.payload.model.toBchUtxo
import info.blockchain.wallet.payload.model.toBtcUtxo
import io.reactivex.Single
import io.reactivex.annotations.NonNull
import okhttp3.ResponseBody
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Transaction
import org.bitcoinj.params.MainNetParams
import org.spongycastle.util.encoders.Hex
import retrofit2.Call
import java.math.BigInteger

class Payment(
    private val bitcoinApi: NonCustodialBitcoinService
) {
    // Fee Handling
    fun estimatedFee(inputs: List<Utxo>, outputs: List<OutputType>, @NonNull feePerKb: BigInteger): BigInteger {
        return Fees.estimatedFee(inputs, outputs, feePerKb)
    }

    fun estimatedSize(inputs: List<Utxo>, outputs: List<OutputType>): Double {
        return Fees.estimatedSize(inputs, outputs)
    }

    fun isAdequateFee(inputs: List<Utxo>, outputs: List<OutputType>, @NonNull absoluteFee: BigInteger): Boolean {
        return Fees.isAdequateFee(inputs, outputs, absoluteFee)
    }

    // Coin selection
    fun getUnspentBtcCoins(@NonNull xpubs: XPubs): Single<List<Utxo>> {
        return bitcoinApi.getUnspentOutputs(
            NonCustodialBitcoinService.BITCOIN,
            xpubs.legacyXpubAddresses(),
            xpubs.segwitXpubAddresses(),
            null,
            null
        ).map { dto ->
            dto.unspentOutputs.map { it.toBtcUtxo(xpubs) }
        }
    }

    fun getUnspentBchCoins(@NonNull addresses: List<String>): Single<List<Utxo>> {
        return bitcoinApi.getUnspentOutputs(
            NonCustodialBitcoinService.BITCOIN_CASH,
            addresses,
            emptyList(),
            null,
            null
        ).map { dto ->
            dto.unspentOutputs.map { it.toBchUtxo() }
        }
    }

    fun getMaximumAvailable(
        @NonNull unspentCoins: List<Utxo>,
        @NonNull targetOutputType: OutputType,
        @NonNull feePerKb: BigInteger,
        addReplayProtection: Boolean
    ): Pair<BigInteger, BigInteger> {
        return Coins.getMaximumAvailable(
            unspentCoins,
            targetOutputType,
            feePerKb,
            addReplayProtection
        )
    }

    fun getSpendableCoins(
        @NonNull unspentCoins: List<Utxo>,
        @NonNull targetOutputType: OutputType,
        @NonNull changeOutputType: OutputType,
        @NonNull paymentAmount: BigInteger,
        @NonNull feePerKb: BigInteger,
        addReplayProtection: Boolean
    ): SpendableUnspentOutputs {
        return Coins.getMinimumCoinsForPayment(
            unspentCoins,
            targetOutputType,
            changeOutputType,
            paymentAmount,
            feePerKb,
            addReplayProtection
        )
    }

    // Simple Transaction
    fun makeBtcSimpleTransaction(
        unspentCoins: List<Utxo>,
        receivingAddresses: HashMap<String, BigInteger>,
        fee: BigInteger,
        changeAddress: String?
    ): Transaction {
        return PaymentTx.makeSimpleTransaction(
            MainNetParams.get(),
            unspentCoins,
            receivingAddresses,
            fee,
            changeAddress
        )
    }

    fun signBtcTransaction(
        @NonNull transaction: Transaction,
        @NonNull keys: List<SigningKey>
    ) {
        PaymentTx.signSimpleTransaction(
            MainNetParams.get(),
            transaction,
            keys.map { it.toECKey() },
            false
        )
    }

    fun signBchTransaction(
        @NonNull transaction: Transaction,
        @NonNull keys: List<SigningKey>
    ) {
        PaymentTx.signSimpleTransaction(
            BchMainNetParams.get(),
            transaction,
            keys.map { it.toECKey() },
            true
        )
    }

    fun publishBtcSimpleTransaction(@NonNull transaction: Transaction): Call<ResponseBody> {
        return bitcoinApi.pushTx(
            NonCustodialBitcoinService.BITCOIN,
            String(Hex.encode(transaction.bitcoinSerialize()))
        )
    }

    // Non-replayable Transactions
    fun makeBchNonReplayableTransaction(
        unspentCoins: List<Utxo>?,
        receivingAddresses: HashMap<String, BigInteger>?,
        fee: BigInteger?,
        changeAddress: String?,
        dustServiceInput: DustInput?
    ): Transaction {

        return PaymentTx.makeNonReplayableTransaction(
            BchMainNetParams.get(),
            unspentCoins,
            receivingAddresses,
            fee,
            changeAddress,
            dustServiceInput
        )
    }

    fun publishBchTransaction(
        @NonNull transaction: Transaction,
        @NonNull lockSecret: String
    ): Call<ResponseBody> {
        return bitcoinApi.pushTxWithSecret(
            NonCustodialBitcoinService.BITCOIN_CASH,
            String(Hex.encode(transaction.bitcoinSerialize())),
            lockSecret
        )
    }

    companion object {
        val PUSHTX_MIN: BigInteger = BigInteger.valueOf(Coin.parseCoin("0.00001").longValue())
        val DUST: BigInteger = BigInteger.valueOf(Coin.parseCoin("0.000005460").longValue())
    }
}

private fun XPubs.segwitXpubAddresses() =
    forDerivation(XPub.Format.SEGWIT)?.address?.let {
        listOf(it)
    } ?: emptyList()

private fun XPubs.legacyXpubAddresses() =
    forDerivation(XPub.Format.LEGACY)?.address?.let {
        listOf(it)
    } ?: emptyList()
